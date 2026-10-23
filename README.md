# DAA Assignment 3 - Graphs

A Java 17/Maven implementation of graph algorithms for the City Network & Job
Scheduler scenario. The project includes one adjacency-list graph type, DSU,
Kruskal, Prim, iterative Kosaraju, Kahn topological sorting, DAG shortest paths,
deterministic benchmarks, tests, plots, and a report.

## Requirements

- JDK 17 or newer
- Maven 3.9 or newer
- Python 3.10+ with Matplotlib (only for regenerating plots)
- ReportLab 4 (optional: regenerate the included five-page PDF)

## Build and test

```bash
mvn clean verify
```

This builds the JAR and runs all 37 JUnit 5 cases. To run only tests, use
`mvn test`. Maven plugin versions are pinned in `pom.xml`, including the
Java 17 compiler plugin. The JUnit 5 suite cross-checks:

- DSU against a simple label-based reference;
- Kruskal against Prim on 100 random connected graphs;
- both MST algorithms against exhaustive enumeration on 100 small multigraphs;
- Kosaraju against brute-force reachability up to 200 vertices;
- Kahn on cyclic and acyclic input;
- DAG shortest paths against Bellman-Ford with negative weights;
- every restored path's source, edges, parent chain and total weight;
- iterative SCC on a 100,000-vertex path and cycle;
- empty/singleton graphs, self-loops, parallel edges, long totals, invalid
  generator sizes and malformed input files.

## Run the benchmark

```bash
mvn -q compile exec:java
```

This command runs five timed repetitions for every algorithm/configuration,
checks all reference values, and writes `results/results.csv` (50 rows) and
`results/runs.csv` (250 raw nanosecond samples). The complete audit used a JVM
with a maximum heap of 3,928 MiB. A 3 GiB heap is a practical setting for the
largest dense input; JVM and GC choices affect the memory needed:

```bash
MAVEN_OPTS="-Xmx3g" mvn -q compile exec:java
```

Validate every CSV reference and median, and regenerate the three plots:

```bash
python3 scripts/analyze_results.py
python3 scripts/plot_results.py
```

The validator also writes `results/analysis.json` with per-size normalized
counters and least-squares slopes. Plots are made only after validation succeeds.
For a fresh Python environment, install optional artifact dependencies:

```bash
python3 -m venv .venv
.venv/bin/python -m pip install -r scripts/requirements.txt
.venv/bin/python scripts/plot_results.py
.venv/bin/python scripts/render_report.py
```

The PDF is generated from `REPORT.md` at `output/pdf/REPORT.pdf`; its four page
breaks produce five A4 pages. The committed report contains the audited run's
times. After rerunning the benchmark, use `results/analysis.json` to update the
report's measured tables and timing paragraph before rendering it again.

## Input format

The first non-comment line is:

```text
n m directed|undirected
```

It is followed by exactly `m` lines `u v w`. Vertices are `0..n-1`, and weights
are signed Java `int` values. Blank lines and text after `#` are ignored. Example
files in `data/` cover a connected graph, a disconnected graph with an isolated
vertex, a cyclic directed graph, self-loops, parallel edges, and a DAG with
negative weights.

## Project layout

```text
src/main/java/kz/aitu/daa/
  graph/       shared graph representation and parser
  algorithms/  DSU, MST, SCC, topological sort, shortest paths
  metrics/     operation counters
  benchmark/   exact Random(42) generators and self-checking benchmark
src/test/java/ JUnit 5 cross-checks and stack-safety test
data/          small input graphs
results/       reproducible CSV and PNG plots
scripts/       plot generator
REPORT.md      analysis and correctness arguments
output/pdf/    five-page printable report
```

## Reproducibility

Each graph kind starts with a fresh `new Random(42)`. The benchmark validates
Kruskal/Prim agreement, topological-order validity, the ten reference rows, and
the exact traversal scan counts before it writes the CSV. All four counters
are checked for repeatability across the five runs. Timing covers the algorithm
call, including its setup, but excludes graph generation, result validation and
aggregation. There is no separate warm-up; small timings can be noisy.
The report defines the counted phases explicitly: Kosaraju's DFS passes scan 2m,
Kahn's queue processing scans m and DAG relaxation scans m. Reversal, in-degree
setup and internal topological ordering are not added to those phase counters.

To rebuild, test and reproduce all Java results in one command:

```bash
mvn clean verify exec:java
```

The graph answers and mandatory scan counts are exact. Sort/heap comparison
counts may depend on the JDK implementation, and wall-clock times are not
expected to reproduce bit-for-bit.

## Submission and Git status

The repository has `main`, `feature/dsu`, `feature/mst`, `feature/scc`,
`feature/paths`, and `feature/metrics`. The pre-existing `v1.0` tag points to
commit `3e25683`, before the audit fixes. It has deliberately not been moved:
the owner must explicitly approve moving this release tag to the audited main
commit. No GitHub remote is configured and nothing has been pushed.

Before submitting, publish the repository, add its real GitHub URL, resolve the
release-tag target, and create `DAA_Assignment3_name_surname_group.zip` with your
actual name and group. Include source, tests, data, results, scripts, README and
the report; omit `target/`, `.venv/` and temporary files. Upload the ZIP to Moodle.
Tarjan SCC and condensation/critical-path bonuses are not implemented; they
are separate from the 100-point core requirements.

## AI-use acknowledgement

OpenAI Codex was used to help scaffold the project, implement and review the
algorithms, write tests and documentation, and generate the plotting script.
All benchmark reference values are checked by executable code, and the submitted
author is responsible for reviewing, understanding, and defending the solution.
