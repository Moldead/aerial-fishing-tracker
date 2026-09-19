package com.aerialfishingtracker;

import org.junit.Test;
import static org.junit.Assert.*;

public class FishingStatsTest
{
    @Test
    public void countsCatchesAndCompletePearlIntervalsWithoutOffByOne()
    {
        FishingStats stats = new FishingStats();
        stats.recordCatch(false, 0);
        stats.recordCatch(false, 0);
        assertEquals(2, stats.longestDry);
        stats.recordCatch(true, 0);
        assertEquals(3, stats.longestDry);
        assertEquals(0, stats.sincePearl);
        assertEquals(0, stats.shortestGap); // first observed interval has an unknown starting point
        stats.recordCatch(false, 0);
        stats.recordCatch(true, 0);
        assertEquals(2, stats.shortestGap);
        stats.recordCatch(true, 0);
        assertEquals(1, stats.shortestGap);
        assertEquals(6, stats.catches);
        assertEquals(3, stats.pearls);
        stats.validate();
    }

    @Test
    public void historyChangesLifetimeTotalsWithoutChangingTrackedStreaks()
    {
        FishingStats stats = new FishingStats();
        stats.recordCatch(true, 2);
        stats.recordCatch(false, 0);
        stats.addHistory(10000, 500, 3);
        assertEquals(10002, stats.totalCatches());
        assertEquals(501, stats.totalPearls());
        assertEquals(5, stats.totalTenches());
        assertEquals(10002.0 / 501.0, stats.actualPearlDenominator(), 0.0);
        assertEquals(2, stats.catches);
        assertEquals(1, stats.pearls);
        assertEquals(1, stats.sincePearl);
        assertEquals(0, stats.shortestGap);
    }

    @Test
    public void historyIsAdditiveAndRejectsNegativeOrOverflowAtomically()
    {
        FishingStats stats = new FishingStats();
        stats.addHistory(40, 1, 0);
        stats.addHistory(20, 2, 1);
        assertEquals(60, stats.totalCatches());
        assertThrows(IllegalArgumentException.class, () -> stats.addHistory(-1, 0, 0));
        assertThrows(ArithmeticException.class, () -> stats.addHistory(4, Long.MAX_VALUE, 0));
        assertEquals(60, stats.totalCatches());
        assertEquals(3, stats.totalPearls());
    }

    @Test
    public void actualPearlRateIncludesHistory()
    {
        FishingStats stats = new FishingStats();
        stats.addHistory(16000, 0, 0);
        assertTrue(Double.isNaN(stats.actualPearlDenominator()));
        stats.addHistory(0, 200, 0);
        assertEquals(80.0, stats.actualPearlDenominator(), 0.0);
    }

    @Test
    public void overflowingCatchCannotPartiallyMutateLifetime()
    {
        FishingStats stats = new FishingStats();
        stats.addHistory(Long.MAX_VALUE, 0, 0);
        assertThrows(ArithmeticException.class, () -> stats.recordCatch(true, 1));
        assertEquals(0, stats.catches);
        assertEquals(0, stats.pearls);
        assertEquals(0, stats.tenches);
    }

    @Test
    public void longestDryIncludesAnUnfinishedRun()
    {
        FishingStats stats = new FishingStats();
        stats.recordCatch(true, 0);
        for (int i = 0; i < 500; i++) { stats.recordCatch(false, 0); }
        assertEquals(500, stats.longestDry);
        stats.recordCatch(true, 0);
        assertEquals(501, stats.longestDry);
        assertEquals(501, stats.shortestGap);
    }
}
