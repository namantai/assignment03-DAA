#!/usr/bin/env python3
"""Validate benchmark evidence and calculate report statistics (standard library only)."""

import csv
import json
import math
import statistics
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HEADER = "algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result".split(",")
KINDS = {"Kruskal": "undirected", "Prim": "undirected", "Kosaraju": "directed",
         "Kahn": "dag", "DagShortestPath": "dag"}
REFERENCES = {
    (1000, 4000): (147970, 27, 396274),
    (1000, 20000): (30363, 1, 252411),
    (3162, 12648): (468911, 137, 1271911),
    (3162, 63240): (97589, 1, 723157),
    (10000, 40000): (1486139, 373, 3941828),
    (10000, 200000): (309550, 1, 2381342),
    (31623, 126492): (4715515, 1188, 12551891),
    (31623, 632460): (968517, 1, 7376612),
    (100000, 400000): (15012094, 3923, 40096539),
    (100000, 2000000): (3056681, 1, 23512087),
}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def read_and_validate():
    with (ROOT / "results/results.csv").open(newline="", encoding="utf-8") as stream:
        reader = csv.DictReader(stream)
        require(reader.fieldnames == HEADER, "Incorrect results.csv header")
        rows = list(reader)
    require(len(rows) == 50, "Expected exactly 50 result rows")
    samples = defaultdict(dict)
    with (ROOT / "results/runs.csv").open(newline="", encoding="utf-8") as stream:
        reader = csv.DictReader(stream)
        require(reader.fieldnames == ["algorithm", "graph", "n", "m", "run", "time_ns"],
                "Incorrect runs.csv header")
        for row in reader:
            key = (row["algorithm"], row["graph"], int(row["n"]), int(row["m"]))
            run = int(row["run"])
            require(run not in samples[key], f"Duplicate run: {key}, {run}")
            require(int(row["time_ns"]) > 0, "Non-positive elapsed time")
            samples[key][run] = int(row["time_ns"])
    seen = set()
    for row in rows:
        require(None not in row and None not in row.values(), "Malformed CSV row")
        n, m = int(row["n"]), int(row["m"])
        algorithm = row["algorithm"]
        require((n, m) in REFERENCES and algorithm in KINDS, "Unexpected configuration")
        key = (algorithm, row["graph"], n, m)
        require(key not in seen, f"Duplicate result: {key}")
        seen.add(key)
        density = "sparse" if m == 4 * n else "dense"
        require(row["graph"] == KINDS[algorithm] + "_" + density, "Incorrect graph kind")
        mst, scc, distance = REFERENCES[(n, m)]
        expected = {"Kruskal": mst, "Prim": mst, "Kosaraju": scc,
                    "Kahn": n, "DagShortestPath": distance}[algorithm]
        require(int(row["result"]) == expected, f"Reference mismatch: {key}")
        require(key in samples and set(samples[key]) == {1, 2, 3, 4, 5},
                f"Missing timed runs: {key}")
        time_ms = float(row["time_ms"])
        median = statistics.median(samples[key].values()) / 1_000_000
        require(math.isfinite(time_ms) and abs(time_ms - median) <= 0.000501,
                f"Incorrect median: {key}")
        scans, dsu, comparisons, depth = (int(row[column]) for column in
                                         ("edge_scans", "dsu_ops", "comparisons", "max_depth"))
        require(min(scans, dsu, comparisons, depth) >= 0, "Negative counter")
        if algorithm == "Kruskal":
            require(n - 1 <= scans <= m, "Invalid Kruskal scan count")
            require(dsu == 2 * scans + 3 * (n - 1), "Incorrect DSU call count")
        else:
            require(dsu == 0, "Unexpected DSU operations")
            expected_scans = 2 * m if algorithm in ("Prim", "Kosaraju") else m
            require(scans == expected_scans, f"Scan count mismatch: {key}")
        require((comparisons > 0) if algorithm in ("Kruskal", "Prim") else comparisons == 0,
                "Incorrect weight-comparison counter")
        require((1 <= depth <= n) if algorithm == "Kosaraju" else depth == 0,
                "Incorrect explicit stack depth")
    require(set(samples) == seen, "Extra or missing timing configurations")
    return rows


def slope(x, y):
    xs, ys = list(map(math.log, x)), list(map(math.log, y))
    mx, my = statistics.mean(xs), statistics.mean(ys)
    return sum((a - mx) * (b - my) for a, b in zip(xs, ys)) / sum((a - mx) ** 2 for a in xs)


def summarize(rows):
    groups = defaultdict(list)
    for row in rows:
        groups[(row["algorithm"], row["graph"].rsplit("_", 1)[1])].append(row)
    summary = {}
    for (algorithm, density), group in sorted(groups.items()):
        group.sort(key=lambda row: int(row["n"]))
        ns = [int(row["n"]) for row in group]
        ms = [int(row["m"]) for row in group]
        scans = [int(row["edge_scans"]) for row in group]
        comparisons = [int(row["comparisons"]) for row in group]
        dsu = [int(row["dsu_ops"]) for row in group]
        work = [s + c + d for s, c, d in zip(scans, comparisons, dsu)]
        formulas = [m * math.log2(m) if algorithm in ("Prim", "Kruskal") else n + m
                    for n, m in zip(ns, ms)]
        summary[algorithm + "_" + density] = {
            "n": ns,
            "work_over_formula": [w / f for w, f in zip(work, formulas)],
            "scans_per_edge": [s / m for s, m in zip(scans, ms)],
            "dsu_ops_per_edge": [d / m for d, m in zip(dsu, ms)],
            "edge_scan_slope": slope(ns, scans),
            "counted_work_slope": slope(ns, work),
            "comparison_slope": slope(ns, comparisons) if all(comparisons) else None,
            "time_ms": [float(row["time_ms"]) for row in group],
        }
    return summary


if __name__ == "__main__":
    rows = read_and_validate()
    summary = summarize(rows)
    (ROOT / "results/analysis.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print("Verified: 50 rows, 10 reference triples, exact counters and 250 raw timing samples.")
    for key, value in summary.items():
        ratios = value["work_over_formula"]
        print(f"{key}: scan slope={value['edge_scan_slope']:.3f}; "
              f"work slope={value['counted_work_slope']:.3f}; "
              f"normalized work={min(ratios):.3f}..{max(ratios):.3f}")
