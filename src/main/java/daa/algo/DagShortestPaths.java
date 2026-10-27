package daa.algo;

import daa.graph.Graph;
import daa.metrics.Metrics;

import java.util.Arrays;

public final class DagShortestPaths {
    private DagShortestPaths() { }

    public static ShortestPathResult compute(Graph graph, Metrics metrics) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("DAG shortest paths require a directed graph");
        }

        TopologicalResult topological = KahnTopologicalSort.sort(graph, new Metrics());
        if (topological.hasCycle()) {
            throw new IllegalArgumentException("graph contains a directed cycle");
        }

        int[] indegree = graph.indegrees();
        long[] distance = new long[graph.vertexCount()];
        int[] parent = new int[graph.vertexCount()];
        Arrays.fill(distance, ShortestPathResult.INFINITY);
        Arrays.fill(parent, -1);
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            if (indegree[vertex] == 0) {
                distance[vertex] = 0;
            }
        }

        for (int vertex : topological.order()) {
            for (Graph.Arc arc : graph.outgoing(vertex)) {
                metrics.recordEdgeScan();
                if (distance[vertex] != ShortestPathResult.INFINITY) {
                    long candidate = distance[vertex] + arc.weight();
                    if (candidate < distance[arc.to()]) {
                        distance[arc.to()] = candidate;
                        parent[arc.to()] = vertex;
                    }
                }
            }
        }
        return new ShortestPathResult(distance, parent);
    }
}
