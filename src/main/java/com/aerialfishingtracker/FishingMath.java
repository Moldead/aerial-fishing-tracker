package com.aerialfishingtracker;

final class FishingMath
{
    private FishingMath() { }

    static double tenchChance(long catches)
    {
        if (catches < 0)
        {
            throw new IllegalArgumentException("Negative catch count");
        }
        return -Math.expm1(catches * Math.log1p(-1.0 / 20_000.0));
    }

    // Wiki formula uses integer X = ((Fishing * 2) + Hunter) / 3.
    static double estimatedPearlDenominator(int fishing, int hunter)
    {
        int level = (2 * fishing + hunter) / 3;
        // Approximation for the August 2026 ~50% pearl chance increase.
        return (100.0 - ((level - 40.0) * 25.0 / 59.0)) / 1.5;
    }
}
