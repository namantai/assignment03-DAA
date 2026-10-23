#!/usr/bin/env python3
"""Generate the three assignment plots from results/results.csv."""

from __future__ import annotations

import os
from collections import defaultdict
from pathlib import Path

os.environ.setdefault("MPLCONFIGDIR", "/tmp/daa-matplotlib")
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from analyze_results import read_and_validate


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "results" / "plots"
STYLES = {
    "Kruskal": dict(color="#6f42a5", marker="D", linestyle="-"),
    "Prim": dict(color="#c06412", marker="^", linestyle="-"),
    "Kosaraju": dict(color="#277343", marker="s", linestyle="--"),
    "Kahn": dict(color="#1664b0", marker="o", linestyle="-"),
    "DagShortestPath": dict(color="#bd3347", marker="x", linestyle=":"),
}


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
    axis.legend(frameon=False, fontsize=8, loc="upper left", bbox_to_anchor=(1.01, 1))


def operations_plot(rows: list[dict[str, str]]) -> None:
    fig, axis = plt.subplots(figsize=(10, 5.0), constrained_layout=True)
    for (algorithm, density), values in sorted(grouped(rows).items()):
        x = [int(row["n"]) for row in values]
        y = [
            int(row["dsu_ops"]) + int(row["edge_scans"]) + int(row["comparisons"])
            for row in values
        ]
        style = dict(STYLES[algorithm])
        style["linestyle"] = "-" if density == "sparse" else "--"
        axis.plot(x, y, **style, markerfacecolor="none", linewidth=1.4,
                  label=f"{algorithm} - {density}")
    style_axis(axis, "Counted operations vs graph size", "Counted operations", log_y=True)
    axis.text(0.03, 0.97, "Kahn and DAG curves coincide at each density",
              transform=axis.transAxes, va="top", fontsize=9)
    fig.savefig(OUTPUT_DIR / "operations-vs-n.png", dpi=180)
    plt.close(fig)


def scans_per_edge_plot(rows: list[dict[str, str]]) -> None:
    fig, axis = plt.subplots(figsize=(10, 4.4), constrained_layout=True)
    for (algorithm, density), values in sorted(grouped(rows).items()):
        x = [int(row["n"]) for row in values]
        y = [int(row["edge_scans"]) / int(row["m"]) for row in values]
        style = dict(STYLES[algorithm])
        style["linestyle"] = "-" if density == "sparse" else "--"
        axis.plot(x, y, **style, markerfacecolor="none", linewidth=1.4,
                  label=f"{algorithm} - {density}")
    style_axis(axis, "Edge scans per input edge", "edgeScans / m")
    axis.set_ylim(0, 2.25)
    axis.text(0.03, 0.94, "2: Prim = Kosaraju (both densities)",
              transform=axis.transAxes, fontsize=9)
    axis.text(0.03, 0.52, "1: Kahn = DAG (both densities)",
              transform=axis.transAxes, fontsize=9)
    fig.savefig(OUTPUT_DIR / "scans-per-edge.png", dpi=180)
    plt.close(fig)


def kruskal_share_plot(rows: list[dict[str, str]]) -> None:
    fig, axis = plt.subplots(figsize=(10, 4.2), constrained_layout=True)
    kruskal = [row for row in rows if row["algorithm"] == "Kruskal"]
    for (_, density), values in sorted(grouped(kruskal).items()):
        x = [int(row["n"]) for row in values]
        y = [100.0 * int(row["edge_scans"]) / int(row["m"]) for row in values]
        axis.plot(x, y, color=STYLES["Kruskal"]["color"],
                  marker="o" if density == "sparse" else "s",
                  linestyle="-" if density == "sparse" else "--", linewidth=2,
                  label=density.capitalize())
    style_axis(axis, "Share of edges examined by Kruskal", "Examined edges (%)")
    axis.set_ylim(0, 105)
    fig.savefig(OUTPUT_DIR / "kruskal-edge-share.png", dpi=180)
    plt.close(fig)


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    rows = read_and_validate()
    operations_plot(rows)
    scans_per_edge_plot(rows)
    kruskal_share_plot(rows)
    print(f"Wrote plots to {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
