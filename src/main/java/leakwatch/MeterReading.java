package leakwatch;

import java.time.LocalDateTime;

/** One water meter reading: the cumulative usage (gallons) recorded at a point in time. */
public final class MeterReading {
    private final LocalDateTime timestamp;
    private final double cumulativeGallons;

    public MeterReading(LocalDateTime timestamp, double cumulativeGallons) {
        this.timestamp = timestamp;
        this.cumulativeGallons = cumulativeGallons;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public double getCumulativeGallons() {
        return cumulativeGallons;
    }
}
