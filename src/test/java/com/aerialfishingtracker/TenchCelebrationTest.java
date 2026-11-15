package com.aerialfishingtracker;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.util.concurrent.ScheduledExecutorService;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

public class TenchCelebrationTest
{
    @Test
    public void catchQuantitySelectsMatchingAudioAndVisualEvent() throws Exception
    {
        for (int quantity : new int[]{1, 2})
        {
            TenchAudio audio = mock(TenchAudio.class);
            TenchCelebration overlay = new TenchCelebration(mock(Client.class),
                new AerialFishingConfig() { }, audio, mock(ItemManager.class));
            overlay.show(quantity, 42, true);
            verify(audio).play(eq(quantity == 2), eq(60), any(Runnable.class));
            java.lang.reflect.Field field = TenchCelebration.class.getDeclaredField("event");
            field.setAccessible(true);
            TenchCelebration.Event event = (TenchCelebration.Event) field.get(overlay);
            assertEquals(quantity == 2, event.doubled);
            assertEquals(quantity == 2 ? 152 : 76, event.particles.length);
            assertEquals(42, event.interval);
        }
    }
    @Test
    public void bundledCustomSoundsArePcmAndHaveExpectedDurations() throws Exception
    {
        String[] names = {"single-tench", "double-tench"};
        double[] seconds = {1.9591875, 3.822};
        for (int i = 0; i < names.length; i++)
        {
            try (BufferedInputStream input = new BufferedInputStream(getClass().getResourceAsStream("/audio/" + names[i] + ".wav"));
                 AudioInputStream audio = AudioSystem.getAudioInputStream(input))
            {
                assertEquals(48000, audio.getFormat().getSampleRate(), 0);
                assertEquals(16, audio.getFormat().getSampleSizeInBits());
                assertEquals(seconds[i], audio.getFrameLength() / 48000.0, .001);
            }
        }
    }

    @Test
    public void secondBurstIsDelayedAndParticlesAreVariedAndFinite()
    {
        TenchCelebration.Event event = new TenchCelebration.Event(123, true, 18432, true);
        assertEquals(152, event.particles.length);
        for (int i = 0; i < event.particles.length; i++)
        {
            TenchCelebration.Particle particle = event.particles[i];
            assertTrue(particle.delay >= (i < 76 ? 0 : .75));
            assertTrue(particle.delay < (i < 76 ? .14 : .89));
            assertTrue(Double.isFinite(particle.x(3, 512)));
            assertTrue(Double.isFinite(particle.y(3)));
            assertTrue(particle.delay + TenchCelebration.PARTICLE_SECONDS < event.duration());
        }
        assertNotEquals(event.particles[0].y(3), event.particles[1].y(3), .001);
    }

    @Test
    public void rendersAtNarrowAndWideSizesWithoutTouchingClientState()
    {
        Client client = mock(Client.class);
        TenchCelebration overlay = new TenchCelebration(client, new AerialFishingConfig() { }, mock(TenchAudio.class), mock(ItemManager.class));
        for (int width : new int[]{320, 512, 1280})
        {
            BufferedImage image = new BufferedImage(width, 400, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            try { overlay.draw(g, width, 400, new TenchCelebration.Event(123, true, 18432, true), 1.2); }
            finally { g.dispose(); }
            assertNotEquals(0, image.getRGB(width / 2, 100));
        }
        verifyNoInteractions(client);
    }

    @Test
    public void cancelledAudioWorkDoesNotLoadOrStartLater()
    {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        org.mockito.ArgumentCaptor<Runnable> task = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        TenchAudio audio = new TenchAudio(executor, mock(net.runelite.client.audio.AudioPlayer.class));
        Runnable started = mock(Runnable.class);
        audio.play(true, 50, started);
        verify(executor).execute(task.capture());
        audio.stop();
        task.getValue().run();
        verifyNoInteractions(started);
    }

    @Test
    public void soundCanBeDisabledAndClearStopsIt()
    {
        TenchAudio audio = mock(TenchAudio.class);
        TenchCelebration overlay = new TenchCelebration(mock(Client.class), new AerialFishingConfig() {
            @Override public boolean celebrationSound() { return false; }
        }, audio, mock(ItemManager.class));
        overlay.show(1, 10, false);
        verify(audio, never()).play(anyBoolean(), anyInt(), any(Runnable.class));
        overlay.clear();
        verify(audio, times(2)).stop();
    }
}
