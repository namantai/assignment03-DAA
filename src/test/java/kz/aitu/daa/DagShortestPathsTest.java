package kz.aitu.daa;

import kz.aitu.daa.algorithms.DagShortestPaths;
import kz.aitu.daa.algorithms.ShortestPathResult;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DagShortestPathsTest {
    @Test
    void agreesWithBellmanFordOnRandomDagsWithNegativeWeights() {
        Random random = new Random(42);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(49);
            int[] order = shuffledIdentity(n, random);
            Graph graph = new Graph(n, true);
            for (int firstPosition = 0; firstPosition < n; firstPosition++) {
                for (int secondPosition = firstPosition + 1; secondPosition < n; secondPosition++) {
                    if (random.nextDouble() < 0.11) {
                        graph.addEdge(order[firstPosition], order[secondPosition],
                                random.nextInt(41) - 20);
                    }
                }
            }
            Metrics metrics = new Metrics();
            ShortestPathResult actual = DagShortestPaths.compute(graph, metrics);
            assertArrayEquals(bellmanFordFromAllSources(graph), actual.distances(), "trial " + trial);
            assertEquals(graph.edgeCount(), metrics.edgeScans());
            for (int vertex = 0; vertex < n; vertex++) {
                assertEquals(vertex, actual.pathTo(vertex).get(actual.pathTo(vertex).size() - 1));
            }
        }
    }

    @Test
    void rejectsCyclicGraph() {
        Graph graph = new Graph(2, true);
        graph.addEdge(0, 1, -1);
        graph.addEdge(1, 0, -1);
        assertThrows(IllegalArgumentException.class,
                () -> DagShortestPaths.compute(graph, new Metrics()));
    }

    private static long[] bellmanFordFromAllSources(Graph graph) {
        int[] indegree = graph.indegrees();
        long[] distance = new long[graph.vertexCount()];
        Arrays.fill(distance, ShortestPathResult.INFINITY);
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            if (indegree[vertex] == 0) {
                distance[vertex] = 0;
            }
        }
        for (int pass = 1; pass < graph.vertexCount(); pass++) {
            boolean changed = false;
            for (Graph.Edge edge : graph.edges()) {
                if (distance[edge.from()] != ShortestPathResult.INFINITY
                        && distance[edge.from()] + edge.weight() < distance[edge.to()]) {
                    distance[edge.to()] = distance[edge.from()] + edge.weight();
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        return distance;
    }

    private static int[] shuffledIdentity(int n, Random random) {
        int[] values = new int[n];
        for (int index = 0; index < n; index++) {
            values[index] = index;
        }
        for (int index = n - 1; index > 0; index--) {
            int other = random.nextInt(index + 1);
            int temporary = values[index];
            values[index] = values[other];
            values[other] = temporary;
        }
        return values;
    }
}

