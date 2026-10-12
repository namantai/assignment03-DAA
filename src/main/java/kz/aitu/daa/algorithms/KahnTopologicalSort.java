package kz.aitu.daa.algorithms;

import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class KahnTopologicalSort {
    private KahnTopologicalSort() { }

    public static TopologicalResult sort(Graph graph, Metrics metrics) {
        requireDirected(graph);
        int[] indegree = graph.indegrees();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            if (indegree[vertex] == 0) {
                queue.add(vertex);
            }
        }
        ArrayList<Integer> order = new ArrayList<>(graph.vertexCount());
        while (!queue.isEmpty()) {
            int vertex = queue.remove();
            order.add(vertex);
            for (Graph.Arc arc : graph.outgoing(vertex)) {
                metrics.recordEdgeScan();
                if (--indegree[arc.to()] == 0) {
                    queue.add(arc.to());
                }
            }
        }
        return new TopologicalResult(order, order.size() != graph.vertexCount());
    }

    public static boolean isValid(Graph graph, List<Integer> order) {
        if (order.size() != graph.vertexCount()) {
            return false;
        }
        int[] position = new int[graph.vertexCount()];
        boolean[] seen = new boolean[graph.vertexCount()];
        for (int index = 0; index < order.size(); index++) {
            int vertex = order.get(index);
            if (vertex < 0 || vertex >= graph.vertexCount() || seen[vertex]) {
                return false;
            }
            seen[vertex] = true;
            position[vertex] = index;
        }
        for (Graph.Edge edge : graph.edges()) {
            if (position[edge.from()] >= position[edge.to()]) {
                return false;
            }
        }
        return true;
    }

    private static void requireDirected(Graph graph) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("topological ordering requires a directed graph");
        }
    }
}

