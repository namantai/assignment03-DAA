package daa;

import daa.ds.DisjointSetUnion;
import daa.metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
            assertEquals(n - successful, dsu.components(), "step " + step);
        }

        HashSet<Integer> distinctLabels = new HashSet<>();
        for (int value : label) {
            distinctLabels.add(value);
        }
        assertEquals(distinctLabels.size(), dsu.components());
        assertEquals(successful, metrics.successfulUnions());
        assertEquals(2_000L * 22, metrics.findCalls());
    }

    @Test
    void countsOnlySuccessfulUnionsAndHandlesEmptySets() {
        Metrics metrics = new Metrics();
        DisjointSetUnion dsu = new DisjointSetUnion(3, metrics);
        assertTrue(dsu.union(0, 1));
        assertFalse(dsu.union(1, 0));
        assertFalse(dsu.union(2, 2));
        assertEquals(2, dsu.components());
        assertEquals(6, metrics.findCalls());
        assertEquals(1, metrics.successfulUnions());
        assertEquals(7, metrics.dsuOps());
        assertEquals(0, DisjointSetUnion.makeSet(0).components());
        assertEquals(1, DisjointSetUnion.makeSet(1).components());
        assertThrows(IllegalArgumentException.class, () -> dsu.find(-1));
        assertThrows(IllegalArgumentException.class, () -> dsu.find(3));
    }
}
