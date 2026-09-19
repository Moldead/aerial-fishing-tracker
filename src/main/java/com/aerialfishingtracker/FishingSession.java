package com.aerialfishingtracker;

import java.time.Duration;

final class FishingSession
{
    static final int DEFAULT_TIMEOUT_MINUTES = 15;
    long catches;
    long pearls;
    private long firstCatch;
    private long lastCatch;
    private boolean active;
    private static final long RATE_REFRESH_NANOS = Duration.ofSeconds(5).toNanos();
    private long lastRateRefresh;
    private boolean ratesInitialized;
    private double displayedFishRate = Double.NaN;
    private double displayedPearlRate = Double.NaN;

    static long timeoutNanos(int minutes)
    {
        return Duration.ofMinutes(Math.max(1, Math.min(1440, minutes))).toNanos();
    }

    void expire(long now, long timeoutNanos)
    {
        if (active && now - lastCatch >= timeoutNanos)
        {
            reset();
        }
    }

    void recordCatch(long now, boolean pearl, long timeoutNanos)
    {
        expire(now, timeoutNanos);
        if (!active)
        {
            firstCatch = now;
        }
        catches++;
        if (pearl)
        {
            pearls++;
        }
        lastCatch = now;
        active = true;
    }

    void reset()
    {
        catches = 0;
        pearls = 0;
        firstCatch = 0;
        lastCatch = 0;
        active = false;
        ratesInitialized = false;
        displayedFishRate = Double.NaN;
        displayedPearlRate = Double.NaN;
    }

    double fishPerHour(long now)
    {
        refreshRates(now);
        return displayedFishRate;
    }

    double pearlsPerHour(long now)
    {
        refreshRates(now);
        return displayedPearlRate;
    }

    private void refreshRates(long now)
    {
        if (!active || catches < 2 || now - firstCatch <= 0) { return; }
        if (ratesInitialized && now - lastRateRefresh < RATE_REFRESH_NANOS) { return; }
        displayedFishRate = perHour(catches, now);
        displayedPearlRate = perHour(pearls, now);
        lastRateRefresh = now;
        ratesInitialized = true;
    }

    private double perHour(long amount, long now)
    {
        long elapsed = now - firstCatch;
        if (!active || catches < 2 || elapsed <= 0)
        {
            return Double.NaN;
        }
        return (double) amount * Duration.ofHours(1).toNanos() / elapsed;
    }
}
