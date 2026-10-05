package leakwatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Turns a series of cumulative meter readings into per-interval usage. */
public final class UsageAnalyzer {

    /**
     * Builds one {@link UsageInterval} per consecutive pair of readings. Readings are sorted by
     * timestamp first. A pair where the later reading is lower than the earlier one (a meter
     * reset/rollover, not a real usage event) is skipped rather than reported as negative usage.
     */
    public List<UsageInterval> toIntervals(List<MeterReading> readings) {
        List<MeterReading> sorted = readings.stream()
                .sorted(Comparator.comparing(MeterReading::getTimestamp))
                .toList();

        List<UsageInterval> intervals = new ArrayList<>();
        for (int i = 0; i < sorted.size() - 1; i++) {
            MeterReading a = sorted.get(i);
            MeterReading b = sorted.get(i + 1);
            double delta = b.getCumulativeGallons() - a.getCumulativeGallons();
            if (delta >= 0) {
                intervals.add(new UsageInterval(a.getTimestamp(), b.getTimestamp(), delta));
            }
        }
        return intervals;
    }
}
