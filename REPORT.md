# Assignment 3 Report: Graph Algorithms

## 1. Design and complexity

The project uses one weighted adjacency-list `Graph` for directed and undirected
inputs. An undirected input edge is stored once in the edge list and twice in the
adjacency lists. All counters are incremented at the point where work occurs;
they are not reconstructed from formulas after a run. The benchmark creates a
fresh `Random(42)` for every graph kind and rejects a row before writing the CSV
if an algorithm disagrees with another algorithm or with the supplied reference.

| Operation / algorithm | Time | Extra space | Justification |
|---|---:|---:|---|
| DSU `find` / `union` | amortized `O(alpha(V))` | `O(V)` | Path compression and union by size bound a sequence of operations by the inverse-Ackermann factor. |
| Kruskal | `O(E log E + E alpha(V))` | `O(V + E)` | Sorting dominates; each examined edge performs a constant number of DSU operations. |
| Prim (binary heap) | `O(E log E)` = `O(E log V)` | `O(V + E)` | Every adjacency entry is scanned and eligible edges enter a priority queue. |
| Kosaraju | `O(V + E)` | `O(V + E)` | Two iterative DFS passes scan each directed edge once; the reversed graph also occupies `O(V + E)`. |
| Kahn | `O(V + E)` | `O(V)` | In-degrees are built once, then every vertex is queued once and every edge is processed once. |
| DAG shortest paths | `O(V + E)` | `O(V)` | A topological order is followed by exactly one relaxation of every edge. |

`alpha(V)` is below 5 for any realistic input, so DSU operations are effectively
constant-time in this experiment. Explicit DFS frame arrays replace the JVM call
stack; the 100,000-vertex path test reaches depth 100,000 without a
`StackOverflowError`. Self-loops, parallel edges, isolated vertices, and
disconnected graphs are accepted. Both MST algorithms return a minimum spanning
forest and its component count when the input is disconnected.

## 2. Counters versus formulas

The table summarizes all ten benchmark configurations. “Observed ratio” is the
minimum-to-maximum range over the five sizes and two densities. Constant bounded
ratios are the empirical signature of the stated class.

| Algorithm | Counter / formula | Observed ratio | Interpretation |
|---|---:|---:|---|
| Kruskal | `(dsuOps + edgeScans + comparisons) / (E log2 E)` | `0.727..1.157` | Sorting plus almost-constant DSU work follows `E log E`. |
| Prim | `(edgeScans + comparisons) / (E log2 V)` | `2.288..2.817` | Heap comparisons account for the logarithmic factor. |
| Kosaraju | `edgeScans / (V + E)` | `1.600` sparse, `1.905` dense | The exact numerator is `2E`; density fixes the ratio. |
| Kahn | `edgeScans / (V + E)` | `0.800` sparse, `0.952` dense | The exact numerator is `E`. |
| DAG shortest paths | `edgeScans / (V + E)` | `0.800` sparse, `0.952` dense | The relaxation pass scans exactly `E` edges. |

Kruskal's `dsuOps/E` remains bounded: `2.290..2.748` on sparse graphs and
`0.565..0.894` on dense graphs. Its examined-edge share is `77.1%..99.9%` for
sparse graphs but only `20.7%..37.2%` for dense graphs, because a connected tree
is completed long before all dense edges are reached. Prim always scans `2E`
adjacency entries for an undirected graph. Kosaraju scans exactly `2E`; Kahn and
the DAG relaxation each scan exactly `E`, matching the assignment's mandatory
counter checks for every row.

![Counted operations versus n](results/plots/operations-vs-n.png)

![Edge scans per edge](results/plots/scans-per-edge.png)

![Kruskal examined-edge share](results/plots/kruskal-edge-share.png)

### Log-log slope check

Least-squares fits use `log(edgeScans)` against `log(n)` over the five sizes.
Kosaraju, Kahn, DAG relaxation, and Prim all have slope `1.000` for both
densities because `E` is a fixed multiple of `V`. Kruskal has slope `1.055` on
sparse graphs and `1.119` on dense graphs. Its scan curve is slightly steeper
because the fraction of sorted edges needed to finish the tree changes with
size; its comparison counter separately captures the `E log E` sorting factor.

## 3. Correctness arguments

### MST and the cut property

Consider any cut that separates the vertices into two sets and the lightest edge
crossing that cut. The cut property says that this edge is safe: some minimum
spanning forest contains it, because replacing a heavier crossing edge on the
corresponding cycle cannot increase total weight. Kruskal's DSU components define
such a cut whenever it considers an edge whose endpoints are in different sets.
Therefore every edge accepted by Kruskal can extend some optimal forest. Prim's
visited and unvisited vertices define a cut, and the priority queue selects the
lightest eligible crossing edge. Thus every Prim step is safe for the same reason,
and induction over accepted edges proves both algorithms produce a minimum
spanning tree in each connected component. The benchmark additionally checks
their total weights for all generated graphs, while randomized tests check 100
smaller connected graphs.

### One-pass shortest paths in a DAG

Add a virtual super-source with a zero-weight edge to every in-degree-zero vertex;
initializing those distances to zero is exactly the same operation without storing
the extra vertex. In a topological order, every predecessor of a vertex appears
before that vertex. Assume inductively that all vertices before position `i` have
their final shortest distances when processed. Every possible path into the
vertex at position `i` ends with an edge from one of those already-final
predecessors, so all candidates have already been relaxed when its turn begins.
Its distance is therefore final, and relaxing its outgoing edges establishes the
induction hypothesis for later vertices. After one pass, every reachable vertex
has its shortest distance, and `parent[]` records a realizing path. Negative
weights are safe because a DAG has no cycle to revisit; in a general graph a
negative cycle could lower a distance indefinitely, invalidating a one-pass
argument.

## 4. Which algorithm when

| Situation | Choice | Reason |
|---|---|---|
| Undirected, edge list already available; sparse or moderate density | Kruskal | Simple global sort plus DSU; easy to stop after `V-1` accepted edges. |
| Undirected, adjacency list; very dense connected graph | Prim | Grows from vertices directly and avoids keeping a second sorted edge order; an `O(V^2)` array variant may beat a heap on truly dense matrices. |
| Need connected components under incremental undirected unions | DSU | Near-constant queries; unlike DFS, it updates without retraversing the graph. |
| Directed graph, cycles possible; need all mutual-reachability groups | Kosaraju | Works on arbitrary directed graphs in linear time and reports SCCs/deadlocks. |
| Directed dependencies, must decide whether a cycle exists | Kahn | Returns a valid order when acyclic and explicitly reports a cycle otherwise. |
| Weighted DAG, including negative edges | DAG shortest paths | Linear-time relaxation; no Bellman-Ford repetitions are needed. |
| General weighted graph with negative edges/cycles possible | Bellman-Ford, not this DAG method | Topological order may not exist, and negative cycles require detection. |

Representation matters as much as density. Kruskal naturally consumes an edge
list, while Prim naturally consumes adjacency lists. Traversals, Kahn, Kosaraju,
and DAG relaxation should use adjacency lists because they need outgoing edges by
vertex. A topological algorithm must never be used merely because the input looks
“mostly acyclic”: Kahn's cycle result must be checked first.

## 5. Results and timing

All ten reference triples match exactly, including the largest row:
MST `3,056,681`, SCC count `1`, and DAG distance sum `23,512,087` at
`n=100,000`, `m=2,000,000`. `results/results.csv` contains 50 data rows with the
requested columns. Median times generally grew with the counters, but small-case
timings were noisy: for example, dense Kahn grew from about `1.2 ms` to `58.5 ms`,
while dense Prim grew from about `8.2 ms` to `1,972.8 ms`. Those values describe
this particular JVM and machine only; JIT compilation, allocation, background
load, and hardware change them. The operation counters and exact reference
results are therefore the primary evidence, while wall-clock time is only a
secondary practical observation.

## 6. Validation summary

The JUnit suite contains 11 tests. It cross-checks DSU with a simple reference,
MST weights between two independent algorithms, SCC membership with BFS-based
reachability, DAG distances with Bellman-Ford including negative weights, cycle
detection, topological-order validity, input parsing, disconnected forests, and
100,000-vertex stack safety. The benchmark repeats result and counter checks on
each timed run and refuses to write a misleading CSV when a check fails.
