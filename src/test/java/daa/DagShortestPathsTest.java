package daa;

import daa.algo.DagShortestPaths;
import daa.algo.ShortestPathResult;
import daa.graph.Graph;
import daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.List;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            assertPaths(graph, actual);
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

    @Test
    void restoresKnownNegativePathAndHandlesDisconnectedDag() throws Exception {
        Graph sample = Graph.read(Path.of("data/dag-negative.txt"));
        ShortestPathResult result = DagShortestPaths.compute(sample, new Metrics());
        assertArrayEquals(new long[]{0, 0, -4, -3, 1, -5}, result.distances());
        assertEquals(List.of(1, 2, 3, 5), result.pathTo(5));
        assertPaths(sample, result);

        Graph graph = new Graph(6, true);
        graph.addEdge(0, 2, 20);
        graph.addEdge(1, 2, -7);
        graph.addEdge(1, 2, -3);
        graph.addEdge(3, 4, 5);
        result = DagShortestPaths.compute(graph, new Metrics());
        assertArrayEquals(new long[]{0, 0, -7, 0, 5, 0}, result.distances());
        assertEquals(List.of(5), result.pathTo(5));
        assertPaths(graph, result);
    }

    @Test
    void supportsLongDistancesEmptyGraphsAndRejectsSelfLoop() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 2, Integer.MAX_VALUE);
        graph.addEdge(2, 3, Integer.MIN_VALUE);
        ShortestPathResult result = DagShortestPaths.compute(graph, new Metrics());
        assertEquals(2L * Integer.MAX_VALUE, result.distanceTo(2));
        assertEquals(2L * Integer.MAX_VALUE + Integer.MIN_VALUE, result.distanceTo(3));
        assertPaths(graph, result);
        assertEquals(0, DagShortestPaths.compute(new Graph(0, true), new Metrics()).distances().length);
        graph.addEdge(3, 3, -1);
        assertThrows(IllegalArgumentException.class, () -> DagShortestPaths.compute(graph, new Metrics()));
        assertThrows(IllegalArgumentException.class,
                () -> DagShortestPaths.compute(new Graph(2, false), new Metrics()));
    }

    private static void assertPaths(Graph graph, ShortestPathResult result) {
        int[] indegree = graph.indegrees();
        int[] parents = result.parents();
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            List<Integer> path = result.pathTo(vertex);
            assertEquals(vertex, path.get(path.size() - 1));
            assertEquals(0, indegree[path.get(0)], "path must start at a source");
            assertEquals(-1, parents[path.get(0)]);
            assertEquals(path.size(), new HashSet<>(path).size());
            long sum = 0;
            for (int i = 1; i < path.size(); i++) {
                int from = path.get(i - 1);
                int to = path.get(i);
                assertEquals(from, parents[to]);
                long difference = result.distanceTo(to) - result.distanceTo(from);
                boolean edgeExists = false;
                for (Graph.Arc arc : graph.outgoing(from)) {
                    if (arc.to() == to && arc.weight() == difference) {
                        edgeExists = true;
                        break;
                    }
                }
                assertTrue(edgeExists);
                sum += difference;
            }
            assertEquals(result.distanceTo(vertex), sum);
        }
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
