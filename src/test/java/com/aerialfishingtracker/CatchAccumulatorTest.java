package com.aerialfishingtracker;

import org.junit.Test;
import static org.junit.Assert.*;

public class CatchAccumulatorTest
{
    @Test
    public void duplicatedSignalsRepresentOneCatchAndOnePearl()
    {
        CatchAccumulator accumulator = new CatchAccumulator();
        accumulator.catchSignal();
        accumulator.catchSignal();
        accumulator.inventory(1, 0);
        accumulator.pearlMessage();
        CatchAccumulator.Result result = accumulator.finish();
        assertTrue(result.caught);
        assertTrue(result.pearl);
        assertFalse(accumulator.finish().caught);
    }

    @Test
    public void fullInventoryStillCountsAndGroundLootIsReconciled()
    {
        CatchAccumulator accumulator = new CatchAccumulator();
        accumulator.catchSignal();
        accumulator.ground(1, 2);
        accumulator.tenchMessage();
        CatchAccumulator.Result result = accumulator.finish();
        assertTrue(result.caught);
        assertTrue(result.pearl);
        assertEquals(2, result.tenches);
    }

    @Test
    public void inventoryAloneAndTradeInDoNotCreateCatchOrPearl()
    {
        CatchAccumulator accumulator = new CatchAccumulator();
        accumulator.inventory(100, 1);
        CatchAccumulator.Result result = accumulator.finish();
        assertFalse(result.caught);
        assertFalse(result.pearl);
    }

    @Test
    public void tenchMessageInventoryAndBonusAreNotThreeRolls()
    {
        CatchAccumulator accumulator = new CatchAccumulator();
        accumulator.tenchMessage();
        accumulator.inventory(0, 2);
        assertEquals(2, accumulator.finish().tenches);
    }

    @Test
    public void rewardBeforeCatchIsStillCorrelated()
    {
        CatchAccumulator accumulator = new CatchAccumulator();
        accumulator.pearlMessage();
        accumulator.catchSignal();
        assertTrue(accumulator.finish().pearl);
    }

    @Test
    public void oldLootPickupDuringCatchIsNotANewReward()
    {
        CatchAccumulator accumulator = new CatchAccumulator();
        accumulator.catchSignal();
        accumulator.inventory(1, 1);
        accumulator.ground(-1, -1);
        CatchAccumulator.Result result = accumulator.finish();
        assertFalse(result.pearl);
        assertEquals(0, result.tenches);
    }

    @Test
    public void droppingOldLootDuringCatchIsNotANewReward()
    {
        CatchAccumulator accumulator = new CatchAccumulator();
        accumulator.catchSignal();
        accumulator.inventory(-1, -2);
        accumulator.ground(1, 2);
        CatchAccumulator.Result result = accumulator.finish();
        assertFalse(result.pearl);
        assertEquals(0, result.tenches);
    }

    @Test
    public void newRewardAndOldLootTransferInSameTickCountOnlyNewReward()
    {
        CatchAccumulator accumulator = new CatchAccumulator();
        accumulator.catchSignal();
        accumulator.inventory(101, 2);
        accumulator.ground(-100, -1);
        CatchAccumulator.Result result = accumulator.finish();
        assertTrue(result.pearl);
        assertEquals(1, result.tenches);
    }
}
