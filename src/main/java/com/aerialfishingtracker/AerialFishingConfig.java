package com.aerialfishingtracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(AerialFishingConfig.GROUP)
public interface AerialFishingConfig extends Config
{
    String GROUP = "aerialfishingtracker";

    @ConfigSection(name = "Catch rolls", description = "Successful cormorant returns; doubled fish do not add another rare-drop roll", position = 0)
    String CATCHES = "catches";

    @ConfigSection(name = "Molch pearls", description = "Only pearls earned by fishing", position = 1)
    String PEARLS = "pearls";

    @ConfigSection(name = "Pearl streaks", description = "Lifetime tracked streaks for this character", position = 2)
    String STREAKS = "streaks";

    @ConfigSection(name = "Golden tench", description = "Chance of at least one tench over the selected catches", position = 3)
    String TENCH = "tench";

    @ConfigSection(name = "Appearance", description = "Overlay appearance and session timeout", position = 4)
    String DISPLAY = "display";

    @ConfigSection(name = "Historical additions", description = "Add pre-tracker totals to the logged-in character", position = 5)
    String HISTORY = "history";

    @ConfigItem(keyName = "lifetimeCatches", name = "Lifetime fish", description = "Tracked successful returns plus historical fish; cleared only by explicit full reset", section = CATCHES, position = 0)
    default boolean lifetimeCatches() { return true; }

    @ConfigItem(keyName = "sessionCatches", name = "Sessions fish", description = "Successful returns this session; resets on client restart, manual reset, or after the configured idle time", section = CATCHES, position = 1)
    default boolean sessionCatches() { return true; }

    @ConfigItem(keyName = "sincePearl", name = "Rolls since pearl", description = "Successful returns since the last caught pearl; before the first pearl, counts from tracking start", section = CATCHES, position = 2)
    default boolean sincePearl() { return true; }

    @ConfigItem(keyName = "fishPerHour", name = "Fish per hour", description = "Session fish divided by elapsed time since the first catch, including idle time", section = CATCHES, position = 3)
    default boolean fishPerHour() { return true; }

    @ConfigItem(keyName = "sessionPearls", name = "Session pearls", description = "Pearls caught this session; excludes trades and pickups", section = PEARLS, position = 0)
    default boolean sessionPearls() { return true; }

    @ConfigItem(keyName = "lifetimePearls", name = "Lifetime pearls", description = "Tracked pearls plus historical pearls", section = PEARLS, position = 1)
    default boolean lifetimePearls() { return true; }

    @ConfigItem(keyName = "observedRate", name = "Actual pearl rate", description = "Lifetime fish divided by lifetime pearls, including historical additions", section = PEARLS, position = 2)
    default boolean observedRate() { return true; }

    @ConfigItem(keyName = "expectedRate", name = "Wiki pearl rate", description = "Estimated wiki rate using visible levels and the August 2026 approximately 50% increase", section = PEARLS, position = 3)
    default boolean expectedRate() { return true; }

    @ConfigItem(keyName = "pearlsPerHour", name = "Pearls per hour", description = "Session pearls divided by elapsed time since the first catch, including idle time", section = PEARLS, position = 4)
    default boolean pearlsPerHour() { return true; }

    @ConfigItem(keyName = "longestDry", name = "Longest dry", description = "Longest tracked pearl interval, including the ending pearl catch", section = STREAKS, position = 0)
    default boolean longestDry() { return true; }

    @ConfigItem(keyName = "shortestGap", name = "Most spooned", description = "Fewest fish between two caught pearls; unknown until two pearls", section = STREAKS, position = 1)
    default boolean shortestGap() { return true; }

    @ConfigItem(keyName = "tenches", name = "Golden tenches caught", description = "Lifetime tench items caught plus historical tenches; includes bonus duplicates", section = TENCH, position = 0)
    default boolean tenches() { return true; }

    @ConfigItem(keyName = "tenchChance", name = "Cumulative tench chance", description = "1 - (19999/20000)^catches. Each next catch remains 1/20,000", section = TENCH, position = 1)
    default boolean tenchChance() { return true; }

    @ConfigItem(keyName = "tenchScope", name = "Chance uses", description = "Lifetime includes historical catches; session excludes them. Catching a tench does not reset cumulative odds", section = TENCH, position = 2)
    default TenchScope tenchScope() { return TenchScope.LIFETIME; }

    @Range(min = 11, max = 22)
    @ConfigItem(keyName = "fontSize", name = "Font size", description = "RuneLite Plain 12 (p12_full) overlay font", section = DISPLAY, position = 0)
    default int fontSize() { return 15; }

    @Range(min = 0, max = 255)
    @ConfigItem(keyName = "backgroundOpacity", name = "Background opacity", description = "0 is transparent; 255 is opaque", section = DISPLAY, position = 1)
    default int backgroundOpacity() { return 160; }

    @Range(min = 1, max = 1440)
    @ConfigItem(keyName = "sessionTimeoutMinutes", name = "Session reset after (minutes)", description = "Start a new session after this many minutes without a successful aerial-fishing catch", section = DISPLAY, position = 2)
    default int sessionTimeoutMinutes() { return 15; }


    @ConfigItem(keyName = "historyFish", name = "Historical fish to add", description = "Fish caught before this tracker. Enter an amount, then enable Apply historical additions.", section = HISTORY, position = 0)
    default String historyFish() { return ""; }

    @ConfigItem(keyName = "historyPearls", name = "Historical pearls to add", description = "Optional pearls caught before this tracker.", section = HISTORY, position = 1)
    default String historyPearls() { return ""; }

    @ConfigItem(keyName = "historyTenches", name = "Historical tenches to add", description = "Optional Golden Tenches caught before this tracker.", section = HISTORY, position = 2)
    default String historyTenches() { return ""; }

    @ConfigItem(keyName = "applyHistorical", name = "Apply historical additions", description = "Enable after entering the amounts above. You will be asked to confirm the character and totals.", section = HISTORY, position = 3)
    default boolean applyHistorical() { return false; }

}
