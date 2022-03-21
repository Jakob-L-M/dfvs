package Utilities;

import java.util.Set;

public class Tuple {

    private final Integer value;
    private final Set<Integer> set;

    public Tuple(Integer value, Set<Integer> set) {
        this.value = value;
        this.set = set;
    }

    public Integer getValue() {
        return value;
    }

    public Set<Integer> getSet() {
        return set;
    }
}
