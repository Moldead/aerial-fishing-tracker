package com.aerialfishingtracker;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Independent review regressions: cosmetics cannot corrupt or pause tracking. */
public class CelebrationIndependentReviewTest
{
    @Test
    public void presentationFailureDoesNotPauseCatchTracking() throws Exception
    {
        AerialFishingPluginTest fixture = new AerialFishingPluginTest();
        fixture.setup();
        AerialFishingPlugin plugin = (AerialFishingPlugin) field(fixture, "plugin");
        TenchCelebration celebration = (TenchCelebration) field(fixture, "celebration");
        doThrow(new IllegalStateException("audio executor unavailable"))
            .when(celebration).show(anyInt(), anyLong(), anyBoolean());
        chat(plugin, "Your cormorant returns with its catch.");
        chat(plugin, "The cormorant has brought you a very strange tench.");
        plugin.onGameTick(new GameTick());
        assertNull("Presentation failure must not pause lifetime tracking", plugin.error());
        assertEquals(1, plugin.stats().tenches);
        chat(plugin, "Your cormorant returns with its catch.");
        plugin.onGameTick(new GameTick());
        assertEquals(2, plugin.stats().catches);
    }

    @Test
    public void cancellationWhilePlayerLoadsSuppressesStaleVisualCallback() throws Exception
    {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        List<Runnable> queued = new ArrayList<>();
        doAnswer(invocation -> { queued.add(invocation.getArgument(0)); return null; })
            .when(executor).execute(any(Runnable.class));
        net.runelite.client.audio.AudioPlayer player = mock(net.runelite.client.audio.AudioPlayer.class);
        TenchAudio audio = new TenchAudio(executor, player);
        Runnable started = mock(Runnable.class);
        doAnswer(invocation -> { audio.stop(); return null; })
            .when(player).play(any(Class.class), anyString(), anyFloat());
        audio.play(true, 100, started);
        queued.get(0).run();
        verify(player).play(TenchAudio.class, "/audio/double-tench.wav", 0f);
        verifyNoInteractions(started);
    }

    @Test
    public void cancelledQueuedSoundDoesNotCallRuneLitePlayer()
    {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        List<Runnable> queued = new ArrayList<>();
        doAnswer(invocation -> { queued.add(invocation.getArgument(0)); return null; })
            .when(executor).execute(any(Runnable.class));
        net.runelite.client.audio.AudioPlayer player = mock(net.runelite.client.audio.AudioPlayer.class);
        TenchAudio audio = new TenchAudio(executor, player);
        Runnable started = mock(Runnable.class);
        audio.play(false, 60, started);
        audio.stop();
        queued.get(0).run();
        verifyNoInteractions(player, started);
    }
    @Test
    public void newSettingsDefaultOnUnderGoldenTenchWithoutPreview() throws Exception
    {
        AerialFishingConfig config = new AerialFishingConfig() { };
        assertTrue(config.celebrationBanner());
        assertTrue(config.celebrationConfetti());
        assertTrue(config.celebrationSound());
        assertTrue(config.celebrationMotion());
        assertEquals(60, config.celebrationVolume());
        for (java.lang.reflect.Method method : AerialFishingConfig.class.getDeclaredMethods())
        {
            assertFalse("No player-facing preview trigger", method.getName().startsWith("preview"));
            if (method.getName().startsWith("celebration"))
            {
                assertEquals(AerialFishingConfig.TENCH, method.getAnnotation(ConfigItem.class).section());
            }
        }
    }

    private static Object field(Object target, String name) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void chat(AerialFishingPlugin plugin, String text)
    {
        plugin.onChatMessage(new ChatMessage(null, ChatMessageType.GAMEMESSAGE, "", text, "", 0));
    }
}
