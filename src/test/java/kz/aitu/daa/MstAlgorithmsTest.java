package kz.aitu.daa;

import kz.aitu.daa.algorithms.KruskalMst;
import kz.aitu.daa.algorithms.MstResult;
import kz.aitu.daa.algorithms.PrimMst;
import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Random;

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
    }
}

