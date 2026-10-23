package daa.bench;

import daa.graph.Graph;

import java.util.Random;

/** The assignment's deterministic Random(42) graph generators. */
public final class GraphGenerator {
    public static final long SEED = 42L;

    private GraphGenerator() { }

    public static Graph undirected(int n, int m) {
        validateSize(n, m);
        if (n > 0 && m < n - 1) {
            throw new IllegalArgumentException("connected generator needs at least n - 1 edges");
        }
        Random random = new Random(SEED);
        Graph graph = new Graph(n, false);
        int[] permutation = shuffledIdentity(n, random);
        for (int index = 1; index < n; index++) {
            int parent = permutation[random.nextInt(index)];
            graph.addEdge(parent, permutation[index], randomWeight(random));
        }
        while (graph.edgeCount() < m) {
            int first = random.nextInt(n);
            int second;
            do {
                second = random.nextInt(n);
            } while (second == first);
            graph.addEdge(first, second, randomWeight(random));
        }
        return graph;
    }

    public static Graph directed(int n, int m) {
        validateSize(n, m);
        Random random = new Random(SEED);
        Graph graph = new Graph(n, true);
        addRandomDirectedEdges(graph, m, random, null);
        return graph;
    }

    public static Graph dag(int n, int m) {
        validateSize(n, m);
        Random random = new Random(SEED);
        int[] rank = shuffledIdentity(n, random);
        Graph graph = new Graph(n, true);
        addRandomDirectedEdges(graph, m, random, rank);
        return graph;
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

    private static void addRandomDirectedEdges(
            Graph graph, int edgeCount, Random random, int[] rank) {
        int n = graph.vertexCount();
        for (int edge = 0; edge < edgeCount; edge++) {
            int first = random.nextInt(n);
            int second;
            do {
                second = random.nextInt(n);
            } while (second == first);
            if (rank != null && rank[first] > rank[second]) {
                int temporary = first;
                first = second;
                second = temporary;
            }
            graph.addEdge(first, second, randomWeight(random));
        }
    }

    private static int randomWeight(Random random) {
        return 1 + random.nextInt(1000);
    }

    private static void validateSize(int n, int m) {
        if (n < 0 || m < 0) {
            throw new IllegalArgumentException("vertex and edge counts must be non-negative");
        }
        if (n < 2 && m > 0) {
            throw new IllegalArgumentException("non-loop edges require at least two vertices");
        }
    }
}
