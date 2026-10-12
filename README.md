# DAA Assignment 3 - Graphs

A Java 17/Maven implementation of graph algorithms for the City Network & Job
Scheduler scenario. The project includes one adjacency-list graph type, DSU,
Kruskal, Prim, iterative Kosaraju, Kahn topological sorting, DAG shortest paths,
deterministic benchmarks, tests, plots, and a report.

## Requirements

- JDK 17 or newer
- Maven 3.9 or newer
- Python 3 with Matplotlib (only for regenerating plots)

## Build and test

```bash
mvn clean test
```

The JUnit 5 suite cross-checks:

- DSU against a simple label-based reference;
- Kruskal against Prim on 100 random connected graphs;
- Kosaraju against brute-force reachability;
- Kahn on cyclic and acyclic input;
- DAG shortest paths against Bellman-Ford with negative weights;
- iterative SCC on a 100,000-vertex path.

## Run the benchmark

```bash
mvn -q exec:java
```

This command runs five timed repetitions for every algorithm/configuration,
checks all reference values, and writes `results/results.csv`. It needs roughly
2 GB of available heap for the largest dense graph. To set the heap explicitly:

```bash
MAVEN_OPTS="-Xmx3g" mvn -q exec:java
```

Regenerate the three plots after the CSV is available:

```bash
python3 scripts/plot_results.py
```

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
```

## Reproducibility

Each graph kind starts with a fresh `new Random(42)`. The benchmark validates
Kruskal/Prim agreement, topological-order validity, the ten reference rows, and
the exact traversal scan counts before it writes the CSV. Timing is recorded as
the median of five runs; conclusions in the report use operation counters.

## AI-use acknowledgement

OpenAI Codex was used to help scaffold the project, implement and review the
algorithms, write tests and documentation, and generate the plotting script.
All benchmark reference values are checked by executable code, and the submitted
author is responsible for reviewing, understanding, and defending the solution.

