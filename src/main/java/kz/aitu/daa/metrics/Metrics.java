package kz.aitu.daa.metrics;

/** Mutable operation counters updated inside the algorithms. */
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

