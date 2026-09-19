package com.aerialfishingtracker;

/** Persistent data only. There is deliberately no reset method. */
final class FishingStats
{
    private static final int CURRENT_SCHEMA = 2;
    int schema = CURRENT_SCHEMA;
    long catches;
    long pearls;
    long tenches;
    long historicalCatches;
    long historicalPearls;
    long historicalTenches;
    long sincePearl;
    long longestDry;
    long shortestGap;
    boolean seenPearl;

    void recordCatch(boolean pearl, int tenchItems)
    {
        if (tenchItems < 0 || tenchItems > 2)
        {
            throw new IllegalArgumentException("Invalid tench drop quantity");
        }
        long nextCatches = Math.addExact(catches, 1);
        long nextTenches = Math.addExact(tenches, tenchItems);
        long nextPearls = Math.addExact(pearls, pearl ? 1 : 0);
        long interval = Math.addExact(sincePearl, 1);
        Math.addExact(nextCatches, historicalCatches);
        Math.addExact(nextTenches, historicalTenches);
        Math.addExact(nextPearls, historicalPearls);
        catches = nextCatches;
        tenches = nextTenches;
        pearls = nextPearls;
        if (pearl)
        {
            longestDry = Math.max(longestDry, interval);
            if (seenPearl && (shortestGap == 0 || interval < shortestGap))
            {
                shortestGap = interval;
            }
            seenPearl = true;
            sincePearl = 0;
        }
        else
        {
            sincePearl = interval;
            longestDry = Math.max(longestDry, sincePearl);
        }
    }

    void addHistory(long fish, long pearl, long tench)
    {
        if (fish < 0 || pearl < 0 || tench < 0)
        {
            throw new IllegalArgumentException("Historical amounts must be non-negative.");
        }
        // Validate every addition before changing any field.
        long nextFish = Math.addExact(historicalCatches, fish);
        long nextPearl = Math.addExact(historicalPearls, pearl);
        long nextTench = Math.addExact(historicalTenches, tench);
        Math.addExact(catches, nextFish);
        Math.addExact(pearls, nextPearl);
        Math.addExact(tenches, nextTench);
        historicalCatches = nextFish;
        historicalPearls = nextPearl;
        historicalTenches = nextTench;
    }

    long totalCatches() { return Math.addExact(catches, historicalCatches); }
    long totalPearls() { return Math.addExact(pearls, historicalPearls); }
    long totalTenches() { return Math.addExact(tenches, historicalTenches); }
    double actualPearlDenominator()
    {
        long total = totalPearls();
        return total == 0 ? Double.NaN : totalCatches() / (double) total;
    }

    void migrate()
    {
        if (schema == 1)
        {
            // Schema 1 counted only failed rolls for completed dry streaks, while shortestGap
            // included the ending pearl. Repair completed legacy streaks without adding a roll
            // to a currently unfinished run that is already the longest.
            if (seenPearl && longestDry > 0 && sincePearl < longestDry)
            {
                longestDry = Math.addExact(longestDry, 1);
            }
            longestDry = Math.max(longestDry, shortestGap);
            schema = CURRENT_SCHEMA;
        }
    }

    void validate()
    {
        if (schema != CURRENT_SCHEMA || catches < 0 || pearls < 0 || tenches < 0 || historicalCatches < 0
            || historicalPearls < 0 || historicalTenches < 0 || sincePearl < 0 || longestDry < sincePearl
            || shortestGap < 0 || pearls > catches || sincePearl > catches || longestDry > catches
            || shortestGap > catches || seenPearl != (pearls > 0))
        {
            throw new IllegalArgumentException("Invalid or unsupported lifetime data; refusing to overwrite it.");
        }
        totalCatches();
        totalPearls();
        totalTenches();
    }
}
