package daa.algo;

import java.util.Arrays;

public record SccResult(int componentCount, int[] componentOf) {
    public SccResult {
        componentOf = Arrays.copyOf(componentOf, componentOf.length);
    }

    @Override
    public int[] componentOf() {
        return Arrays.copyOf(componentOf, componentOf.length);
    }

    public int componentOf(int vertex) {
        return componentOf[vertex];
    }
}
