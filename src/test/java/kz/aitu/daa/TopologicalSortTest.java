package kz.aitu.daa;

import kz.aitu.daa.algorithms.KahnTopologicalSort;
import kz.aitu.daa.algorithms.TopologicalResult;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

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
}

