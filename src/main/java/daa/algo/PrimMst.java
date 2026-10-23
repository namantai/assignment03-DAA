package daa.algo;

import daa.graph.Graph;
import daa.metrics.Metrics;

import java.util.ArrayList;
import java.util.PriorityQueue;

public final class PrimMst {
    private record Candidate(int from, int to, int weight) { }

    private PrimMst() { }

    public static MstResult compute(Graph graph, Metrics metrics) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("MST requires an undirected graph");
        }
        boolean[] inForest = new boolean[graph.vertexCount()];
        PriorityQueue<Candidate> queue = new PriorityQueue<>((left, right) -> {
            metrics.recordComparison();
            return Integer.compare(left.weight(), right.weight());
        });
        ArrayList<Graph.Edge> forest = new ArrayList<>();
        long totalWeight = 0;
        int components = 0;

        for (int root = 0; root < graph.vertexCount(); root++) {
            if (inForest[root]) {
                continue;
            }
            components++;
            queue.add(new Candidate(-1, root, 0));
            while (!queue.isEmpty()) {
                Candidate candidate = queue.remove();
                if (inForest[candidate.to()]) {
                    continue;
                }
                inForest[candidate.to()] = true;
                if (candidate.from() >= 0) {
                    forest.add(new Graph.Edge(
                            candidate.from(), candidate.to(), candidate.weight()));
                    totalWeight += candidate.weight();
                }
                for (Graph.Arc arc : graph.outgoing(candidate.to())) {
                    metrics.recordEdgeScan();
                    if (!inForest[arc.to()]) {
                        queue.add(new Candidate(candidate.to(), arc.to(), arc.weight()));
                    }
                }
            }
        }
        return new MstResult(totalWeight, forest, components);
    }
}
