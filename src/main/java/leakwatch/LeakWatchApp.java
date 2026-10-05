package leakwatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI: {@code java -cp out/main leakwatch.LeakWatchApp <readings.csv> [dollarsPerThousandGallons]}
 *
 * <p>Expects a CSV with a header row {@code timestamp,gallons}, timestamps in ISO-8601
 * (e.g. {@code 2026-01-01T01:00:00}), and gallons as the meter's cumulative reading.
 */
public final class LeakWatchApp {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: LeakWatchApp <readings.csv> [dollarsPerThousandGallons]");
            return;
        }
        double rate = args.length > 1 ? Double.parseDouble(args[1]) : 8.0;

        List<MeterReading> readings = readCsv(Path.of(args[0]));
        List<UsageInterval> intervals = new UsageAnalyzer().toIntervals(readings);

        LeakDetector detector = new LeakDetector();
        List<UsageInterval> quietHoursLeaks = detector.detectQuietHoursFlow(
                intervals, LocalTime.of(1, 0), LocalTime.of(5, 0), 2.0);
        List<UsageInterval> anomalies = detector.detectStatisticalAnomalies(intervals, 3.0);

        CostEstimator costEstimator = new CostEstimator();

        System.out.println(intervals.size() + " usage interval(s) analyzed.");
        System.out.println();
        System.out.println(quietHoursLeaks.size() + " quiet-hours flow event(s) (possible running toilet/leak):");
        for (UsageInterval leak : quietHoursLeaks) {
            System.out.printf("  %s - %s: %.1f gallons (%.2f gal/hr)%n",
                    leak.getStart(), leak.getEnd(), leak.getGallonsUsed(), leak.getRateGallonsPerHour());
        }
        if (!quietHoursLeaks.isEmpty()) {
            System.out.printf("  Wasted cost so far: $%.2f%n", costEstimator.wastedCost(quietHoursLeaks, rate));
            double avgRate = quietHoursLeaks.stream().mapToDouble(UsageInterval::getRateGallonsPerHour).average().orElse(0);
            System.out.printf("  If it keeps running: ~$%.2f/month%n", costEstimator.projectedMonthlyCost(avgRate, rate));
        }

        System.out.println();
        System.out.println(anomalies.size() + " statistical anomaly/anomalies (unusually high flow):");
        for (UsageInterval anomaly : anomalies) {
            System.out.printf("  %s - %s: %.1f gallons (%.2f gal/hr)%n",
                    anomaly.getStart(), anomaly.getEnd(), anomaly.getGallonsUsed(), anomaly.getRateGallonsPerHour());
        }
    }

    private static List<MeterReading> readCsv(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<MeterReading> readings = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) { // skip header
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",");
            readings.add(new MeterReading(LocalDateTime.parse(parts[0].trim()), Double.parseDouble(parts[1].trim())));
        }
        return readings;
    }
}
