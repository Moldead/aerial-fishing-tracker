package com.aerialfishingtracker;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;
import org.mockito.MockedStatic;
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
    public void cancellationDuringDeviceOpenCannotStartAudio() throws Exception
    {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        List<Runnable> queued = new ArrayList<>();
        doAnswer(invocation -> { queued.add(invocation.getArgument(0)); return null; })
            .when(executor).execute(any(Runnable.class));
        TenchAudio audio = new TenchAudio(executor);
        Runnable started = mock(Runnable.class);
        Clip clip = mock(Clip.class);
        doAnswer(invocation -> { audio.stop(); return null; }).when(clip).open(any(AudioInputStream.class));
        try (MockedStatic<AudioSystem> system = mockStatic(AudioSystem.class, CALLS_REAL_METHODS))
        {
            system.when(AudioSystem::getClip).thenReturn(clip);
            audio.play(true, 100, started);
            queued.get(0).run();
        }
        verify(clip, never()).start();
        verify(clip, atLeastOnce()).close();
        verifyNoInteractions(started);
    }

    @Test
    public void cancelledQueuedSoundDoesNotOpenDevice() throws Exception
    {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        List<Runnable> queued = new ArrayList<>();
        doAnswer(invocation -> { queued.add(invocation.getArgument(0)); return null; })
            .when(executor).execute(any(Runnable.class));
        TenchAudio audio = new TenchAudio(executor);
        Runnable started = mock(Runnable.class);
        try (MockedStatic<AudioSystem> system = mockStatic(AudioSystem.class))
        {
            audio.play(false, 60, started);
            audio.stop();
            queued.get(0).run();
            system.verifyNoInteractions();
        }
        verifyNoInteractions(started);
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
