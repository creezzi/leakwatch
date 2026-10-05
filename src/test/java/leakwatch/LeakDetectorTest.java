package leakwatch;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeakDetectorTest {

    private final LeakDetector detector = new LeakDetector();

    @Test
    void flagsSustainedFlowEntirelyInsideQuietHours() {
        UsageInterval overnight = new UsageInterval(
                LocalDateTime.of(2026, 1, 1, 2, 0), LocalDateTime.of(2026, 1, 1, 3, 0), 10.0);

        List<UsageInterval> flagged = detector.detectQuietHoursFlow(
                List.of(overnight), LocalTime.of(1, 0), LocalTime.of(5, 0), 2.0);

        assertEquals(1, flagged.size());
    }

    @Test
    void ignoresQuietHoursUsageBelowThreshold() {
        UsageInterval tiny = new UsageInterval(
                LocalDateTime.of(2026, 1, 1, 2, 0), LocalDateTime.of(2026, 1, 1, 3, 0), 0.5);

        assertTrue(detector.detectQuietHoursFlow(
                List.of(tiny), LocalTime.of(1, 0), LocalTime.of(5, 0), 2.0).isEmpty());
    }

    @Test
    void ignoresUsageOutsideTheQuietWindowEvenIfLarge() {
        UsageInterval daytime = new UsageInterval(
                LocalDateTime.of(2026, 1, 1, 14, 0), LocalDateTime.of(2026, 1, 1, 15, 0), 50.0);

        assertTrue(detector.detectQuietHoursFlow(
                List.of(daytime), LocalTime.of(1, 0), LocalTime.of(5, 0), 2.0).isEmpty());
    }

    @Test
    void ignoresIntervalsThatOnlyPartiallyOverlapTheQuietWindow() {
        UsageInterval spanning = new UsageInterval(
                LocalDateTime.of(2026, 1, 1, 4, 30), LocalDateTime.of(2026, 1, 1, 6, 0), 10.0);

        assertTrue(detector.detectQuietHoursFlow(
                List.of(spanning), LocalTime.of(1, 0), LocalTime.of(5, 0), 2.0).isEmpty());
    }

    @Test
    void flagsRatesFarAboveTheMeanAsStatisticalAnomalies() {
        // A single huge outlier inflates its own mean/stddev, so a healthy baseline of normal
        // readings (rather than just one or two) is needed for the outlier to still stand out -
        // this dataset mirrors what a real multi-day reading history looks like.
        List<UsageInterval> intervals = new java.util.ArrayList<>();
        for (int hour = 0; hour < 15; hour++) {
            intervals.add(interval(hour, hour + 1, 2)); // steady ~2 gal/hr baseline
        }
        intervals.add(interval(15, 16, 60)); // a burst pipe, way outside the baseline

        List<UsageInterval> anomalies = detector.detectStatisticalAnomalies(intervals, 3.0);

        assertEquals(1, anomalies.size());
        assertEquals(60.0, anomalies.get(0).getGallonsUsed(), 1e-9);
    }

    @Test
    void noAnomaliesWhenUsageIsUniform() {
        List<UsageInterval> intervals = List.of(interval(0, 1, 5), interval(1, 2, 5), interval(2, 3, 5));

        assertTrue(detector.detectStatisticalAnomalies(intervals, 3.0).isEmpty());
    }

    @Test
    void tooFewIntervalsProducesNoAnomalies() {
        assertTrue(detector.detectStatisticalAnomalies(List.of(interval(0, 1, 100)), 3.0).isEmpty());
    }

    private UsageInterval interval(int startHour, int endHour, double gallons) {
        LocalDateTime day = LocalDateTime.of(2026, 1, 1, 0, 0);
        return new UsageInterval(day.plusHours(startHour), day.plusHours(endHour), gallons);
    }
}
