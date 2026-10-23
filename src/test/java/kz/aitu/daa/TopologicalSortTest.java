package kz.aitu.daa;

import kz.aitu.daa.algorithms.KahnTopologicalSort;
import kz.aitu.daa.algorithms.TopologicalResult;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopologicalSortTest {
    @Test
    void returnsAndValidatesOrderForDag() throws Exception {
        Graph graph = Graph.read(Path.of("data/dag-negative.txt"));
        Metrics metrics = new Metrics();
        TopologicalResult result = KahnTopologicalSort.sort(graph, metrics);
        assertFalse(result.hasCycle());
        assertTrue(KahnTopologicalSort.isValid(graph, result.order()));
        assertEquals(graph.edgeCount(), metrics.edgeScans());
    }

    @Test
    void reportsCycleInsteadOfReturningWrongOrder() throws Exception {
        Graph graph = Graph.read(Path.of("data/cyclic-directed.txt"));
        TopologicalResult result = KahnTopologicalSort.sort(graph, new Metrics());
        assertTrue(result.hasCycle());
        assertFalse(KahnTopologicalSort.isValid(graph, result.order()));
    }

    @Test
    void handlesParallelEdgesIsolatedVerticesAndSelfLoops() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 2, 1);
        graph.addEdge(0, 2, 2);
        Metrics metrics = new Metrics();
        TopologicalResult order = KahnTopologicalSort.sort(graph, metrics);
        assertFalse(order.hasCycle());
        assertTrue(KahnTopologicalSort.isValid(graph, order.order()));
        assertEquals(2, metrics.edgeScans());
        graph.addEdge(3, 3, 0);
        assertTrue(KahnTopologicalSort.sort(graph, new Metrics()).hasCycle());
        assertThrows(IllegalArgumentException.class,
                () -> KahnTopologicalSort.sort(new Graph(2, false), new Metrics()));
    }

    @Test
    void validatorRejectsWrongDirectionDuplicatesAndUndirectedGraphs() {
        Graph graph = new Graph(3, true);
        graph.addEdge(2, 0, 1);
        assertTrue(KahnTopologicalSort.isValid(graph, List.of(2, 1, 0)));
        assertFalse(KahnTopologicalSort.isValid(graph, List.of(0, 1, 2)));
        assertFalse(KahnTopologicalSort.isValid(graph, List.of(2, 0, 0)));
        assertFalse(KahnTopologicalSort.isValid(graph, List.of(2, 0)));
        assertFalse(KahnTopologicalSort.isValid(graph, List.of(2, -1, 0)));
        assertFalse(KahnTopologicalSort.isValid(graph, List.of(2, 3, 0)));
        Graph undirected = new Graph(2, false);
        undirected.addEdge(0, 1, 1);
        assertFalse(KahnTopologicalSort.isValid(undirected, List.of(0, 1)));
    }

    @Test
    void validatesRandomDagsAndEmptyGraph() {
        Random random = new Random(14);
        for (int trial = 0; trial < 100; trial++) {
            Graph graph = new Graph(30, true);
            for (int u = 0; u < 30; u++) {
                for (int v = u + 1; v < 30; v++) {
                    if (random.nextInt(8) == 0) {
                        graph.addEdge(u, v, 1);
                    }
                }
            }
            TopologicalResult result = KahnTopologicalSort.sort(graph, new Metrics());
            assertFalse(result.hasCycle());
            int[] positions = new int[30];
            assertEquals(30, result.order().stream().distinct().count());
            for (int index = 0; index < 30; index++) {
                positions[result.order().get(index)] = index;
            }
            for (Graph.Edge edge : graph.edges()) {
                assertTrue(positions[edge.from()] < positions[edge.to()]);
            }
        }
        TopologicalResult empty = KahnTopologicalSort.sort(new Graph(0, true), new Metrics());
        assertFalse(empty.hasCycle());
        assertTrue(empty.order().isEmpty());
    }
}
