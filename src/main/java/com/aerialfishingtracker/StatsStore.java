package com.aerialfishingtracker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.math.BigInteger;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;

final class StatsStore
{
    private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
    // Separate from visible preferences: only the explicit, confirmed full reset clears statistics.
    static final String GROUP = "aerialfishingtrackerdata";
    static final String KEY = "lifetimeV1";
    static final String BACKUP = "lifetimeBackupV1";
    private final ConfigManager configManager;
    private final Gson gson;

    @Inject
    StatsStore(ConfigManager configManager, Gson gson)
    {
        this.configManager = configManager;
        this.gson = gson;
    }

    FishingStats load(String profile)
    {
        requireProfile(profile);
        String json = configManager.getConfiguration(GROUP, profile, KEY);
        if (json == null)
        {
            json = configManager.getConfiguration(GROUP, profile, BACKUP);
        }
        return json == null ? new FishingStats() : decode(json);
    }

    void save(String profile, FishingStats stats)
    {
        requireProfile(profile);
        stats.validate();
        String previous = configManager.getConfiguration(GROUP, profile, KEY);
        if (previous == null)
        {
            previous = configManager.getConfiguration(GROUP, profile, BACKUP);
        }
        if (previous != null)
        {
            FishingStats prior = decode(previous);
            if (stats.catches < prior.catches || stats.pearls < prior.pearls || stats.tenches < prior.tenches
                || stats.historicalCatches < prior.historicalCatches || stats.historicalPearls < prior.historicalPearls
                || stats.historicalTenches < prior.historicalTenches)
            {
                throw new IllegalStateException("Refusing to reduce saved lifetime totals.");
            }
        }
        String json = gson.toJson(stats);
        configManager.setConfiguration(GROUP, profile, KEY, json);
        configManager.setConfiguration(GROUP, profile, BACKUP, json);
    }

    void reset(String profile)
    {
        requireProfile(profile);
        configManager.unsetConfiguration(GROUP, profile, KEY);
        configManager.unsetConfiguration(GROUP, profile, BACKUP);
    }

    private FishingStats decode(String json)
    {
        JsonObject object = gson.fromJson(json, JsonObject.class);
        String[] required = {"schema", "catches", "pearls", "tenches", "historicalCatches", "historicalPearls",
            "historicalTenches", "sincePearl", "longestDry", "shortestGap", "seenPearl"};
        for (String key : required)
        {
            if (object == null || !object.has(key) || object.get(key).isJsonNull())
            {
                throw new IllegalArgumentException("Incomplete saved statistics; refusing to replace them.");
            }
            if (!object.get(key).isJsonPrimitive()
                || ("seenPearl".equals(key) ? !object.getAsJsonPrimitive(key).isBoolean()
                : !object.getAsJsonPrimitive(key).isNumber() || !object.get(key).getAsString().matches("[0-9]+")))
            {
                throw new IllegalArgumentException("Invalid saved statistic type; refusing to replace it.");
            }
            if (!"seenPearl".equals(key)
                && new BigInteger(object.get(key).getAsString()).compareTo(MAX_LONG) > 0)
            {
                throw new IllegalArgumentException("Saved statistic is too large; refusing to replace it.");
            }
        }
        FishingStats result = gson.fromJson(object, FishingStats.class);
        result.migrate();
        result.validate();
        return result;
    }

    private static void requireProfile(String profile)
    {
        if (profile == null || profile.isEmpty())
        {
            throw new IllegalArgumentException("A character profile is required.");
        }
    }
}
