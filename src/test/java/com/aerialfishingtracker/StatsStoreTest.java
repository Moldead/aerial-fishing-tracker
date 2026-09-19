package com.aerialfishingtracker;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class StatsStoreTest
{
    private final Map<String, String> data = new HashMap<>();
    private StatsStore store;

    @Before
    public void setup()
    {
        ConfigManager manager = mock(ConfigManager.class);
        when(manager.getConfiguration(anyString(), anyString(), anyString())).thenAnswer(invocation ->
            data.get(invocation.getArgument(1) + ":" + invocation.getArgument(2)));
        doAnswer(invocation ->
        {
            data.put(invocation.getArgument(1) + ":" + invocation.getArgument(2), invocation.getArgument(3));
            return null;
        }).when(manager).setConfiguration(anyString(), anyString(), anyString(), anyString());
        doAnswer(invocation ->
        {
            data.remove(invocation.getArgument(1) + ":" + invocation.getArgument(2));
            return null;
        }).when(manager).unsetConfiguration(anyString(), anyString(), anyString());
        store = new StatsStore(manager, new Gson());
    }

    @Test
    public void roundTripAndCharacterIsolation()
    {
        FishingStats alice = store.load("alice");
        alice.recordCatch(true, 1);
        alice.addHistory(100, 2, 3);
        store.save("alice", alice);
        FishingStats loaded = store.load("alice");
        assertEquals(101, loaded.totalCatches());
        assertEquals(3, loaded.totalPearls());
        assertEquals(4, loaded.totalTenches());
        assertEquals(0, store.load("bob").totalCatches());
    }

    @Test
    public void mirrorsLatestValidSnapshotAndNeverAllowsDecrease()
    {
        FishingStats stats = store.load("alice");
        stats.recordCatch(false, 0);
        store.save("alice", stats);
        stats.recordCatch(true, 0);
        store.save("alice", stats);
        assertTrue(data.get("alice:" + StatsStore.BACKUP).contains("\"catches\":2"));
        assertThrows(IllegalStateException.class, () -> store.save("alice", new FishingStats()));
        assertEquals(2, store.load("alice").catches);
    }

    @Test
    public void malformedOrIncompleteDataNeverSilentlyBecomesZero()
    {
        for (String invalid : new String[]{"garbage", "null", "{}", "{\"schema\":1}", "[]"})
        {
            data.put("alice:" + StatsStore.KEY, invalid);
            assertThrows(RuntimeException.class, () -> store.load("alice"));
            assertEquals(invalid, data.get("alice:" + StatsStore.KEY));
        }
    }

    @Test
    public void missingPrimaryCanUseBackup()
    {
        FishingStats stats = new FishingStats();
        stats.recordCatch(false, 0);
        data.put("alice:" + StatsStore.BACKUP, new Gson().toJson(stats));
        assertEquals(1, store.load("alice").catches);
    }

    @Test
    public void invalidStateAndUnknownProfileAreRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> store.load(null));
        FishingStats stats = new FishingStats();
        stats.schema = 3;
        data.put("alice:" + StatsStore.KEY, new Gson().toJson(stats));
        assertThrows(IllegalArgumentException.class, () -> store.load("alice"));
    }

    @Test
    public void legacyCompletedDryStreakMigratesToIncludeEndingPearlRoll()
    {
        FishingStats legacy = new FishingStats();
        legacy.schema = 1;
        legacy.catches = 175;
        legacy.pearls = 1;
        legacy.seenPearl = true;
        legacy.longestDry = 174;
        legacy.shortestGap = 175;
        data.put("alice:" + StatsStore.KEY, new Gson().toJson(legacy));

        FishingStats migrated = store.load("alice");

        assertEquals(2, migrated.schema);
        assertEquals(175, migrated.longestDry);
        assertEquals(175, migrated.shortestGap);
    }

    @Test
    public void explicitResetDeletesPrimaryAndBackupForOnlyThatCharacter()
    {
        FishingStats alice = new FishingStats();
        alice.recordCatch(false, 0);
        store.save("alice", alice);
        FishingStats bob = new FishingStats();
        bob.recordCatch(false, 0);
        store.save("bob", bob);

        store.reset("alice");

        assertEquals(0, store.load("alice").totalCatches());
        assertEquals(1, store.load("bob").totalCatches());
        assertFalse(data.containsKey("alice:" + StatsStore.KEY));
        assertFalse(data.containsKey("alice:" + StatsStore.BACKUP));
    }
}
