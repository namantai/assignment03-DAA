package kz.aitu.daa;

import kz.aitu.daa.algorithms.KosarajuScc;
import kz.aitu.daa.algorithms.SccResult;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KosarajuSccTest {
    @Test
    void agreesWithBruteForceReachabilityOnSmallRandomGraphs() {
        Random random = new Random(42);
        for (int trial = 0; trial < 30; trial++) {
            int n = 2 + random.nextInt(59);
            Graph graph = new Graph(n, true);
            int m = random.nextInt(n * 5 + 1);
            for (int edge = 0; edge < m; edge++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), 1);
            }

            Metrics metrics = new Metrics();
            SccResult result = KosarajuScc.compute(graph, metrics);
            boolean[][] reachable = transitiveReachability(graph);
            for (int first = 0; first < n; first++) {
                for (int second = 0; second < n; second++) {
                    boolean sameByBruteForce = reachable[first][second] && reachable[second][first];
                    boolean sameByKosaraju = result.componentOf(first) == result.componentOf(second);
                    assertEquals(sameByBruteForce, sameByKosaraju,
                            "trial=" + trial + ", vertices=" + first + "," + second);
                }
            }
            assertEquals(2L * m, metrics.edgeScans());
        }
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

