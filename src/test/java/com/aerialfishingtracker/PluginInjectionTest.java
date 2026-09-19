package com.aerialfishingtracker;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PluginInjectionTest
{
    @Test
    public void runeLiteStyleChildInjectorBuildsPluginAndRealOverlay()
    {
        ConfigManager config = mock(ConfigManager.class);
        when(config.getConfig(AerialFishingConfig.class)).thenReturn(new AerialFishingConfig() { });
        Injector parent = Guice.createInjector(binder ->
        {
            binder.bind(Client.class).toInstance(mock(Client.class));
            binder.bind(ClientThread.class).toInstance(mock(ClientThread.class));
            binder.bind(ConfigManager.class).toInstance(config);
            binder.bind(ClientToolbar.class).toInstance(mock(ClientToolbar.class));
            binder.bind(OverlayManager.class).toInstance(mock(OverlayManager.class));
            binder.bind(Gson.class).toInstance(new Gson());
        });
        AerialFishingPlugin plugin = new AerialFishingPlugin();
        Injector child = parent.createChildInjector(binder ->
        {
            binder.bind(AerialFishingPlugin.class).toInstance(plugin);
            binder.install(plugin);
        });
        assertSame(plugin, child.getInstance(AerialFishingPlugin.class));
        assertNotNull(child.getInstance(AerialFishingOverlay.class));
        assertNotNull(child.getInstance(StatsStore.class));
    }
}
