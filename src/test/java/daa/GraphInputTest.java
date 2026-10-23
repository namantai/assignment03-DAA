package daa;

import daa.graph.Graph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class GraphInputTest {
    @Test
    void readsAllProvidedGraphFiles() throws Exception {
        Graph connected = Graph.read(Path.of("data/connected-undirected.txt"));
        Graph disconnected = Graph.read(Path.of("data/disconnected-undirected.txt"));
        Graph cyclic = Graph.read(Path.of("data/cyclic-directed.txt"));
        Graph dag = Graph.read(Path.of("data/dag-negative.txt"));

        assertEquals(5, connected.vertexCount());
        assertEquals(8, connected.edgeCount());
        assertFalse(connected.isDirected());
        assertEquals(6, disconnected.vertexCount());
        assertEquals(5, disconnected.edgeCount());
        assertTrue(cyclic.isDirected());
        assertEquals(7, cyclic.edgeCount());
        assertTrue(dag.isDirected());
        assertEquals(-4, dag.edges().get(1).weight());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-1 0 directed", "1 -1 directed", "1 0 unknown",
            "a 0 directed", "2 1 directed\n0 2 3", "2 1 directed\n-1 0 3",
            "2 1 directed", "2 0 directed\n0 1 3", "2 1 directed\n0 1",
            "2 1 directed\n0 1 2147483648", "2 0 directed extra"})
    void rejectsMalformedFilesWithIOException(String input, @TempDir Path directory) throws Exception {
        Path file = directory.resolve("invalid.txt");
        Files.writeString(file, input);
        assertThrows(IOException.class, () -> Graph.read(file));
    }

    @Test
    void preservesLoopsParallelEdgesWeightsAndReverse() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, -5);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 1, Integer.MIN_VALUE);
        assertArrayEquals(new int[]{0, 3, 0}, graph.indegrees());
        assertEquals(graph.edges(), graph.reverse().reverse().edges());
        assertEquals(3, graph.reverse().outgoing(1).size());
        assertThrows(UnsupportedOperationException.class, () -> graph.edges().clear());
        assertThrows(UnsupportedOperationException.class, () -> graph.outgoing(0).clear());
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(0, 3, 7));
        assertEquals(3, graph.edgeCount());
        Graph undirected = new Graph(2, false);
        undirected.addEdge(0, 0, -2);
        undirected.addEdge(0, 1, 7);
        assertArrayEquals(new int[]{3, 1}, undirected.indegrees());
        assertEquals(2, undirected.edgeCount());
        assertEquals(undirected.edges(), undirected.reverse().edges());
    }
}
