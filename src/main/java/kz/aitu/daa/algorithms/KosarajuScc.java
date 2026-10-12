package kz.aitu.daa.algorithms;

import kz.aitu.daa.graph.Graph;
import kz.aitu.daa.metrics.Metrics;

import java.util.Arrays;
import java.util.List;

/** Stack-safe Kosaraju implementation: both DFS passes use explicit frame stacks. */
public final class KosarajuScc {
    private KosarajuScc() { }

    public static SccResult compute(Graph graph, Metrics metrics) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("SCC requires a directed graph");
        }
        int n = graph.vertexCount();
        boolean[] visited = new boolean[n];
        int[] finishOrder = new int[n];
        int finishSize = 0;
        int[] vertices = new int[Math.max(1, n)];
        int[] nextArc = new int[Math.max(1, n)];

        for (int start = 0; start < n; start++) {
            if (visited[start]) {
                continue;
            }
            int depth = 1;
            vertices[0] = start;
            nextArc[0] = 0;
            visited[start] = true;
            metrics.observeDepth(depth);
            while (depth > 0) {
                int frame = depth - 1;
                int vertex = vertices[frame];
                List<Graph.Arc> outgoing = graph.outgoing(vertex);
                if (nextArc[frame] < outgoing.size()) {
                    Graph.Arc arc = outgoing.get(nextArc[frame]++);
                    metrics.recordEdgeScan();
                    if (!visited[arc.to()]) {
                        visited[arc.to()] = true;
                        vertices[depth] = arc.to();
                        nextArc[depth] = 0;
                        depth++;
                        metrics.observeDepth(depth);
                    }
                } else {
                    finishOrder[finishSize++] = vertex;
                    depth--;
                }
            }
        }

        Graph reversed = graph.reverse();
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int componentCount = 0;
        for (int orderIndex = finishSize - 1; orderIndex >= 0; orderIndex--) {
            int start = finishOrder[orderIndex];
            if (component[start] >= 0) {
                continue;
            }
            int depth = 1;
            vertices[0] = start;
            nextArc[0] = 0;
            component[start] = componentCount;
            metrics.observeDepth(depth);
            while (depth > 0) {
                int frame = depth - 1;
                int vertex = vertices[frame];
                List<Graph.Arc> outgoing = reversed.outgoing(vertex);
                if (nextArc[frame] < outgoing.size()) {
                    Graph.Arc arc = outgoing.get(nextArc[frame]++);
                    metrics.recordEdgeScan();
                    if (component[arc.to()] < 0) {
                        component[arc.to()] = componentCount;
                        vertices[depth] = arc.to();
                        nextArc[depth] = 0;
                        depth++;
                        metrics.observeDepth(depth);
                    }
                } else {
                    depth--;
                }
            }
            componentCount++;
        }
        return new SccResult(componentCount, component);
    }
}

