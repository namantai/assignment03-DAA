package kz.aitu.daa.algorithms;

import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;

import java.util.ArrayList;

public final class KruskalMst {
    private KruskalMst() { }

    public static MstResult compute(Graph graph, Metrics metrics) {
        requireUndirected(graph);
        ArrayList<Graph.Edge> sorted = new ArrayList<>(graph.edges());
        sorted.sort((left, right) -> {
            metrics.recordComparison();
            return Integer.compare(left.weight(), right.weight());
        });

        DisjointSetUnion dsu = new DisjointSetUnion(graph.vertexCount(), metrics);
        ArrayList<Graph.Edge> forest = new ArrayList<>();
        long totalWeight = 0;
        for (Graph.Edge edge : sorted) {
            metrics.recordEdgeScan();
            if (dsu.find(edge.from()) != dsu.find(edge.to())
                    && dsu.union(edge.from(), edge.to())) {
                forest.add(edge);
                totalWeight += edge.weight();
                if (forest.size() == graph.vertexCount() - 1) {
                    break;
                }
            }
        }
        return new MstResult(totalWeight, forest, dsu.components());
    }

    private static void requireUndirected(Graph graph) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("MST requires an undirected graph");
        }
    }
}

