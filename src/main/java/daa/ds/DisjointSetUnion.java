package daa.ds;

import daa.metrics.Metrics;

import java.util.Arrays;

/** DSU with both union by size and path compression. */
public final class DisjointSetUnion {
    private final int[] parent;
    private final int[] size;
    private final Metrics metrics;
    private int components;

    public DisjointSetUnion(int n, Metrics metrics) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative");
        }
        this.parent = new int[n];
        this.size = new int[n];
        this.metrics = metrics;
        this.components = n;
        for (int vertex = 0; vertex < n; vertex++) {
            parent[vertex] = vertex;
        }
        Arrays.fill(size, 1);
    }

    public static DisjointSetUnion makeSet(int n) {
        return new DisjointSetUnion(n, new Metrics());
    }

    public int find(int vertex) {
        checkVertex(vertex);
        metrics.recordFindCall();
        int root = vertex;
        while (root != parent[root]) {
            root = parent[root];
        }
        while (vertex != root) {
            int next = parent[vertex];
            parent[vertex] = root;
            vertex = next;
        }
        return root;
    }

    public boolean union(int first, int second) {
        int firstRoot = find(first);
        int secondRoot = find(second);
        if (firstRoot == secondRoot) {
            return false;
        }
        if (size[firstRoot] < size[secondRoot]) {
            int temporary = firstRoot;
            firstRoot = secondRoot;
            secondRoot = temporary;
        }
        parent[secondRoot] = firstRoot;
        size[firstRoot] += size[secondRoot];
        components--;
        metrics.recordSuccessfulUnion();
        return true;
    }

    public int components() {
        return components;
    }

    private void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= parent.length) {
            throw new IllegalArgumentException("vertex outside the DSU: " + vertex);
        }
    }
}
