package kz.aitu.daa;

import kz.aitu.daa.algorithms.DisjointSetUnion;
import kz.aitu.daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisjointSetUnionTest {
    @Test
    void agreesWithSimpleReferenceAfterRandomUnions() {
        int n = 250;
        Metrics metrics = new Metrics();
        DisjointSetUnion dsu = new DisjointSetUnion(n, metrics);
        int[] label = new int[n];
        for (int vertex = 0; vertex < n; vertex++) {
            label[vertex] = vertex;
        }

        Random random = new Random(42);
        int successful = 0;
        for (int step = 0; step < 2_000; step++) {
            int first = random.nextInt(n);
            int second = random.nextInt(n);
            boolean expected = label[first] != label[second];
            boolean actual = dsu.union(first, second);
            assertEquals(expected, actual);
            if (expected) {
                successful++;
                int oldLabel = label[second];
                int newLabel = label[first];
                for (int vertex = 0; vertex < n; vertex++) {
                    if (label[vertex] == oldLabel) {
                        label[vertex] = newLabel;
                    }
                }
            }
            for (int check = 0; check < 10; check++) {
                int x = random.nextInt(n);
                int y = random.nextInt(n);
                assertEquals(label[x] == label[y], dsu.find(x) == dsu.find(y));
            }
        }

        long distinct = java.util.Arrays.stream(label).distinct().count();
        assertEquals(distinct, dsu.components());
        assertEquals(successful, metrics.successfulUnions());
        assertTrue(metrics.findCalls() >= 2_000L * 2);
    }
}

