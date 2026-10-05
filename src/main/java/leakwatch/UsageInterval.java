package leakwatch;

import java.time.Duration;
import java.time.LocalDateTime;

/** Water usage between two consecutive meter readings. */
public final class UsageInterval {
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final double gallonsUsed;

    public UsageInterval(LocalDateTime start, LocalDateTime end, double gallonsUsed) {
        this.start = start;
        this.end = end;
        this.gallonsUsed = gallonsUsed;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public double getGallonsUsed() {
        return gallonsUsed;
    }

    public double getHours() {
        return Duration.between(start, end).toSeconds() / 3600.0;
    }

    /** Average flow rate in gallons per hour over this interval. */
    public double getRateGallonsPerHour() {
        double hours = getHours();
        return hours <= 0 ? 0 : gallonsUsed / hours;
    }

    /**
     * Whether this interval falls entirely within a same-day [quietStart, quietEnd) clock window
     * (e.g. 1:00-5:00 AM). Only supports windows that don't cross midnight, which covers the
     * common "everyone's asleep" case this check exists for.
     */
    public boolean isEntirelyWithin(java.time.LocalTime quietStart, java.time.LocalTime quietEnd) {
        java.time.LocalTime startTime = start.toLocalTime();
        java.time.LocalTime endTime = end.toLocalTime();
        return start.toLocalDate().equals(end.toLocalDate())
                && !startTime.isBefore(quietStart) && !endTime.isAfter(quietEnd);
    }
}
