package com.aerialfishingtracker;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
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
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemQuantityChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(name = "Aerial Fishing Tracker", description = "Track aerial catches, pearl streaks and cumulative tench odds", tags = {"aerial", "fishing", "molch", "pearl", "tench"})
public class AerialFishingPlugin extends Plugin
{
    private static final Logger LOG = LoggerFactory.getLogger(AerialFishingPlugin.class);

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ConfigManager configManager;
    @Inject private AerialFishingConfig config;
    @Inject private StatsStore store;
    @Inject private OverlayManager overlayManager;
    @Inject private AerialFishingOverlay overlay;
    @Inject private ClientToolbar toolbar;

    // Retain sessions across logout, profile switches and plugin toggles within this client process.
    private final Map<String, FishingSession> sessions = new HashMap<>();
    private final CatchAccumulator pending = new CatchAccumulator();
    private String profile;
    private FishingStats stats;
    private FishingSession session;
    private int fishingXp = -1;
    private int hunterXp = -1;
    private int inventoryPearls = -1;
    private int inventoryTenches = -1;
    private boolean ready;
    private volatile boolean running;
    private volatile long lifecycle;
    private String error;
    private TrackerPanel panel;
    private NavigationButton navigation;

    @Provides
    AerialFishingConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(AerialFishingConfig.class);
    }

    @Override
    protected void startUp()
    {
        running = true;
        long generation = ++lifecycle;
        panel = new TrackerPanel(this);
        navigation = NavigationButton.builder().tooltip("Aerial Fishing Tracker").icon(icon()).priority(7).panel(panel).build();
        toolbar.addNavigation(navigation);
        overlayManager.add(overlay);
        clientThread.invoke(() ->
        {
            if (running && generation == lifecycle)
            {
                selectProfile();
                baseline();
                refreshPanel();
            }
        });
    }

    @Override
    protected void shutDown()
    {
        running = false;
        long generation = ++lifecycle;
        overlayManager.remove(overlay);
        toolbar.removeNavigation(navigation);
        clientThread.invoke(() ->
        {
            // Lifecycle methods run on the Swing EDT. Only the client thread may touch tracking
            // state; preserve a catch received after the last GameTick before disabling.
            completeCatch();
            if (generation == lifecycle)
            {
                ready = false;
            }
        });
    }

    @Subscribe
    public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
    {
        clientThread.invoke(() ->
        {
            if (running)
            {
                selectProfile();
                baseline();
                refreshPanel();
            }
        });
    }

    private void selectProfile()
    {
        String next = configManager.getRSProfileKey();
        if (next == null || next.equals(profile))
        {
            return;
        }
        completeCatch();
        ready = false;
        profile = next;
        error = null;
        stats = null;
        session = sessions.computeIfAbsent(profile, key -> new FishingSession());
        session.expire(System.nanoTime(), sessionTimeoutNanos());
        try
        {
            stats = store.load(profile);
        }
        catch (RuntimeException ex)
        {
            fail(ex);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            // Flush a completed tick before a hop/logout; never interpret login inventory as loot.
            completeCatch();
            pending.clear();
            ready = false;
            fishingXp = -1;
            hunterXp = -1;
            inventoryPearls = -1;
            inventoryTenches = -1;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!running)
        {
            return;
        }
        selectProfile();
        if (!ready)
        {
            baseline();
        }
        else
        {
            completeCatch();
        }
        if (session != null)
        {
            session.expire(System.nanoTime(), sessionTimeoutNanos());
        }
        refreshPanel();
    }

    private void baseline()
    {
        pending.clear();
        ready = stats != null && client.getGameState() == GameState.LOGGED_IN
            && profile != null && profile.equals(configManager.getRSProfileKey());
        fishingXp = ready ? client.getSkillExperience(Skill.FISHING) : -1;
        hunterXp = ready ? client.getSkillExperience(Skill.HUNTER) : -1;
        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        inventoryPearls = inventory == null ? -1 : inventory.count(ItemID.AERIAL_FISHING_PEARL);
        inventoryTenches = inventory == null ? -1 : inventory.count(ItemID.GOLDEN_TENCH);
    }

    private boolean canTrack()
    {
        return running && ready && error == null && client.getGameState() == GameState.LOGGED_IN
            && profile != null && profile.equals(configManager.getRSProfileKey()) && isFishingLocation();
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (!canTrack() || (event.getType() != ChatMessageType.SPAM && event.getType() != ChatMessageType.GAMEMESSAGE))
        {
            return;
        }
        String message = Text.removeTags(event.getMessage());
        if ("Your cormorant returns with its catch.".equals(message))
        {
            pending.catchSignal();
        }
        else if ("The cormorant has brought you a very strange tench.".equals(message))
        {
            pending.tenchMessage();
        }
        else if ("Untradeable drop: Molch pearl".equals(message))
        {
            pending.pearlMessage();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (event.getSkill() != Skill.FISHING && event.getSkill() != Skill.HUNTER)
        {
            return;
        }
        boolean fishing = event.getSkill() == Skill.FISHING;
        int previous = fishing ? fishingXp : hunterXp;
        if (fishing) { fishingXp = event.getXp(); }
        else { hunterXp = event.getXp(); }
        // Require both fishing and hunter awards, avoiding a false catch from a one-skill XP
        // reward. If one skill is capped, use the other; if both are capped, use return chat.
        if (canTrack() && hasCormorant() && previous >= 0 && event.getXp() > previous && event.getXp() - previous <= 1000)
        {
            if (fishing)
            {
                pending.fishingXp();
                if (hunterXp == 200_000_000) { pending.hunterXp(); }
            }
            else
            {
                pending.hunterXp();
                if (fishingXp == 200_000_000) { pending.fishingXp(); }
            }
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() != InventoryID.INV)
        {
            return;
        }
        int pearls = event.getItemContainer().count(ItemID.AERIAL_FISHING_PEARL);
        int tenches = event.getItemContainer().count(ItemID.GOLDEN_TENCH);
        if (canTrack() && inventoryPearls >= 0 && inventoryTenches >= 0)
        {
            pending.inventory(pearls - inventoryPearls, tenches - inventoryTenches);
        }
        inventoryPearls = pearls;
        inventoryTenches = tenches;
    }

    @Subscribe
    public void onItemSpawned(ItemSpawned event)
    {
        groundReward(event.getItem(), event.getTile(), event.getItem().getQuantity());
    }

    @Subscribe
    public void onItemQuantityChanged(ItemQuantityChanged event)
    {
        groundReward(event.getItem(), event.getTile(), event.getNewQuantity() - event.getOldQuantity());
    }

    @Subscribe
    public void onItemDespawned(ItemDespawned event)
    {
        // An early removal can be an inventory pickup; natural expiry is not a transfer.
        if (event.getItem().getDespawnTime() > client.getTickCount())
        {
            groundReward(event.getItem(), event.getTile(), -event.getItem().getQuantity());
        }
    }

    private void groundReward(TileItem item, Tile tile, int quantity)
    {
        Player player = client.getLocalPlayer();
        if (canTrack() && player != null && item.getOwnership() == TileItem.OWNERSHIP_SELF
            && player.getWorldLocation().equals(tile.getWorldLocation()))
        {
            pending.ground(item.getId() == ItemID.AERIAL_FISHING_PEARL ? quantity : 0,
                item.getId() == ItemID.GOLDEN_TENCH ? quantity : 0);
        }
    }

    private void completeCatch()
    {
        CatchAccumulator.Result result = pending.finish();
        if (!result.caught || stats == null || error != null || profile == null)
        {
            return;
        }
        try
        {
            stats.recordCatch(result.pearl, result.tenches);
            session.recordCatch(System.nanoTime(), result.pearl, sessionTimeoutNanos());
            store.save(profile, stats);
        }
        catch (RuntimeException ex)
        {
            fail(ex);
        }
    }

    boolean isFishingLocation()
    {
        return isOnMolchIsland() || hasCormorant();
    }

    boolean shouldDisplayOverlay()
    {
        return isOnMolchIsland();
    }

    @Override
    public void resetConfiguration()
    {
        clientThread.invoke(() ->
        {
            String target = profile != null ? profile : configManager.getRSProfileKey();
            if (target == null) { return; }
            pending.clear();
            store.reset(target);
            if (target.equals(profile))
            {
                stats = new FishingStats();
                if (session != null) { session.reset(); }
                error = null;
            }
            overlayManager.resetOverlay(overlay);
            refreshPanel();
        });
    }

    private boolean isOnMolchIsland()
    {
        Player player = client.getLocalPlayer();
        if (player == null || client.getGameState() != GameState.LOGGED_IN)
        {
            return false;
        }
        WorldPoint point = player.getWorldLocation();
        // RuneLite's own Discord area map identifies region 5432 as Molch Island.
        return point.getPlane() == 0 && point.getRegionID() == 5432;
    }

    private boolean hasCormorant()
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
        Item weapon = equipment == null ? null : equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
        return weapon != null && (weapon.getId() == ItemID.AERIAL_FISHING_GLOVES_BIRD
            || weapon.getId() == ItemID.AERIAL_FISHING_GLOVES_NO_BIRD);
    }

    void resetSession(String expectedProfile)
    {
        clientThread.invoke(() ->
        {
            if (running && expectedProfile != null && expectedProfile.equals(profile) && canEdit())
            {
                completeCatch();
                session.reset();
                refreshPanel();
            }
        });
    }

    void addHistory(String expectedProfile, long catches, long pearls, long tenches)
    {
        clientThread.invoke(() ->
        {
            if (!running || expectedProfile == null || !expectedProfile.equals(profile) || !canEdit())
            {
                panel.showMessage("Character changed or is logged out. Nothing was added.");
                return;
            }
            try
            {
                stats.addHistory(catches, pearls, tenches);
            }
            catch (IllegalArgumentException | ArithmeticException ex)
            {
                panel.showMessage("Invalid amount or total too large. Nothing was added.");
                return;
            }
            try
            {
                store.save(profile, stats);
                panel.showMessage("Historical counts added to this character.");
                refreshPanel();
            }
            catch (RuntimeException ex)
            {
                fail(ex);
            }
        });
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!AerialFishingConfig.GROUP.equals(event.getGroup()) || !"applyHistorical".equals(event.getKey())
            || !Boolean.parseBoolean(event.getNewValue()) || panel == null)
        {
            return;
        }

        // Reset the switch immediately so an accidental revisit cannot apply the same additions twice.
        configManager.setConfiguration(AerialFishingConfig.GROUP, "applyHistorical", false);
        panel.confirmHistoryFromSettings(config.historyFish(), config.historyPearls(), config.historyTenches());
    }

    void clearHistorySettings()
    {
        configManager.setConfiguration(AerialFishingConfig.GROUP, "historyFish", "");
        configManager.setConfiguration(AerialFishingConfig.GROUP, "historyPearls", "");
        configManager.setConfiguration(AerialFishingConfig.GROUP, "historyTenches", "");
    }

    private long sessionTimeoutNanos()
    {
        return FishingSession.timeoutNanos(config.sessionTimeoutMinutes());
    }

    private boolean canEdit()
    {
        return stats != null && error == null && client.getGameState() == GameState.LOGGED_IN
            && profile != null && profile.equals(configManager.getRSProfileKey());
    }

    private void fail(RuntimeException ex)
    {
        error = "Tracking paused: saved data needs attention. See client log; lifetime data was not reset.";
        LOG.error("Aerial Fishing Tracker stopped to protect lifetime data", ex);
        refreshPanel();
    }

    private void refreshPanel()
    {
        if (panel != null)
        {
            Player player = client.getLocalPlayer();
            String name = canEdit() && player != null ? player.getName() : "Log in to edit this character";
            panel.update(profile, name, stats == null ? 0 : stats.totalCatches(), canEdit(), error);
        }
    }

    FishingStats stats() { return stats; }
    FishingSession session() { return session; }
    String error() { return error; }
    String profile() { return profile; }

    private static BufferedImage icon()
    {
        BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(158, 226, 177));
        graphics.fillOval(3, 7, 15, 10);
        graphics.fillPolygon(new int[]{16, 23, 23}, new int[]{12, 5, 19}, 3);
        graphics.setColor(new Color(25, 30, 36));
        graphics.fillOval(6, 10, 3, 3);
        graphics.dispose();
        return image;
    }
}
