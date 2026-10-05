package leakwatch;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsageAnalyzerTest {

    private final UsageAnalyzer analyzer = new UsageAnalyzer();

    @Test
    void computesDeltaBetweenConsecutiveReadings() {
        List<MeterReading> readings = List.of(
                new MeterReading(LocalDateTime.of(2026, 1, 1, 0, 0), 100),
                new MeterReading(LocalDateTime.of(2026, 1, 1, 1, 0), 105)
        );

        List<UsageInterval> intervals = analyzer.toIntervals(readings);

        assertEquals(1, intervals.size());
        assertEquals(5.0, intervals.get(0).getGallonsUsed(), 1e-9);
    }

    @Test
    void sortsOutOfOrderReadingsByTimestampFirst() {
        List<MeterReading> readings = List.of(
                new MeterReading(LocalDateTime.of(2026, 1, 1, 1, 0), 105),
                new MeterReading(LocalDateTime.of(2026, 1, 1, 0, 0), 100)
        );

        List<UsageInterval> intervals = analyzer.toIntervals(readings);

        assertEquals(1, intervals.size());
        assertEquals(5.0, intervals.get(0).getGallonsUsed(), 1e-9);
    }

    @Test
    void skipsIntervalsWhereTheMeterAppearsToGoBackward() {
        List<MeterReading> readings = List.of(
                new MeterReading(LocalDateTime.of(2026, 1, 1, 0, 0), 100),
                new MeterReading(LocalDateTime.of(2026, 1, 1, 1, 0), 50), // reset/rollover
                new MeterReading(LocalDateTime.of(2026, 1, 1, 2, 0), 60)
        );

        List<UsageInterval> intervals = analyzer.toIntervals(readings);

        assertEquals(1, intervals.size());
        assertEquals(10.0, intervals.get(0).getGallonsUsed(), 1e-9);
    }

    @Test
    void rateGallonsPerHourAccountsForIntervalDuration() {
        MeterReading a = new MeterReading(LocalDateTime.of(2026, 1, 1, 0, 0), 0);
        MeterReading b = new MeterReading(LocalDateTime.of(2026, 1, 1, 2, 0), 10);

        List<UsageInterval> intervals = analyzer.toIntervals(List.of(a, b));

        assertEquals(5.0, intervals.get(0).getRateGallonsPerHour(), 1e-9);
    }
}
