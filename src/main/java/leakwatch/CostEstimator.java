package leakwatch;

import java.util.List;

/** Translates flagged leak intervals into dollar terms a homeowner actually cares about. */
public final class CostEstimator {

    public double wastedCost(List<UsageInterval> leakIntervals, double dollarsPerThousandGallons) {
        double totalGallons = leakIntervals.stream().mapToDouble(UsageInterval::getGallonsUsed).sum();
        return totalGallons / 1000.0 * dollarsPerThousandGallons;
    }

    /** If a leak keeps flowing at this rate nonstop, what would it cost over 30 days? */
    public double projectedMonthlyCost(double leakRateGallonsPerHour, double dollarsPerThousandGallons) {
        double monthlyGallons = leakRateGallonsPerHour * 24 * 30;
        return monthlyGallons / 1000.0 * dollarsPerThousandGallons;
    }
}
