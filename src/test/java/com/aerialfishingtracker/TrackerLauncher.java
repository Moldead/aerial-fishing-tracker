package com.aerialfishingtracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class TrackerLauncher
{
    private TrackerLauncher() { }

    @SuppressWarnings("unchecked") // RuneLite's loadBuiltin API uses generic varargs.
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(AerialFishingPlugin.class);
        RuneLite.main(args);
    }
}
