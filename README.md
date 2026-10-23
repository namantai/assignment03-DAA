# DAA Assignment 3 - Graphs

A Java 17/Maven implementation of graph algorithms for the City Network & Job
Scheduler scenario. The project includes one adjacency-list graph type, DSU,
Kruskal, Prim, iterative Kosaraju, Kahn topological sorting, DAG shortest paths,
deterministic benchmarks, tests, plots, and a report.

## Requirements

- JDK 17 or newer
- Maven 3.9 or newer
- Python 3.10+ with Matplotlib (only for regenerating plots)

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
checks all reference values, and writes only `results/results.csv` (50 rows).
The five measurements stay in a local array; its middle value after sorting
is the median. No separate timing CSV is created. A 3 GiB heap is a practical
setting for the largest dense input; JVM and GC choices affect the memory needed:

```bash
MAVEN_OPTS="-Xmx3g" mvn -q compile exec:java
```

Validate the CSV results and counters, and regenerate the three plots:

```bash
python3 scripts/analyze_results.py
python3 scripts/plot_results.py
```

The validator prints per-size normalized counters, least-squares slopes and
times without creating additional files. It cannot independently reconstruct
the five timings from the median alone. Plots are made only after validation
succeeds. For a fresh Python environment, install Matplotlib:

```bash
python3 -m venv .venv
.venv/bin/python -m pip install -r scripts/requirements.txt
.venv/bin/python scripts/analyze_results.py
.venv/bin/python scripts/plot_results.py
```

`REPORT.md` is the only report; a duplicate PDF is not needed. After rerunning
the benchmark, use the analysis script's output to update the report's measured
tables and timing paragraph. Timings naturally change between runs.

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
src/main/java/daa/
  algo/        MST, SCC, topological sort, shortest paths and their results
  bench/       exact Random(42) generators and BenchmarkRunner
  ds/          DisjointSetUnion
  graph/       shared graph representation and parser
  metrics/     operation counters
src/test/java/daa/  JUnit 5 cross-checks and stack-safety tests
data/              four small input graphs
results/
  results.csv      50 rows, one median per algorithm/configuration
  plots/           three PNG plots
scripts/           CSV analysis, plot generator and Python requirements
pom.xml            Maven build; main class daa.bench.BenchmarkRunner
README.md          build, test and demonstration instructions
REPORT.md          analysis and correctness arguments
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

