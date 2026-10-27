package daa.bench;

import daa.algo.DagShortestPaths;
import daa.algo.KahnTopologicalSort;
import daa.algo.KosarajuScc;
import daa.algo.KruskalMst;
import daa.algo.MstResult;
import daa.algo.PrimMst;
import daa.algo.SccResult;
import daa.algo.ShortestPathResult;
import daa.algo.TopologicalResult;
import daa.graph.Graph;
import daa.metrics.Metrics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class BenchmarkRunner {
    private static final int[] SIZES = {1_000, 3_162, 10_000, 31_623, 100_000};
    private static final int[] DENSITIES = {4, 20};
    private static final int RUNS = 5;
    private static final long[] EXPECTED_MST = {
            147_970, 30_363, 468_911, 97_589, 1_486_139,
            309_550, 4_715_515, 968_517, 15_012_094, 3_056_681
    };
    private static final long[] EXPECTED_SCC = {
            27, 1, 137, 1, 373, 1, 1_188, 1, 3_923, 1
    };
    private static final long[] EXPECTED_DAG_SUM = {
            396_274, 252_411, 1_271_911, 723_157, 3_941_828,
            2_381_342, 12_551_891, 7_376_612, 40_096_539, 23_512_087
    };

    private record Row(
            String algorithm,
            String graph,
            int n,
            int m,
            double timeMs,
            long dsuOps,
            long edgeScans,
            long comparisons,
            long maxDepth,
            long result) { }

    private BenchmarkRunner() { }

    public static void main(String[] args) throws IOException {
        Path output = args.length == 0 ? Path.of("results/results.csv") : Path.of(args[0]);
        ArrayList<Row> rows = new ArrayList<>(50);
        int referenceIndex = 0;

        for (int n : SIZES) {
            for (int factor : DENSITIES) {
                int m = Math.multiplyExact(n, factor);
                String density = factor == 4 ? "sparse" : "dense";
                System.out.printf(Locale.ROOT, "n=%d m=%d (%s)%n", n, m, density);

                Graph undirected = GraphGenerator.undirected(n, m);
                Row kruskal = measure("Kruskal", "undirected_" + density, undirected);
                Row prim = measure("Prim", "undirected_" + density, undirected);
                requireEqual("Kruskal/Prim agreement", kruskal.result(), prim.result());
                requireEqual("MST reference", EXPECTED_MST[referenceIndex], kruskal.result());
                rows.add(kruskal);
                rows.add(prim);
                undirected = null;

                Graph directed = GraphGenerator.directed(n, m);
                Row kosaraju = measure("Kosaraju", "directed_" + density, directed);
                requireEqual("SCC reference", EXPECTED_SCC[referenceIndex], kosaraju.result());
                requireEqual("Kosaraju edge scans", 2L * m, kosaraju.edgeScans());
                rows.add(kosaraju);
                directed = null;

                Graph dag = GraphGenerator.dag(n, m);
                Row kahn = measure("Kahn", "dag_" + density, dag);
                Row shortest = measure("DagShortestPath", "dag_" + density, dag);
                requireEqual("Kahn order size", n, kahn.result());
                requireEqual("Kahn edge scans", m, kahn.edgeScans());
                requireEqual("DAG shortest-path edge scans", m, shortest.edgeScans());
                requireEqual("DAG distance reference", EXPECTED_DAG_SUM[referenceIndex], shortest.result());
                rows.add(kahn);
                rows.add(shortest);

                System.out.printf(Locale.ROOT, "  reference: MST=%d SCC=%d DAG=%d%n",
                        kruskal.result(), kosaraju.result(), shortest.result());
                referenceIndex++;
            }
        }
        writeCsv(output, rows);
        System.out.printf("Runtime: %s %s; %s %s; max heap %d MiB%n",
                System.getProperty("java.vm.name"), System.getProperty("java.version"),
                System.getProperty("os.name"), System.getProperty("os.arch"),
                Runtime.getRuntime().maxMemory() / (1024 * 1024));
        System.out.println("Wrote " + rows.size() + " rows to " + output.toAbsolutePath());
    }

    private static Row measure(String algorithm, String graphName, Graph graph) {
        long[] elapsed = new long[RUNS];
        Metrics firstMetrics = null;
        long firstResult = 0;
        for (int run = 0; run < RUNS; run++) {
            Metrics runMetrics = new Metrics();
            long value;
            if (algorithm.equals("Kruskal")) {
                long start = System.nanoTime();
                MstResult result = KruskalMst.compute(graph, runMetrics);
                elapsed[run] = System.nanoTime() - start;
                value = checkedTreeWeight(result, graph.vertexCount());
            } else if (algorithm.equals("Prim")) {
                long start = System.nanoTime();
                MstResult result = PrimMst.compute(graph, runMetrics);
                elapsed[run] = System.nanoTime() - start;
                value = checkedTreeWeight(result, graph.vertexCount());
            } else if (algorithm.equals("Kosaraju")) {
                long start = System.nanoTime();
                SccResult result = KosarajuScc.compute(graph, runMetrics);
                elapsed[run] = System.nanoTime() - start;
                value = result.componentCount();
            } else if (algorithm.equals("Kahn")) {
                long start = System.nanoTime();
                TopologicalResult result = KahnTopologicalSort.sort(graph, runMetrics);
                elapsed[run] = System.nanoTime() - start;
                if (result.hasCycle() || !KahnTopologicalSort.isValid(graph, result.order())) {
                    throw new IllegalStateException("generated DAG has no valid topological order");
                }
                value = result.order().size();
            } else if (algorithm.equals("DagShortestPath")) {
                long start = System.nanoTime();
                ShortestPathResult result = DagShortestPaths.compute(graph, runMetrics);
                elapsed[run] = System.nanoTime() - start;
                value = sumDistances(result);
            } else {
                throw new IllegalArgumentException("unknown algorithm: " + algorithm);
            }
            if (run == 0) {
                firstResult = value;
                firstMetrics = runMetrics;
            } else {
                requireEqual(algorithm + " result stability", firstResult, value);
                requireEqual(algorithm + " counter stability",
                        firstMetrics.edgeScans(), runMetrics.edgeScans());
                requireEqual(algorithm + " find stability",
                        firstMetrics.findCalls(), runMetrics.findCalls());
                requireEqual(algorithm + " union stability",
                        firstMetrics.successfulUnions(), runMetrics.successfulUnions());
                requireEqual(algorithm + " comparison stability",
                        firstMetrics.comparisons(), runMetrics.comparisons());
                requireEqual(algorithm + " depth stability",
                        firstMetrics.maxDepth(), runMetrics.maxDepth());
            }
        }
        Arrays.sort(elapsed);
        return new Row(
                algorithm,
                graphName,
                graph.vertexCount(),
                graph.edgeCount(),
                elapsed[RUNS / 2] / 1_000_000.0,
                firstMetrics.dsuOps(),
                firstMetrics.edgeScans(),
                firstMetrics.comparisons(),
                firstMetrics.maxDepth(),
                firstResult);
    }

    private static long checkedTreeWeight(MstResult result, int n) {
        requireEqual("MST components", 1, result.components());
        requireEqual("MST edge count", n - 1L, result.edges().size());
        long sum = 0;
        for (Graph.Edge edge : result.edges()) {
            sum += edge.weight();
        }
        requireEqual("MST edge-weight sum", sum, result.totalWeight());
        return result.totalWeight();
    }

    private static long sumDistances(ShortestPathResult result) {
        long sum = 0;
        for (long distance : result.distances()) {
            if (distance == ShortestPathResult.INFINITY) {
                throw new IllegalStateException("virtual super-source did not reach every DAG vertex");
            }
            sum += distance;
        }
        return sum;
    }

    private static void writeCsv(Path output, List<Row> rows) throws IOException {
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            writer.write("algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result\n");
            for (Row row : rows) {
                writer.write(String.format(Locale.ROOT,
                        "%s,%s,%d,%d,%.3f,%d,%d,%d,%d,%d%n",
                        row.algorithm(), row.graph(), row.n(), row.m(), row.timeMs(),
                        row.dsuOps(), row.edgeScans(), row.comparisons(), row.maxDepth(),
                        row.result()));
            }
        }
    }

    private static void requireEqual(String label, long expected, long actual) {
        if (expected != actual) {
            throw new IllegalStateException(
                    label + " mismatch: expected " + expected + ", got " + actual);
        }
    }
}
