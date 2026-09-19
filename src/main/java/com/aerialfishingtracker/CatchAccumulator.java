package com.aerialfishingtracker;

/** Reconciles signals from one server tick. Bonus items never create extra tench rolls. */
final class CatchAccumulator
{
    private boolean caught;
    private boolean fishingXp;
    private boolean hunterXp;
    private boolean pearlMessage;
    private int tenchMessage;
    private long inventoryPearls;
    private long inventoryTenches;
    private long groundPearls;
    private long groundTenches;

    void catchSignal() { caught = true; }
    void fishingXp() { fishingXp = true; }
    void hunterXp() { hunterXp = true; }
    void pearlMessage() { pearlMessage = true; }
    void tenchMessage() { caught = true; tenchMessage = 1; }

    void inventory(int pearls, int tenches)
    {
        inventoryPearls += pearls;
        inventoryTenches += tenches;
    }

    void ground(int pearls, int tenches)
    {
        groundPearls += pearls;
        groundTenches += tenches;
    }

    Result finish()
    {
        // Moving old loot between ground and inventory has net zero gain. A tench trade/refund
        // has a bulk pearl gain, never the single pearl produced by one successful catch.
        long pearls = inventoryPearls + groundPearls;
        long tenches = inventoryTenches + groundTenches;
        // Rada's blessing and spirit flakes add independent chances to double the selected
        // catch, but their reward quantity is capped at two items.
        int tenchItems = tenches >= 1 && tenches <= 2 ? (int) tenches : 0;
        Result result = new Result(caught || (fishingXp && hunterXp), pearlMessage || pearls == 1,
            Math.max(tenchMessage, tenchItems));
        clear();
        return result;
    }

    void clear()
    {
        caught = false;
        fishingXp = false;
        hunterXp = false;
        pearlMessage = false;
        tenchMessage = 0;
        inventoryPearls = 0;
        inventoryTenches = 0;
        groundPearls = 0;
        groundTenches = 0;
    }

    static final class Result
    {
        final boolean caught;
        final boolean pearl;
        final int tenches;

        Result(boolean caught, boolean pearl, int tenches)
        {
            this.caught = caught;
            this.pearl = pearl;
            this.tenches = tenches;
        }
    }
}
