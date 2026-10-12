package kz.aitu.daa.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public record ShortestPathResult(long[] distances, int[] parents) {
    public static final long INFINITY = Long.MAX_VALUE / 4;

    public ShortestPathResult {
        distances = Arrays.copyOf(distances, distances.length);
        parents = Arrays.copyOf(parents, parents.length);
    }

    @Override
    public long[] distances() {
        return Arrays.copyOf(distances, distances.length);
    }

    @Override
    public int[] parents() {
        return Arrays.copyOf(parents, parents.length);
    }

    public long distanceTo(int vertex) {
        return distances[vertex];
    }

    public List<Integer> pathTo(int vertex) {
        if (distances[vertex] == INFINITY) {
            return List.of();
        }
        ArrayList<Integer> path = new ArrayList<>();
        boolean[] seen = new boolean[parents.length];
        for (int current = vertex; current >= 0; current = parents[current]) {
            if (seen[current]) {
                throw new IllegalStateException("parent array contains a cycle");
            }
            seen[current] = true;
            path.add(current);
        }
        Collections.reverse(path);
        return List.copyOf(path);
    }
}

