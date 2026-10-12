package kz.aitu.daa.graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * A weighted adjacency-list graph. Vertices are numbered from 0 to n - 1.
 * The same type supports directed and undirected graphs.
 */
public final class Graph {
    public record Edge(int from, int to, int weight) { }
    public record Arc(int to, int weight) { }

    private final int vertexCount;
    private final boolean directed;
    private final ArrayList<ArrayList<Arc>> adjacency;
    private final ArrayList<Edge> edges;

    public Graph(int vertexCount, boolean directed) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("vertexCount must be non-negative");
        }
        this.vertexCount = vertexCount;
        this.directed = directed;
        this.adjacency = new ArrayList<>(vertexCount);
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            adjacency.add(new ArrayList<>());
        }
        this.edges = new ArrayList<>();
    }

    public int vertexCount() {
        return vertexCount;
    }

    public int edgeCount() {
        return edges.size();
    }

    public boolean isDirected() {
        return directed;
    }

    public void addEdge(int from, int to, int weight) {
        checkVertex(from);
        checkVertex(to);
        edges.add(new Edge(from, to, weight));
        adjacency.get(from).add(new Arc(to, weight));
        if (!directed) {
            adjacency.get(to).add(new Arc(from, weight));
        }
    }

    public List<Arc> outgoing(int vertex) {
        checkVertex(vertex);
        return Collections.unmodifiableList(adjacency.get(vertex));
    }

    public List<Edge> edges() {
        return Collections.unmodifiableList(edges);
    }

    public int[] indegrees() {
        int[] indegree = new int[vertexCount];
        if (directed) {
            for (Edge edge : edges) {
                indegree[edge.to()]++;
            }
        } else {
            for (int vertex = 0; vertex < vertexCount; vertex++) {
                for (Arc arc : adjacency.get(vertex)) {
                    indegree[arc.to()]++;
                }
            }
        }
        return indegree;
    }

    public Graph reverse() {
        Graph reversed = new Graph(vertexCount, directed);
        for (Edge edge : edges) {
            if (directed) {
                reversed.addEdge(edge.to(), edge.from(), edge.weight());
            } else {
                reversed.addEdge(edge.from(), edge.to(), edge.weight());
            }
        }
        return reversed;
    }

    public static Graph read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String header = nextDataLine(reader);
            if (header == null) {
                throw new IOException("Empty graph file: " + path);
            }
            StringTokenizer tokens = new StringTokenizer(header);
            if (tokens.countTokens() != 3) {
                throw new IOException("Header must be: n m directed|undirected");
            }
            int n = parseInt(tokens.nextToken(), "vertex count");
            int m = parseInt(tokens.nextToken(), "edge count");
            String kind = tokens.nextToken().toLowerCase(Locale.ROOT);
            boolean directed;
            if (kind.equals("directed")) {
                directed = true;
            } else if (kind.equals("undirected")) {
                directed = false;
            } else {
                throw new IOException("Graph kind must be directed or undirected");
            }
            if (m < 0) {
                throw new IOException("edge count must be non-negative");
            }

            Graph graph = new Graph(n, directed);
            for (int index = 0; index < m; index++) {
                String line = nextDataLine(reader);
                if (line == null) {
                    throw new IOException("Expected " + m + " edges, found " + index);
                }
                StringTokenizer edgeTokens = new StringTokenizer(line);
                if (edgeTokens.countTokens() != 3) {
                    throw new IOException("Edge line must be: u v w");
                }
                int from = parseInt(edgeTokens.nextToken(), "edge start");
                int to = parseInt(edgeTokens.nextToken(), "edge end");
                int weight = parseInt(edgeTokens.nextToken(), "edge weight");
                try {
                    graph.addEdge(from, to, weight);
                } catch (IllegalArgumentException exception) {
                    throw new IOException("Invalid edge at index " + index + ": " + exception.getMessage(), exception);
                }
            }
            if (nextDataLine(reader) != null) {
                throw new IOException("Graph file contains more than " + m + " edges");
            }
            return graph;
        }
    }

    private static String nextDataLine(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            int comment = line.indexOf('#');
            if (comment >= 0) {
                line = line.substring(0, comment);
            }
            line = line.trim();
            if (!line.isEmpty()) {
                return line;
            }
        }
        return null;
    }

    private static int parseInt(String value, String label) throws IOException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid " + label + ": " + value, exception);
        }
    }

    private void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= vertexCount) {
            throw new IllegalArgumentException(
                    "vertex " + vertex + " is outside 0.." + (vertexCount - 1));
        }
    }
}

