package Utilities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Timer {

    public long[] times;
    private final Map<String, Integer> timeMap;

    public Timer(int slots, String[] names) {
        assert slots == names.length;
        times = new long[slots];
        timeMap = new HashMap<>();
        int c = 0;
        for (String s : names) {
            timeMap.put(s, c);
            c++;
        }
    }

    public void addTime(String name, long time) {
        times[timeMap.get(name)] += time;
    }

    public void addTimer(Timer that) {
        assert that.times.length == this.times.length;
        for (int i = 0; i < times.length; i++) {
            this.times[i] += that.times[i];
        }
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        long total = Arrays.stream(times).sum();
        res.append("time: ").append(utils.round((double)total/1_000_000_000, 4)).append(" ");
        for (String name : timeMap.keySet()) {
            res.append(name).append(": ");
            double percent = utils.round((double) 100 * times[timeMap.get(name)]/total, 4);
            res.append(percent).append("% | ");
        }
        return res.toString();
    }
}
