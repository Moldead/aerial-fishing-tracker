package com.aerialfishingtracker;

import org.junit.Test;
import static org.junit.Assert.*;

public class FishingMathTest
{
    @Test
    public void tenchProbabilityMatchesIndependentReferenceValues()
    {
        assertEquals(0, FishingMath.tenchChance(0), 0);
        assertEquals(0.00005, FishingMath.tenchChance(1), 1e-16);
        assertEquals(0.632129756, FishingMath.tenchChance(20000), 1e-9);
        assertEquals(0.55068002267, FishingMath.tenchChance(16000), 1e-11);
        assertEquals(1, FishingMath.tenchChance(Long.MAX_VALUE), 0);
        assertThrows(IllegalArgumentException.class, () -> FishingMath.tenchChance(-1));
    }

    @Test
    public void probabilityIsMonotonic()
    {
        double previous = 0;
        for (long n = 1; n < 100000; n += 37)
        {
            double chance = FishingMath.tenchChance(n);
            assertTrue(chance >= previous && chance <= 1);
            previous = chance;
        }
    }

    @Test
    public void pearlEstimateUsesWeightedBoostedLevels()
    {
        assertEquals(50, FishingMath.estimatedPearlDenominator(99, 99), 1e-12);
        assertEquals(200.0 / 3.0, FishingMath.estimatedPearlDenominator(43, 35), 1e-12);
        // X uses integer division in the game formula.
        assertEquals(FishingMath.estimatedPearlDenominator(43, 35),
            FishingMath.estimatedPearlDenominator(43, 36), 1e-12);
        assertTrue(FishingMath.estimatedPearlDenominator(104, 102) < 50);
        assertTrue(FishingMath.estimatedPearlDenominator(90, 80) < FishingMath.estimatedPearlDenominator(80, 90));
    }
}
