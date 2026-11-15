package com.aerialfishingtracker;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Queue;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Independent event-level and deferred-lifecycle checks, not live server observations. */
public class IndependentEventTest
{
    private final Queue<Runnable> callbacks = new ArrayDeque<>();
    private final AerialFishingPlugin plugin = new AerialFishingPlugin();
    private final Client client = mock(Client.class);
    private final ConfigManager config = mock(ConfigManager.class);
    private final StatsStore store = mock(StatsStore.class);
    private final ItemContainer inventory = mock(ItemContainer.class);
    private final WorldPoint position = new WorldPoint(1368, 3633, 0);

    @Before
    public void initialize() throws Exception
    {
        ClientThread thread = mock(ClientThread.class);
        doAnswer(call -> { callbacks.add(call.getArgument(0)); return null; })
            .when(thread).invoke(any(Runnable.class));
        Player player = mock(Player.class);
        when(player.getWorldLocation()).thenReturn(position);
        when(client.getLocalPlayer()).thenReturn(player);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getTickCount()).thenReturn(100);
        when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
        when(config.getRSProfileKey()).thenReturn("critic");
        when(store.load("critic")).thenReturn(new FishingStats());
        inject("client", client);
        inject("clientThread", thread);
        inject("configManager", config);
        inject("config", new AerialFishingConfig() { });
        inject("store", store);
        inject("toolbar", mock(ClientToolbar.class));
        inject("overlayManager", mock(OverlayManager.class));
        inject("celebration", mock(TenchCelebration.class));
        plugin.startUp();
        drainCallbacks();
    }

    @Test
    public void naturalPearlExpiryDoesNotCancelFreshInventoryReward()
    {
        caught();
        when(inventory.count(ItemID.AERIAL_FISHING_PEARL)).thenReturn(1);
        plugin.onItemContainerChanged(new ItemContainerChanged(InventoryID.INV, inventory));
        plugin.onItemDespawned(new ItemDespawned(tile(), pearl(100)));
        plugin.onGameTick(new GameTick());
        assertEquals(1, plugin.stats().pearls);
    }

    @Test
    public void earlyPearlDespawnAndInventoryGainAreOnlyAPickup()
    {
        caught();
        when(inventory.count(ItemID.AERIAL_FISHING_PEARL)).thenReturn(1);
        plugin.onItemContainerChanged(new ItemContainerChanged(InventoryID.INV, inventory));
        plugin.onItemDespawned(new ItemDespawned(tile(), pearl(200)));
        plugin.onGameTick(new GameTick());
        assertEquals(1, plugin.stats().catches);
        assertEquals(0, plugin.stats().pearls);
    }

    @Test
    public void freshGroundPearlThenImmediatePickupCountsOnce()
    {
        caught();
        Tile tile = tile();
        TileItem pearl = pearl(200);
        plugin.onItemSpawned(new ItemSpawned(tile, pearl));
        plugin.onItemDespawned(new ItemDespawned(tile, pearl));
        when(inventory.count(ItemID.AERIAL_FISHING_PEARL)).thenReturn(1);
        plugin.onItemContainerChanged(new ItemContainerChanged(InventoryID.INV, inventory));
        plugin.onGameTick(new GameTick());
        assertEquals(1, plugin.stats().pearls);
    }

    @Test
    public void rapidDisableEnableKeepsQueuedCatchExactlyOnce()
    {
        caught();
        plugin.shutDown();
        plugin.startUp();
        assertEquals(0, plugin.stats().catches);
        drainCallbacks();
        assertEquals(1, plugin.stats().catches);
        caught();
        plugin.onGameTick(new GameTick());
        assertEquals(2, plugin.stats().catches);
        assertEquals(2, plugin.session().catches);
        verify(store, times(2)).save("critic", plugin.stats());
    }

    @Test
    public void staleStartupDoesNotUndoFinalShutdown() throws Exception
    {
        plugin.shutDown();
        plugin.startUp();
        plugin.shutDown();
        drainCallbacks();
        Field ready = AerialFishingPlugin.class.getDeclaredField("ready");
        ready.setAccessible(true);
        assertFalse(ready.getBoolean(plugin));
        caught();
        plugin.onGameTick(new GameTick());
        assertEquals(0, plugin.stats().catches);
    }

    private void caught()
    {
        plugin.onChatMessage(new ChatMessage(null, ChatMessageType.SPAM, "",
            "Your cormorant returns with its catch.", "", 0));
    }

    private Tile tile()
    {
        Tile tile = mock(Tile.class);
        when(tile.getWorldLocation()).thenReturn(position);
        return tile;
    }

    private TileItem pearl(int despawnTick)
    {
        TileItem pearl = mock(TileItem.class);
        when(pearl.getId()).thenReturn(ItemID.AERIAL_FISHING_PEARL);
        when(pearl.getQuantity()).thenReturn(1);
        when(pearl.getOwnership()).thenReturn(TileItem.OWNERSHIP_SELF);
        when(pearl.getDespawnTime()).thenReturn(despawnTick);
        return pearl;
    }

    private void drainCallbacks()
    {
        while (!callbacks.isEmpty()) { callbacks.remove().run(); }
    }

    private void inject(String name, Object value) throws Exception
    {
        Field field = AerialFishingPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(plugin, value);
    }
}
