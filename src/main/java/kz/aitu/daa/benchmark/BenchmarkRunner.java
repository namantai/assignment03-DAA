package kz.aitu.daa.benchmark;

import kz.aitu.daa.algorithms.DagShortestPaths;
import kz.aitu.daa.algorithms.KahnTopologicalSort;
import kz.aitu.daa.algorithms.KosarajuScc;
import kz.aitu.daa.algorithms.KruskalMst;
import kz.aitu.daa.algorithms.MstResult;
import kz.aitu.daa.algorithms.PrimMst;
import kz.aitu.daa.algorithms.SccResult;
import kz.aitu.daa.algorithms.ShortestPathResult;
import kz.aitu.daa.algorithms.TopologicalResult;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.ToLongFunction;

/** Runs the deterministic 50-row benchmark and fails on any reference mismatch. */
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

    private record Outcome(long result, Metrics metrics) { }

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
            long result,
            long[] samplesNanos) { }

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
                Row kruskal = measure("Kruskal", "undirected_" + density, undirected,
                        KruskalMst::compute, result -> checkedTreeWeight(result, n));
                Row prim = measure("Prim", "undirected_" + density, undirected,
                        PrimMst::compute, result -> checkedTreeWeight(result, n));
                requireEqual("Kruskal/Prim agreement", kruskal.result(), prim.result());
                requireEqual("MST reference", EXPECTED_MST[referenceIndex], kruskal.result());
                rows.add(kruskal);
                rows.add(prim);
                undirected = null;

                Graph directed = GraphGenerator.directed(n, m);
                Row kosaraju = measure("Kosaraju", "directed_" + density, directed,
                        KosarajuScc::compute, SccResult::componentCount);
                requireEqual("SCC reference", EXPECTED_SCC[referenceIndex], kosaraju.result());
                requireEqual("Kosaraju edge scans", 2L * m, kosaraju.edgeScans());
                rows.add(kosaraju);
                directed = null;

                Graph dag = GraphGenerator.dag(n, m);
                Row kahn = measure("Kahn", "dag_" + density, dag,
                        KahnTopologicalSort::sort, result -> {
                            if (result.hasCycle() || !KahnTopologicalSort.isValid(dag, result.order())) {
                                throw new IllegalStateException("generated DAG has no valid topological order");
                            }
                            return result.order().size();
                        });
                Row shortest = measure("DagShortestPath", "dag_" + density, dag,
                        DagShortestPaths::compute, BenchmarkRunner::sumDistances);
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
        writeSamples(output.resolveSibling("runs.csv"), rows);
        System.out.printf("Runtime: %s %s; %s %s; max heap %d MiB%n",
                System.getProperty("java.vm.name"), System.getProperty("java.version"),
                System.getProperty("os.name"), System.getProperty("os.arch"),
                Runtime.getRuntime().maxMemory() / (1024 * 1024));
        System.out.println("Wrote " + rows.size() + " rows to " + output.toAbsolutePath());
    }

    private static <T> Row measure(
            String algorithm,
            String graphName,
            Graph graph,
            BiFunction<Graph, Metrics, T> operation,
            ToLongFunction<T> validateAndSummarize) {
        long[] elapsed = new long[RUNS];
        Outcome canonical = null;
        for (int run = 0; run < RUNS; run++) {
            Metrics runMetrics = new Metrics();
            long start = System.nanoTime();
            T result = operation.apply(graph, runMetrics);
            elapsed[run] = System.nanoTime() - start;
            // Correctness checks and result aggregation are outside the timer.
            Outcome outcome = new Outcome(validateAndSummarize.applyAsLong(result), runMetrics);
            if (canonical == null) {
                canonical = outcome;
            } else {
                requireEqual(algorithm + " result stability", canonical.result(), outcome.result());
                requireEqual(algorithm + " counter stability",
                        canonical.metrics().edgeScans(), outcome.metrics().edgeScans());
                requireEqual(algorithm + " find stability",
                        canonical.metrics().findCalls(), runMetrics.findCalls());
                requireEqual(algorithm + " union stability",
                        canonical.metrics().successfulUnions(), runMetrics.successfulUnions());
                requireEqual(algorithm + " comparison stability",
                        canonical.metrics().comparisons(), runMetrics.comparisons());
                requireEqual(algorithm + " depth stability",
                        canonical.metrics().maxDepth(), runMetrics.maxDepth());
            }
        }
        long[] samples = elapsed.clone();
        Arrays.sort(elapsed);
        Metrics metrics = canonical.metrics();
        return new Row(
                algorithm,
                graphName,
                graph.vertexCount(),
                graph.edgeCount(),
                elapsed[RUNS / 2] / 1_000_000.0,
                metrics.dsuOps(),
                metrics.edgeScans(),
                metrics.comparisons(),
                metrics.maxDepth(),
                canonical.result(), samples);
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

    private static void writeSamples(Path output, List<Row> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            writer.write("algorithm,graph,n,m,run,time_ns\n");
            for (Row row : rows) {
                for (int run = 0; run < RUNS; run++) {
                    writer.write(String.format(Locale.ROOT, "%s,%s,%d,%d,%d,%d%n",
                            row.algorithm(), row.graph(), row.n(), row.m(), run + 1,
                            row.samplesNanos()[run]));
                }
            }
        }
    }
}
