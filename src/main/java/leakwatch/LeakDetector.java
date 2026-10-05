package leakwatch;

import java.time.LocalTime;
import java.util.List;

/**
 * Two independent leak signals, run over a list of usage intervals:
 *
 * <ul>
 *   <li><b>Quiet-hours flow</b>: any interval entirely inside a configurable "everyone's asleep"
 *       window (e.g. 1-5 AM) that still used more than a small threshold amount. Real household
 *       usage during sleep should be near zero, so sustained flow then is a strong signal of a
 *       running toilet or a pipe leak, independent of any statistics about the rest of the day.
 *   <li><b>Statistical anomalies</b>: intervals whose flow rate is more than {@code k} standard
 *       deviations above the mean rate across the whole dataset - catches bursts (a burst pipe,
 *       a stuck-open valve) that happen outside quiet hours and so wouldn't trip the first check.
 * </ul>
 */
public final class LeakDetector {

    public List<UsageInterval> detectQuietHoursFlow(List<UsageInterval> intervals, LocalTime quietStart,
                                                      LocalTime quietEnd, double thresholdGallons) {
        return intervals.stream()
                .filter(i -> i.isEntirelyWithin(quietStart, quietEnd))
                .filter(i -> i.getGallonsUsed() > thresholdGallons)
                .toList();
    }

    public List<UsageInterval> detectStatisticalAnomalies(List<UsageInterval> intervals, double stdDevMultiplier) {
        if (intervals.size() < 2) {
            return List.of();
        }
        double mean = intervals.stream().mapToDouble(UsageInterval::getRateGallonsPerHour).average().orElse(0);
        double variance = intervals.stream()
                .mapToDouble(i -> Math.pow(i.getRateGallonsPerHour() - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        if (stdDev <= 1e-9) {
            return List.of();
        }
        double threshold = mean + stdDevMultiplier * stdDev;
        return intervals.stream().filter(i -> i.getRateGallonsPerHour() > threshold).toList();
    }
}
