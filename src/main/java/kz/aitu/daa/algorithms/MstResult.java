package kz.aitu.daa.algorithms;

import kz.aitu.daa.graph.Graph;

import java.util.List;

public record MstResult(long totalWeight, List<Graph.Edge> edges, int components) {
    public MstResult {
        edges = List.copyOf(edges);
    }
}

