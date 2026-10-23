package daa;

import daa.algo.DagShortestPaths;
import daa.algo.KosarajuScc;
import daa.algo.KruskalMst;
import daa.bench.GraphGenerator;
import daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import daa.graph.Graph;
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
        long[] distances = DagShortestPaths.compute(GraphGenerator.dag(n, m), new Metrics()).distances();
        long sum = 0;
        for (long distance : distances) {
            sum += distance;
        }
        assertEquals(396_274, sum);
    }

    @Test
    void validatesSizesAndSupportsEmptyAndSingletonGraphs() {
        for (String kind : new String[]{"undirected", "directed", "dag"}) {
            assertThrows(IllegalArgumentException.class, () -> generate(kind, -1, 0));
            assertThrows(IllegalArgumentException.class, () -> generate(kind, 2, -1));
            assertThrows(IllegalArgumentException.class, () -> generate(kind, 0, 1));
            assertThrows(IllegalArgumentException.class, () -> generate(kind, 1, 1));
            assertEquals(0, generate(kind, 0, 0).edgeCount());
            assertEquals(1, generate(kind, 1, 0).vertexCount());
            assertEquals(generate(kind, 8, 30).edges(), generate(kind, 8, 30).edges());
        }
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.undirected(5, 3));
    }

    private static Graph generate(String kind, int n, int m) {
        if (kind.equals("undirected")) {
            return GraphGenerator.undirected(n, m);
        } else if (kind.equals("directed")) {
            return GraphGenerator.directed(n, m);
        } else if (kind.equals("dag")) {
            return GraphGenerator.dag(n, m);
        }
        throw new IllegalArgumentException("unknown graph kind: " + kind);
    }
}
