package kz.aitu.daa;

import kz.aitu.daa.algorithms.KosarajuScc;
import kz.aitu.daa.algorithms.SccResult;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Random;
import java.nio.file.Path;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KosarajuSccTest {
    @Test
    void agreesWithBruteForceReachabilityOnSmallRandomGraphs() {
        Random random = new Random(42);
        for (int trial = 0; trial < 30; trial++) {
            int n = trial == 0 ? 200 : 2 + random.nextInt(199);
            Graph graph = new Graph(n, true);
            int m = random.nextInt(n * 5 + 1);
            for (int edge = 0; edge < m; edge++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), 1);
            }

            Metrics metrics = new Metrics();
            SccResult result = KosarajuScc.compute(graph, metrics);
            boolean[][] reachable = transitiveReachability(graph);
            int expectedCount = 0;
            for (int first = 0; first < n; first++) {
                boolean hasEarlierMember = false;
                for (int second = 0; second < n; second++) {
                    boolean sameByBruteForce = reachable[first][second] && reachable[second][first];
                    boolean sameByKosaraju = result.componentOf(first) == result.componentOf(second);
                    assertEquals(sameByBruteForce, sameByKosaraju,
                            "trial=" + trial + ", vertices=" + first + "," + second);
                    if (second < first && sameByBruteForce) {
                        hasEarlierMember = true;
                    }
                }
                if (!hasEarlierMember) {
                    expectedCount++;
                }
            }
            assertEquals(expectedCount, result.componentCount());
            assertEquals(2L * m, metrics.edgeScans());
        }
    }

    @Test
    void checksKnownComponentsAndEmptyGraph() throws Exception {
        Graph graph = Graph.read(Path.of("data/cyclic-directed.txt"));
        graph.addEdge(0, 1, -10);
        SccResult result = KosarajuScc.compute(graph, new Metrics());
        assertEquals(3, result.componentCount());
        assertEquals(result.componentOf(0), result.componentOf(1));
        assertEquals(result.componentOf(1), result.componentOf(2));
        assertEquals(result.componentOf(3), result.componentOf(4));
        assertEquals(3, new HashSet<>(java.util.List.of(result.componentOf(0),
                result.componentOf(3), result.componentOf(5))).size());
        Metrics metrics = new Metrics();
        assertEquals(0, KosarajuScc.compute(new Graph(0, true), metrics).componentCount());
        assertEquals(0, metrics.maxDepth());
        assertEquals(5, KosarajuScc.compute(new Graph(5, true), new Metrics()).componentCount());
        assertThrows(IllegalArgumentException.class,
                () -> KosarajuScc.compute(new Graph(2, false), new Metrics()));
    }

    private static boolean[][] transitiveReachability(Graph graph) {
        int n = graph.vertexCount();
        boolean[][] reachable = new boolean[n][n];
        for (int source = 0; source < n; source++) {
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(source);
            reachable[source][source] = true;
            while (!queue.isEmpty()) {
                int vertex = queue.remove();
                for (Graph.Arc arc : graph.outgoing(vertex)) {
                    if (!reachable[source][arc.to()]) {
                        reachable[source][arc.to()] = true;
                        queue.add(arc.to());
                    }
                }
            }
        }
        return reachable;
    }
}
