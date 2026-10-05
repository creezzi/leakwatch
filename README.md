# LeakWatch

Turns raw water meter readings into leak alerts and a dollar cost, using two independent
detection signals instead of one fragile heuristic.

## The problem

A running toilet or a slow pipe leak can waste tens of thousands of gallons — and hundreds of
dollars — before anyone notices, because nobody watches their water meter hour to hour. The
signal is there in smart-meter data; it just needs to be looked at the right way.

## What it does

Given a series of timestamped cumulative meter readings, LeakWatch runs two checks that catch
different failure modes:

1. **Quiet-hours flow** — any usage interval that falls entirely inside a configurable
   "everyone's asleep" window (e.g. 1-5 AM) and still exceeds a small threshold. Real household
   usage during sleep should be close to zero, so sustained flow then is a strong, simple signal
   of a running toilet or a small pipe leak — no history or statistics required.
2. **Statistical anomalies** — intervals whose flow rate is more than *k* standard deviations
   above the mean rate across the dataset. This catches bursts (a burst pipe, a valve stuck open)
   that happen during the day and so wouldn't trip the quiet-hours check.

Flagged leak intervals are then translated into a dollar cost: what's been wasted so far, and what
it would cost per month if the leak just kept running at the observed rate.

## Design

```
leakwatch/
  MeterReading      one raw (timestamp, cumulative gallons) reading
  UsageInterval     usage between two consecutive readings, plus its flow rate
  UsageAnalyzer     readings -> intervals (sorts by time, skips meter resets/rollovers)
  LeakDetector      the two detection algorithms
  CostEstimator     leak intervals -> dollars wasted / projected monthly cost
  LeakWatchApp      CLI: reads a CSV, prints both leak reports + cost estimate
```

The statistical anomaly check has a real, easy-to-miss subtlety that's covered by a dedicated
test: a single huge outlier inflates its own mean and standard deviation, which can hide it from
a naive threshold check if the rest of the dataset is too small. The test uses a proper baseline
(15 normal readings) specifically to catch a regression where that stops working.

## Running it

Requires a JDK (17+); `lib/` has the JUnit console jar used for tests.

```powershell
.\build.ps1     # Windows
```
```bash
./build.sh      # macOS/Linux
```

```
java -cp out/main leakwatch.LeakWatchApp sample_readings.csv 8.0
```

```
3 quiet-hours flow event(s) (possible running toilet/leak):
  2026-01-01T01:00 - 2026-01-01T02:00: 6.0 gallons (6.00 gal/hr)
  2026-01-01T02:00 - 2026-01-01T03:00: 6.0 gallons (6.00 gal/hr)
  2026-01-01T03:00 - 2026-01-01T04:00: 6.0 gallons (6.00 gal/hr)
  Wasted cost so far: $0.14
  If it keeps running: ~$34.56/month

1 statistical anomaly/anomalies (unusually high flow):
  2026-01-01T18:00 - 2026-01-01T19:00: 70.0 gallons (70.00 gal/hr)
```

## Tests

11 JUnit 5 tests: interval construction (sorting out-of-order readings, skipping meter
resets/rollovers, correct rate-per-hour math), the quiet-hours check (flags sustained flow inside
the window, ignores it below threshold, ignores large daytime usage, ignores intervals only
partially overlapping the window), and the statistical anomaly check (flags a genuine outlier
against a proper baseline, no false positives on uniform usage, handles too-few-data-points
gracefully).

## What I'd change for production

- Real smart-meter API ingestion instead of a CSV, and a rolling baseline instead of one static
  dataset (so the mean/stddev adapt as a household's habits change).
- Push notifications instead of a CLI report.
- Per-fixture leak attribution (which toilet/faucet), which needs sub-metering this dataset doesn't have.
