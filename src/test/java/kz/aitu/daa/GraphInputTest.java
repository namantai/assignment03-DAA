package kz.aitu.daa;

import kz.aitu.daa.graph.Graph;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphInputTest {
    @Test
    void readsAllProvidedGraphFiles() throws Exception {
        Graph connected = Graph.read(Path.of("data/connected-undirected.txt"));
        Graph disconnected = Graph.read(Path.of("data/disconnected-undirected.txt"));
        Graph cyclic = Graph.read(Path.of("data/cyclic-directed.txt"));
        Graph dag = Graph.read(Path.of("data/dag-negative.txt"));

        assertEquals(5, connected.vertexCount());
        assertEquals(7, connected.edgeCount());
        assertFalse(connected.isDirected());
        assertEquals(6, disconnected.vertexCount());
        assertEquals(5, disconnected.edgeCount());
        assertTrue(cyclic.isDirected());
        assertEquals(7, cyclic.edgeCount());
        assertTrue(dag.isDirected());
        assertEquals(-4, dag.edges().get(1).weight());
    }
}

