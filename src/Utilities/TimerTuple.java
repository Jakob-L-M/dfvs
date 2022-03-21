package Utilities;

import java.util.Set;

public class TimerTuple {

    private final Timer timer;
    private final Set<Integer> set;

    public TimerTuple(Timer timer, Set<Integer> set) {
        this.timer = timer;
        this.set = set;
    }

    public Set<Integer> getSolution() {
        return set;
    }

    public Timer getTimer() {
        return timer;
    }
}
