package com.aerialfishingtracker;

import java.lang.reflect.Field;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Regression cases written independently after the implementation's first passing build. */
public class IndependentCriticTest
{
    @Test
    public void disablingBetweenCompletionAndTickMustPreserveTheCompletedCatch() throws Exception
    {
        AerialFishingPlugin plugin = new AerialFishingPlugin();
        Client client = mock(Client.class);
        ConfigManager config = mock(ConfigManager.class);
        StatsStore store = mock(StatsStore.class);
        ClientThread thread = mock(ClientThread.class);
        doAnswer(invocation -> { ((Runnable) invocation.getArgument(0)).run(); return null; })
            .when(thread).invoke(any(Runnable.class));
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(config.getRSProfileKey()).thenReturn("critic");
        when(store.load("critic")).thenReturn(new FishingStats());
        field(plugin, "client", client);
        field(plugin, "clientThread", thread);
        field(plugin, "configManager", config);
        field(plugin, "config", new AerialFishingConfig() { });
        field(plugin, "store", store);
        field(plugin, "toolbar", mock(ClientToolbar.class));
        field(plugin, "overlayManager", mock(OverlayManager.class));
        field(plugin, "celebration", mock(TenchCelebration.class));
        plugin.startUp();
        CatchAccumulator pending = (CatchAccumulator) field(plugin, "pending");
        pending.catchSignal();
        plugin.shutDown();
        plugin.startUp();
        plugin.onGameTick(new GameTick());
        assertEquals("Already-completed catches must survive a plugin toggle", 1, plugin.stats().catches);
        verify(store).save("critic", plugin.stats());
    }

    @Test
    public void separateGroundTenchItemsFromOneBonusCatchBothCount()
    {
        CatchAccumulator pending = new CatchAccumulator();
        pending.catchSignal();
        pending.tenchMessage();
        pending.ground(0, 1);
        pending.ground(0, 1);
        CatchAccumulator.Result result = pending.finish();
        assertTrue(result.caught);
        assertEquals("Two distinct nonstackable ground items are two tench items", 2, result.tenches);
    }

    @Test
    public void interleavedSeparateInventoryTenchGainsBothCount()
    {
        CatchAccumulator pending = new CatchAccumulator();
        pending.catchSignal();
        pending.inventory(0, 1);
        pending.inventory(0, 1);
        assertEquals("Two positive deltas in one catch tick represent two awarded items", 2, pending.finish().tenches);
    }

    @Test
    public void pickingUpOldPearlDuringCatchDoesNotProduceNewPearl()
    {
        CatchAccumulator pending = new CatchAccumulator();
        pending.catchSignal();
        pending.ground(-1, 0);
        pending.inventory(1, 0);
        assertFalse("A transfer of existing loot does not improve pearl streaks", pending.finish().pearl);
    }

    @Test
    public void droppingOldTenchDuringCatchDoesNotProduceNewTench()
    {
        CatchAccumulator pending = new CatchAccumulator();
        pending.catchSignal();
        pending.inventory(0, -1);
        pending.ground(0, 1);
        assertEquals("Dropping an existing tench does not earn another tench", 0, pending.finish().tenches);
    }

    @Test
    public void immediatePickupOfFreshGroundTenchIsCountedOnce()
    {
        CatchAccumulator pending = new CatchAccumulator();
        pending.catchSignal();
        pending.ground(0, 1);
        pending.ground(0, -1);
        pending.inventory(0, 1);
        assertEquals("Fresh loot changing containers is one reward", 1, pending.finish().tenches);
    }

    @Test
    public void backupPreservesLatestSnapshotWhenPrimaryKeyIsMissing()
    {
        ConfigManager config = mock(ConfigManager.class);
        java.util.Map<String, String> values = new java.util.HashMap<>();
        when(config.getConfiguration(anyString(), anyString(), anyString()))
            .thenAnswer(call -> values.get(call.getArgument(2)));
        doAnswer(call -> { values.put(call.getArgument(2), call.getArgument(3)); return null; })
            .when(config).setConfiguration(anyString(), anyString(), anyString(), anyString());
        StatsStore store = new StatsStore(config, new com.google.gson.Gson());
        FishingStats stats = new FishingStats();
        stats.recordCatch(false, 0);
        store.save("critic", stats);
        stats.recordCatch(false, 0);
        store.save("critic", stats);
        values.remove(StatsStore.KEY);
        assertEquals("Recovery should not silently reduce lifetime catches", 2, store.load("critic").catches);
    }

    @Test
    public void outOfRangeSavedIntegerMustNotWrapIntoAnEmptyLifetime()
    {
        ConfigManager config = mock(ConfigManager.class);
        String json = new com.google.gson.Gson().toJson(new FishingStats())
            .replace("\"catches\":0", "\"catches\":18446744073709551616");
        when(config.getConfiguration(StatsStore.GROUP, "critic", StatsStore.KEY)).thenReturn(json);
        StatsStore store = new StatsStore(config, new com.google.gson.Gson());
        assertThrows(RuntimeException.class, () -> store.load("critic"));
    }

    private static void field(Object target, String name, Object value) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object field(Object target, String name) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
