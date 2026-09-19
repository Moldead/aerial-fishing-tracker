package com.aerialfishingtracker;

import org.junit.Test;
import static org.junit.Assert.*;

public class FishingSessionTest
{
    @Test
    public void displayedRatesStayFixedUntilFiveSecondsAndIncludeNewCatches()
    {
        long second = java.time.Duration.ofSeconds(1).toNanos();
        long timeout = FishingSession.timeoutNanos(15);
        FishingSession session = new FishingSession();
        session.recordCatch(0, false, timeout);
        session.recordCatch(10 * second, true, timeout);
        assertEquals(720.0, session.fishPerHour(10 * second), 0.0);
        assertEquals(360.0, session.pearlsPerHour(10 * second), 0.0);
        session.recordCatch(12 * second, true, timeout);
        assertEquals(720.0, session.fishPerHour(15 * second - 1), 0.0);
        assertEquals(360.0, session.pearlsPerHour(15 * second - 1), 0.0);
        assertEquals(480.0, session.pearlsPerHour(15 * second), 0.0);
        assertEquals(720.0, session.fishPerHour(15 * second), 0.0);
        assertEquals(540.0, session.fishPerHour(20 * second), 0.0);
        session.reset();
        assertTrue(Double.isNaN(session.fishPerHour(20 * second)));
        assertTrue(Double.isNaN(session.pearlsPerHour(20 * second)));
    }

    @Test
    public void hourlyRatesUseSessionTimeAndResetWithSession()
    {
        long hour = java.time.Duration.ofHours(1).toNanos();
        long timeout = FishingSession.timeoutNanos(120);
        FishingSession session = new FishingSession();
        assertTrue(Double.isNaN(session.fishPerHour(0)));
        session.recordCatch(0, true, timeout);
        assertTrue(Double.isNaN(session.pearlsPerHour(1)));
        session.recordCatch(hour, false, timeout);
        assertEquals(2.0, session.fishPerHour(hour), 0.0);
        assertEquals(1.0, session.pearlsPerHour(hour), 0.0);
        assertEquals(1.0, session.fishPerHour(2 * hour), 0.0);
        session.reset();
        assertTrue(Double.isNaN(session.fishPerHour(2 * hour)));
        session.recordCatch(3 * hour, false, timeout);
        session.recordCatch(4 * hour, false, timeout);
        assertEquals(2.0, session.fishPerHour(4 * hour), 0.0);
        assertEquals(0.0, session.pearlsPerHour(4 * hour), 0.0);
        session.expire(6 * hour, timeout);
        assertTrue(Double.isNaN(session.fishPerHour(6 * hour)));
    }

    @Test
    public void exactConfiguredTimeoutBoundary()
    {
        long timeout = FishingSession.timeoutNanos(15);
        FishingSession session = new FishingSession();
        session.recordCatch(0, true, timeout);
        session.expire(timeout - 1, timeout);
        assertEquals(1, session.catches);
        session.expire(timeout, timeout);
        assertEquals(0, session.catches);
        assertEquals(0, session.pearls);
    }

    @Test
    public void firstCatchAfterIdleStartsNewSessionAndIsIncluded()
    {
        FishingSession session = new FishingSession();
        long timeout = FishingSession.timeoutNanos(15);
        session.recordCatch(0, true, timeout);
        session.recordCatch(timeout, false, timeout);
        assertEquals(1, session.catches);
        assertEquals(0, session.pearls);
    }

    @Test
    public void everyCatchExtendsSessionAndResetOnlyTouchesSession()
    {
        FishingSession session = new FishingSession();
        long timeout = FishingSession.timeoutNanos(15);
        FishingStats stats = new FishingStats();
        stats.recordCatch(true, 1);
        session.recordCatch(0, true, timeout);
        session.recordCatch(timeout - 1, false, timeout);
        session.expire(timeout + 1, timeout);
        assertEquals(2, session.catches);
        session.reset();
        assertEquals(0, session.catches);
        assertEquals(1, stats.totalCatches());
        assertEquals(1, stats.totalPearls());
    }

    @Test
    public void timeoutClampsToSafeMinuteRange()
    {
        assertEquals(FishingSession.timeoutNanos(1), FishingSession.timeoutNanos(0));
        assertEquals(FishingSession.timeoutNanos(1440), FishingSession.timeoutNanos(2000));
    }


}
