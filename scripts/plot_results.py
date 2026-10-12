#!/usr/bin/env python3
"""Generate the three assignment plots from results/results.csv."""

from __future__ import annotations

import csv
import os
from collections import defaultdict
from pathlib import Path

os.environ.setdefault("MPLCONFIGDIR", "/tmp/daa-matplotlib")
import matplotlib.pyplot as plt


ROOT = Path(__file__).resolve().parents[1]
CSV_PATH = ROOT / "results" / "results.csv"
OUTPUT_DIR = ROOT / "results" / "plots"


def read_rows() -> list[dict[str, str]]:
    with CSV_PATH.open(newline="", encoding="utf-8") as stream:
        return list(csv.DictReader(stream))


def grouped(rows: list[dict[str, str]]):
    groups = defaultdict(list)
    for row in rows:
        density = row["graph"].rsplit("_", 1)[-1]
        groups[(row["algorithm"], density)].append(row)
    for values in groups.values():
        values.sort(key=lambda row: int(row["n"]))
    return groups


def style_axis(axis, title: str, ylabel: str, log_y: bool = False) -> None:
    axis.set_title(title, fontweight="bold")
    axis.set_xlabel("Vertices, n")
    axis.set_ylabel(ylabel)
    axis.set_xscale("log")
    if log_y:
        axis.set_yscale("log")
    axis.grid(True, which="both", alpha=0.25)
    axis.legend(frameon=False, fontsize=8, ncol=2)


def operations_plot(rows: list[dict[str, str]]) -> None:
    fig, axis = plt.subplots(figsize=(10, 6.2), constrained_layout=True)
    markers = {"sparse": "o", "dense": "s"}
    for (algorithm, density), values in sorted(grouped(rows).items()):
        x = [int(row["n"]) for row in values]
        y = [
            int(row["dsu_ops"]) + int(row["edge_scans"]) + int(row["comparisons"])
            for row in values
        ]
        axis.plot(x, y, marker=markers[density], linewidth=1.8,
                  label=f"{algorithm} - {density}")
    style_axis(axis, "Counted operations vs graph size", "Counted operations", log_y=True)
    fig.savefig(OUTPUT_DIR / "operations-vs-n.png", dpi=180)
    plt.close(fig)


def scans_per_edge_plot(rows: list[dict[str, str]]) -> None:
    fig, axis = plt.subplots(figsize=(10, 6.2), constrained_layout=True)
    markers = {"sparse": "o", "dense": "s"}
    for (algorithm, density), values in sorted(grouped(rows).items()):
        x = [int(row["n"]) for row in values]
        y = [int(row["edge_scans"]) / int(row["m"]) for row in values]
        axis.plot(x, y, marker=markers[density], linewidth=1.8,
                  label=f"{algorithm} - {density}")
    style_axis(axis, "Edge scans per input edge", "edgeScans / m")
    fig.savefig(OUTPUT_DIR / "scans-per-edge.png", dpi=180)
    plt.close(fig)


def kruskal_share_plot(rows: list[dict[str, str]]) -> None:
    fig, axis = plt.subplots(figsize=(8.5, 5.4), constrained_layout=True)
    kruskal = [row for row in rows if row["algorithm"] == "Kruskal"]
    for (_, density), values in sorted(grouped(kruskal).items()):
        x = [int(row["n"]) for row in values]
        y = [100.0 * int(row["edge_scans"]) / int(row["m"]) for row in values]
        axis.plot(x, y, marker="o" if density == "sparse" else "s", linewidth=2,
                  label=density.capitalize())
    style_axis(axis, "Share of edges examined by Kruskal", "Examined edges (%)")
    axis.set_ylim(bottom=0)
    fig.savefig(OUTPUT_DIR / "kruskal-edge-share.png", dpi=180)
    plt.close(fig)


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    rows = read_rows()
    if len(rows) != 50:
        raise SystemExit(f"Expected 50 CSV rows, found {len(rows)}")
    operations_plot(rows)
    scans_per_edge_plot(rows)
    kruskal_share_plot(rows)
    print(f"Wrote plots to {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
