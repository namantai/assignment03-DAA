package kz.aitu.daa;

import kz.aitu.daa.algorithms.DagShortestPaths;
import kz.aitu.daa.algorithms.KosarajuScc;
import kz.aitu.daa.algorithms.KruskalMst;
import kz.aitu.daa.benchmark.GraphGenerator;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReferenceGeneratorTest {
    @Test
    void matchesFirstSparseReferenceRow() {
        int n = 1_000;
        int m = 4_000;
        assertEquals(147_970,
                KruskalMst.compute(GraphGenerator.undirected(n, m), new Metrics()).totalWeight());
        assertEquals(27,
                KosarajuScc.compute(GraphGenerator.directed(n, m), new Metrics()).componentCount());
        long sum = Arrays.stream(
                DagShortestPaths.compute(GraphGenerator.dag(n, m), new Metrics()).distances()).sum();
        assertEquals(396_274, sum);
    }
}
