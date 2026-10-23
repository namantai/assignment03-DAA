package kz.aitu.daa;

import kz.aitu.daa.algorithms.KosarajuScc;
import kz.aitu.daa.algorithms.SccResult;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StackSafetyTest {
    @Test
    void kosarajuHandlesPathOfOneHundredThousandVertices() {
        int n = 100_000;
        Graph graph = new Graph(n, true);
        for (int vertex = 0; vertex + 1 < n; vertex++) {
            graph.addEdge(vertex, vertex + 1, 1);
        }
        Metrics metrics = new Metrics();
        SccResult result = KosarajuScc.compute(graph, metrics);
        assertEquals(n, result.componentCount());
        assertEquals(2L * (n - 1), metrics.edgeScans());
        assertEquals(n, metrics.maxDepth());
    }

    @Test
    void bothDfsPassesHandleOneHundredThousandVertexCycle() {
        int n = 100_000;
        Graph graph = new Graph(n, true);
        for (int vertex = 0; vertex < n; vertex++) {
            graph.addEdge(vertex, (vertex + 1) % n, 1);
        }
        Metrics metrics = new Metrics();
        SccResult result = KosarajuScc.compute(graph, metrics);
        assertEquals(1, result.componentCount());
        assertEquals(2L * n, metrics.edgeScans());
        assertEquals(n, metrics.maxDepth());
        for (int vertex = 0; vertex < n; vertex++) {
            assertEquals(result.componentOf(0), result.componentOf(vertex));
        }
    }
}
