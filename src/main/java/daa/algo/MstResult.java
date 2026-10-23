package daa.algo;

import daa.graph.Graph;

import java.util.List;

public record MstResult(long totalWeight, List<Graph.Edge> edges, int components) {
    public MstResult {
        edges = List.copyOf(edges);
    }
}
