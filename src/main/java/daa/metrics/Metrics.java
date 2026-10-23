package daa.metrics;

/**
 * Mutable counters updated inside the measured algorithm phase.
 * edgeScans counts Kruskal's selection loop, Prim's adjacency traversal,
 * both Kosaraju DFS passes, Kahn's queue processing, or DAG relaxation.
 * Graph construction, reversal, in-degree setup and validation are excluded.
 * comparisons counts weight-comparator calls in sorting and the priority queue;
 * maxDepth is the maximum live DFS frame count, not a cumulative operation count.
 */
public final class Metrics {
    private long findCalls;
    private long successfulUnions;
    private long edgeScans;
    private long comparisons;
    private long maxDepth;

    public void recordFindCall() {
        findCalls++;
    }

    public void recordSuccessfulUnion() {
        successfulUnions++;
    }

    public void recordEdgeScan() {
        edgeScans++;
    }

    public void recordComparison() {
        comparisons++;
    }

    public void observeDepth(long depth) {
        maxDepth = Math.max(maxDepth, depth);
    }

    public long findCalls() {
        return findCalls;
    }

    public long successfulUnions() {
        return successfulUnions;
    }

    public long dsuOps() {
        return findCalls + successfulUnions;
    }

    public long edgeScans() {
        return edgeScans;
    }

    public long comparisons() {
        return comparisons;
    }

    public long maxDepth() {
        return maxDepth;
    }
}
