package kz.aitu.daa;

import kz.aitu.daa.algorithms.DagShortestPaths;
import kz.aitu.daa.algorithms.KosarajuScc;
import kz.aitu.daa.algorithms.KruskalMst;
import kz.aitu.daa.benchmark.GraphGenerator;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import kz.aitu.daa.graph.Graph;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void validatesSizesAndSupportsEmptyAndSingletonGraphs() {
        List<BiFunction<Integer, Integer, Graph>> generators = List.of(
                GraphGenerator::undirected, GraphGenerator::directed, GraphGenerator::dag);
        for (BiFunction<Integer, Integer, Graph> generator : generators) {
            assertThrows(IllegalArgumentException.class, () -> generator.apply(-1, 0));
            assertThrows(IllegalArgumentException.class, () -> generator.apply(2, -1));
            assertThrows(IllegalArgumentException.class, () -> generator.apply(0, 1));
            assertThrows(IllegalArgumentException.class, () -> generator.apply(1, 1));
            assertEquals(0, generator.apply(0, 0).edgeCount());
            assertEquals(1, generator.apply(1, 0).vertexCount());
            assertEquals(generator.apply(8, 30).edges(), generator.apply(8, 30).edges());
        }
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.undirected(5, 3));
    }
}
