package com.aerialfishingtracker;

import java.lang.reflect.Field;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AerialFishingPluginTest
{
    private AerialFishingPlugin plugin;
    private Client client;
    private ConfigManager config;
    private AerialFishingConfig trackerConfig;
    private StatsStore store;
    private Player player;
    private ItemContainer inventory;
    private ItemContainer equipment;
    private TenchCelebration celebration;

    @Before
    public void setup() throws Exception
    {
        plugin = new AerialFishingPlugin();
        client = mock(Client.class);
        config = mock(ConfigManager.class);
        trackerConfig = mock(AerialFishingConfig.class);
        store = mock(StatsStore.class);
        player = mock(Player.class);
        inventory = mock(ItemContainer.class);
        equipment = mock(ItemContainer.class);
        ClientThread thread = mock(ClientThread.class);
        doAnswer(invocation -> { ((Runnable) invocation.getArgument(0)).run(); return null; })
            .when(thread).invoke(any(Runnable.class));
        inject("client", client);
        inject("clientThread", thread);
        inject("configManager", config);
        inject("config", trackerConfig);
        inject("store", store);
        inject("overlayManager", mock(OverlayManager.class));
        inject("toolbar", mock(ClientToolbar.class));
        celebration = mock(TenchCelebration.class);
        inject("celebration", celebration);
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Alice");
        when(player.getWorldLocation()).thenReturn(new WorldPoint(1368, 3633, 0));
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getSkillExperience(Skill.FISHING)).thenReturn(10000);
        when(client.getSkillExperience(Skill.HUNTER)).thenReturn(10000);
        when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
        when(client.getItemContainer(InventoryID.WORN)).thenReturn(equipment);
        when(equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx()))
            .thenReturn(new Item(ItemID.AERIAL_FISHING_GLOVES_BIRD, 1));
        when(config.getRSProfileKey()).thenReturn("alice");
        when(trackerConfig.sessionTimeoutMinutes()).thenReturn(15);
        when(store.load(anyString())).thenAnswer(invocation -> new FishingStats());
        plugin.startUp();
    }

    @Test
    public void tenchMessageAloneTriggersSingleExactlyOnce()
    {
        chat(ChatMessageType.GAMEMESSAGE, "The cormorant has brought you a very strange tench.");
        tick();
        tick();
        verify(celebration, times(1)).show(1, 1, false);
        verify(celebration, never()).show(eq(2), anyLong(), anyBoolean());
    }

    @Test
    public void doubleOnGroundAndSplitInventoryGroundTriggerDouble()
    {
        assertGroundCelebration(0, 2);
    }

    @Test
    public void splitDoubleTriggersDouble()
    {
        assertGroundCelebration(1, 1);
    }

    @Test
    public void singleOnGroundTriggersSingle()
    {
        assertGroundCelebration(0, 1);
    }

    private void assertGroundCelebration(int inBag, int onGround)
    {
        Tile tile = mock(Tile.class);
        TileItem item = mock(TileItem.class);
        when(tile.getWorldLocation()).thenReturn(new WorldPoint(1368, 3633, 0));
        when(item.getId()).thenReturn(ItemID.GOLDEN_TENCH);
        when(item.getQuantity()).thenReturn(onGround);
        when(item.getOwnership()).thenReturn(TileItem.OWNERSHIP_SELF);
        caught();
        chat(ChatMessageType.GAMEMESSAGE, "The cormorant has brought you a very strange tench.");
        when(inventory.count(ItemID.GOLDEN_TENCH)).thenReturn(inBag);
        plugin.onItemContainerChanged(new ItemContainerChanged(InventoryID.INV, inventory));
        plugin.onItemSpawned(new ItemSpawned(tile, item));
        tick();
        tick();
        assertEquals(inBag + onGround, plugin.stats().tenches);
        verify(celebration, times(1)).show(inBag + onGround, 1, false);
        verify(celebration, times(1)).show(anyInt(), anyLong(), anyBoolean());
    }
    private void inject(String name, Object value) throws Exception
    {
        Field field = AerialFishingPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(plugin, value);
    }

    private void chat(ChatMessageType type, String message)
    {
        plugin.onChatMessage(new ChatMessage(null, type, "", message, "", 0));
    }

    private void caught()
    {
        chat(ChatMessageType.SPAM, "Your cormorant returns with its catch.");
    }

    private void tick() { plugin.onGameTick(new GameTick()); }

    @Test
    public void dispatchAndPlayerChatDoNotCountButFullInventoryCompletionDoes()
    {
        chat(ChatMessageType.SPAM, "You send your cormorant to try to catch a fish from out at sea.");
        chat(ChatMessageType.PUBLICCHAT, "Your cormorant returns with its catch.");
        tick();
        assertEquals(0, plugin.stats().catches);
        caught();
        tick();
        assertEquals(1, plugin.stats().catches);
        verify(store).save("alice", plugin.stats());
    }

    @Test
    public void xpCompletionAndBonusItemsCountOnlyOneRoll()
    {
        plugin.onStatChanged(new StatChanged(Skill.FISHING, 10040, 50, 50));
        caught();
        chat(ChatMessageType.GAMEMESSAGE, "<col=ef1020>The cormorant has brought you a very strange tench.</col>");
        when(inventory.count(ItemID.GOLDEN_TENCH)).thenReturn(2);
        plugin.onItemContainerChanged(new ItemContainerChanged(InventoryID.INV, inventory));
        tick();
        assertEquals(1, plugin.stats().catches);
        assertEquals(2, plugin.stats().tenches);
        verify(celebration).show(2, 1, false);
        tick();
        verify(celebration, times(1)).show(anyInt(), anyLong(), anyBoolean());
    }

    @Test
    public void pearlsDoNotDependOnUntradeableLootChatSetting()
    {
        caught();
        when(inventory.count(ItemID.AERIAL_FISHING_PEARL)).thenReturn(1);
        plugin.onItemContainerChanged(new ItemContainerChanged(InventoryID.INV, inventory));
        tick();
        assertEquals(1, plugin.stats().pearls);
    }

    @Test
    public void untradeableDropChatAndInventoryChangeAreOnePearl()
    {
        caught();
        chat(ChatMessageType.GAMEMESSAGE, "<col=ef1020>Untradeable drop: Molch pearl</col>");
        when(inventory.count(ItemID.AERIAL_FISHING_PEARL)).thenReturn(1);
        plugin.onItemContainerChanged(new ItemContainerChanged(InventoryID.INV, inventory));
        tick();
        assertEquals(1, plugin.stats().catches);
        assertEquals(1, plugin.stats().pearls);
    }

    @Test
    public void inventoryPickupAndTradeDoNotCreateCatches()
    {
        when(inventory.count(ItemID.AERIAL_FISHING_PEARL)).thenReturn(100);
        plugin.onItemContainerChanged(new ItemContainerChanged(InventoryID.INV, inventory));
        tick();
        assertEquals(0, plugin.stats().catches);
        assertEquals(0, plugin.stats().pearls);
    }

    @Test
    public void ownGroundPearlCountsButOtherPlayersPearlDoesNot()
    {
        Tile tile = mock(Tile.class);
        TileItem item = mock(TileItem.class);
        when(tile.getWorldLocation()).thenReturn(new WorldPoint(1368, 3633, 0));
        when(item.getId()).thenReturn(ItemID.AERIAL_FISHING_PEARL);
        when(item.getQuantity()).thenReturn(1);
        when(item.getOwnership()).thenReturn(TileItem.OWNERSHIP_OTHER);
        caught();
        plugin.onItemSpawned(new ItemSpawned(tile, item));
        tick();
        assertEquals(0, plugin.stats().pearls);
        when(item.getOwnership()).thenReturn(TileItem.OWNERSHIP_SELF);
        caught();
        plugin.onItemSpawned(new ItemSpawned(tile, item));
        tick();
        assertEquals(1, plugin.stats().pearls);
    }

    @Test
    public void hopAndTeleportPreserveSessionAndInventoryIsRebaselined()
    {
        caught();
        tick();
        when(client.getGameState()).thenReturn(GameState.HOPPING);
        GameStateChanged hopping = new GameStateChanged();
        hopping.setGameState(GameState.HOPPING);
        plugin.onGameStateChanged(hopping);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(inventory.count(ItemID.AERIAL_FISHING_PEARL)).thenReturn(500);
        tick();
        assertEquals(1, plugin.session().catches);
        assertEquals(0, plugin.stats().pearls);
        when(player.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
        when(equipment.getItem(anyInt())).thenReturn(null);
        tick();
        assertFalse(plugin.isFishingLocation());
        assertEquals(1, plugin.session().catches);
        when(player.getWorldLocation()).thenReturn(new WorldPoint(1368, 3633, 0));
        assertTrue(plugin.isFishingLocation());
    }

    @Test
    public void overlayOnlyShowsOnMolchIslandEvenWithCormorantEquipped()
    {
        assertTrue(plugin.shouldDisplayOverlay());
        when(player.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
        assertFalse(plugin.shouldDisplayOverlay());
    }

    @Test
    public void characterSwitchIsIsolatedAndStaleHistoryActionIsRejected()
    {
        caught();
        tick();
        when(config.getRSProfileKey()).thenReturn("bob");
        plugin.onRuneScapeProfileChanged(new RuneScapeProfileChanged("alice", "bob"));
        assertEquals(0, plugin.session().catches);
        plugin.addHistory("alice", 123, 0, 0);
        assertEquals(0, plugin.stats().totalCatches());
        when(config.getRSProfileKey()).thenReturn("alice");
        plugin.onRuneScapeProfileChanged(new RuneScapeProfileChanged("bob", "alice"));
        assertEquals(1, plugin.session().catches);
    }

    @Test
    public void manualResetAndHistoryNeverChangeLifetimeTrackedData()
    {
        caught();
        tick();
        plugin.addHistory("alice", 10000, 40, 2);
        plugin.resetSession("alice");
        assertEquals(10001, plugin.stats().totalCatches());
        assertEquals(1, plugin.stats().catches);
        assertEquals(0, plugin.stats().pearls);
        assertEquals(1, plugin.stats().sincePearl);
        assertEquals(0, plugin.session().catches);
    }

    @Test
    public void togglePreservesSession()
    {
        caught();
        tick();
        plugin.shutDown();
        plugin.startUp();
        assertEquals(1, plugin.session().catches);
        assertEquals(1, plugin.stats().catches);
    }

    @Test
    public void explicitFullResetClearsTrackedAndHistoricalCounts()
    {
        caught();
        tick();
        plugin.addHistory("alice", 100, 2, 1);
        plugin.resetConfiguration();
        assertEquals(0, plugin.stats().totalCatches());
        assertEquals(0, plugin.stats().totalPearls());
        assertEquals(0, plugin.stats().totalTenches());
        assertEquals(0, plugin.session().catches);
        verify(store).reset("alice");
    }

    @Test
    public void fullResetAlsoWorksWhenPluginDisabled()
    {
        caught();
        tick();
        plugin.shutDown();
        plugin.resetConfiguration();
        assertEquals(0, plugin.stats().totalCatches());
        assertEquals(0, plugin.session().catches);
        verify(store).reset("alice");
    }

    @Test
    public void xpCapUsesCompletionAndNoGloveXpIsIgnored()
    {
        plugin.onStatChanged(new StatChanged(Skill.FISHING, 200000000, 99, 99));
        tick();
        assertEquals(0, plugin.stats().catches);
        caught();
        plugin.onStatChanged(new StatChanged(Skill.FISHING, 200000000, 99, 99));
        tick();
        assertEquals(1, plugin.stats().catches);
        when(equipment.getItem(anyInt())).thenReturn(null);
        plugin.onStatChanged(new StatChanged(Skill.FISHING, 200000001, 99, 99));
        tick();
        assertEquals(1, plugin.stats().catches);
    }

    @Test
    public void xpOnlyCatchRequiresPairedSkillsAndDoesNotDependOnInventory()
    {
        plugin.onStatChanged(new StatChanged(Skill.FISHING, 10040, 50, 50));
        tick();
        assertEquals(0, plugin.stats().catches);
        plugin.onStatChanged(new StatChanged(Skill.HUNTER, 10060, 50, 50));
        plugin.onStatChanged(new StatChanged(Skill.FISHING, 10080, 50, 50));
        tick();
        assertEquals(1, plugin.stats().catches);
        assertEquals(1, plugin.session().catches);
    }

    @Test
    public void historyAndUnrelatedInventoryNeverCelebrateButIntervalSurvivesSessionReset()
    {
        plugin.addHistory("alice", 16000, 200, 1);
        when(inventory.count(ItemID.GOLDEN_TENCH)).thenReturn(1);
        plugin.onItemContainerChanged(new ItemContainerChanged(InventoryID.INV, inventory));
        tick();
        verify(celebration, never()).show(anyInt(), anyLong(), anyBoolean());
        caught();
        chat(ChatMessageType.GAMEMESSAGE, "The cormorant has brought you a very strange tench.");
        tick();
        verify(celebration).show(1, 1, false);
        caught();
        tick();
        plugin.resetSession("alice");
        caught();
        chat(ChatMessageType.GAMEMESSAGE, "The cormorant has brought you a very strange tench.");
        tick();
        verify(celebration).show(1, 2, true);
    }

    @Test
    public void pendingCatchIsSavedToOldCharacterBeforeProfileSwitch()
    {
        FishingStats alice = plugin.stats();
        caught();
        when(config.getRSProfileKey()).thenReturn("bob");
        plugin.onRuneScapeProfileChanged(new RuneScapeProfileChanged("alice", "bob"));
        assertEquals(1, alice.catches);
        verify(store).save("alice", alice);
        assertEquals(0, plugin.stats().catches);
        assertEquals(0, plugin.session().catches);
    }
}
