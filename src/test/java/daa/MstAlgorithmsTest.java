package daa;

import daa.algo.KruskalMst;
import daa.algo.MstResult;
import daa.algo.PrimMst;
import daa.graph.Graph;
import daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Random;
import java.util.ArrayDeque;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MstAlgorithmsTest {
    @Test
    void kruskalAndPrimAgreeOnOneHundredRandomConnectedGraphs() {
        Random random = new Random(42);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(39);
            Graph graph = new Graph(n, false);
            for (int vertex = 1; vertex < n; vertex++) {
                graph.addEdge(random.nextInt(vertex), vertex, random.nextInt(101) - 50);
            }
            int extras = n + random.nextInt(3 * n);
            for (int edge = 0; edge < extras; edge++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(101) - 50);
            }
            MstResult kruskal = KruskalMst.compute(graph, new Metrics());
            MstResult prim = PrimMst.compute(graph, new Metrics());
            assertEquals(kruskal.totalWeight(), prim.totalWeight(), "trial " + trial);
            assertEquals(n - 1, kruskal.edges().size());
            assertEquals(n - 1, prim.edges().size());
            assertEquals(1, kruskal.components());
            assertEquals(1, prim.components());
            assertForest(graph, kruskal);
            assertForest(graph, prim);
        }
    }

    @Test
    void returnsForestForDisconnectedGraph() throws Exception {
        Graph graph = Graph.read(Path.of("data/disconnected-undirected.txt"));
        MstResult kruskal = KruskalMst.compute(graph, new Metrics());
        MstResult prim = PrimMst.compute(graph, new Metrics());
        assertEquals(kruskal.totalWeight(), prim.totalWeight());
        assertEquals(3, kruskal.components());
        assertEquals(3, prim.components());
        assertEquals(graph.vertexCount() - 3, kruskal.edges().size());
        assertEquals(graph.vertexCount() - 3, prim.edges().size());
        assertEquals(7, kruskal.totalWeight());
        assertForest(graph, kruskal);
        assertForest(graph, prim);
    }

    @Test
    void connectedSampleReallyHasOneComponent() throws Exception {
        Graph graph = Graph.read(Path.of("data/connected-undirected.txt"));
        for (MstResult result : new MstResult[]{KruskalMst.compute(graph, new Metrics()),
                PrimMst.compute(graph, new Metrics())}) {
            assertForest(graph, result);
            assertEquals(1, result.components());
            assertEquals(14, result.totalWeight());
        }
    }

    @Test
    void agreesWithExhaustiveForestsOnSmallRandomMultigraphs() {
        Random random = new Random(71);
        for (int trial = 0; trial < 100; trial++) {
            Graph graph = new Graph(1 + random.nextInt(6), false);
            for (int i = 0; i < 9; i++) {
                graph.addEdge(random.nextInt(graph.vertexCount()),
                        random.nextInt(graph.vertexCount()), random.nextInt(21) - 10);
            }
            long expected = bruteForceWeight(graph);
            for (MstResult result : new MstResult[]{KruskalMst.compute(graph, new Metrics()),
                    PrimMst.compute(graph, new Metrics())}) {
                assertEquals(expected, result.totalWeight(), "trial " + trial);
                assertForest(graph, result);
            }
        }
    }

    @Test
    void handlesEmptyGraphsIsolatedVerticesAndLongTotals() {
        for (int n : new int[]{0, 1, 5}) {
            Graph graph = new Graph(n, false);
            assertForest(graph, KruskalMst.compute(graph, new Metrics()));
            assertForest(graph, PrimMst.compute(graph, new Metrics()));
        }
        Graph graph = new Graph(3, false);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 2, Integer.MAX_VALUE);
        graph.addEdge(0, 0, Integer.MIN_VALUE);
        assertEquals(2L * Integer.MAX_VALUE, KruskalMst.compute(graph, new Metrics()).totalWeight());
        assertEquals(2L * Integer.MAX_VALUE, PrimMst.compute(graph, new Metrics()).totalWeight());
        assertThrows(IllegalArgumentException.class,
                () -> KruskalMst.compute(new Graph(2, true), new Metrics()));
        assertThrows(IllegalArgumentException.class,
                () -> PrimMst.compute(new Graph(2, true), new Metrics()));
    }

    private static void assertForest(Graph graph, MstResult result) {
        Graph forest = new Graph(graph.vertexCount(), false);
        HashMap<Graph.Edge, Integer> available = new HashMap<>();
        for (Graph.Edge edge : graph.edges()) {
            // Prim may return the same undirected edge in the opposite direction.
            Graph.Edge key = normalized(edge);
            available.put(key, available.getOrDefault(key, 0) + 1);
        }
        long sum = 0;
        for (Graph.Edge edge : result.edges()) {
            Graph.Edge key = normalized(edge);
            assertTrue(available.getOrDefault(key, 0) > 0, "edge must exist in the input");
            available.put(key, available.get(key) - 1);
            forest.addEdge(edge.from(), edge.to(), edge.weight());
            sum += edge.weight();
        }
        int components = componentCount(graph);
        assertEquals(components, result.components());
        assertEquals(components, componentCount(forest));
        // A spanning subgraph with V-C edges and C components cannot contain a cycle.
        assertEquals(graph.vertexCount() - components, result.edges().size());
        assertEquals(sum, result.totalWeight());
    }

    @Test
    void countsExaminedEdgesAndActualDsuCalls() {
        Graph graph = new Graph(3, false);
        graph.addEdge(0, 0, -5);
        graph.addEdge(0, 1, 1);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 2, 3);
        graph.addEdge(0, 2, 4);
        Metrics kruskal = new Metrics();
        Metrics prim = new Metrics();
        assertEquals(4, KruskalMst.compute(graph, kruskal).totalWeight());
        assertEquals(4, PrimMst.compute(graph, prim).totalWeight());
        assertEquals(4, kruskal.edgeScans());
        assertEquals(12, kruskal.findCalls());
        assertEquals(2, kruskal.successfulUnions());
        assertEquals(14, kruskal.dsuOps());
        assertEquals(10, prim.edgeScans());
        assertEquals(0, prim.dsuOps());
        assertTrue(kruskal.comparisons() > 0);
        assertTrue(prim.comparisons() > 0);
    }

    private static Graph.Edge normalized(Graph.Edge edge) {
        return new Graph.Edge(Math.min(edge.from(), edge.to()),
                Math.max(edge.from(), edge.to()), edge.weight());
    }

    private static long bruteForceWeight(Graph graph) {
        int components = componentCount(graph);
        int needed = graph.vertexCount() - components;
        long best = Long.MAX_VALUE;
        for (int mask = 0; mask < (1 << graph.edgeCount()); mask++) {
            if (Integer.bitCount(mask) != needed) {
                continue;
            }
            Graph candidate = new Graph(graph.vertexCount(), false);
            long weight = 0;
            for (int i = 0; i < graph.edgeCount(); i++) {
                if ((mask & (1 << i)) != 0) {
                    Graph.Edge edge = graph.edges().get(i);
                    candidate.addEdge(edge.from(), edge.to(), edge.weight());
                    weight += edge.weight();
                }
            }
            if (componentCount(candidate) == components) {
                best = Math.min(best, weight);
            }
        }
        return best;
    }

    private static int componentCount(Graph graph) {
        boolean[] visited = new boolean[graph.vertexCount()];
        int components = 0;
        for (int root = 0; root < graph.vertexCount(); root++) {
            if (visited[root]) {
                continue;
            }
            components++;
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(root);
            visited[root] = true;
            while (!queue.isEmpty()) {
                for (Graph.Arc arc : graph.outgoing(queue.remove())) {
                    if (!visited[arc.to()]) {
                        visited[arc.to()] = true;
                        queue.add(arc.to());
                    }
                }
            }
        }
        return components;
    }
}
