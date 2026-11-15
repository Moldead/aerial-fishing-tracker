package com.aerialfishingtracker;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Audio device work never runs on the game or Swing thread. */
@Singleton
final class TenchAudio
{
    private static final Logger LOG = LoggerFactory.getLogger(TenchAudio.class);
    private final ScheduledExecutorService executor;
    private final AtomicLong generation = new AtomicLong();
    private final AtomicReference<Clip> active = new AtomicReference<>();
    private final Object playbackLock = new Object(); // Worker threads only; never acquired by stop/play.

    @Inject
    TenchAudio(ScheduledExecutorService executor) { this.executor = executor; }

    void play(boolean doubled, int volume, Runnable started)
    {
        long request = generation.incrementAndGet();
        int level = Math.max(0, Math.min(100, volume));
        executor.execute(() -> load(request, doubled, level, started));
    }

    void stop()
    {
        generation.incrementAndGet();
        Clip old = active.getAndSet(null);
        if (old != null) { executor.execute(old::close); }
    }

    private void load(long request, boolean doubled, int volume, Runnable started)
    {
        Clip clip = null;
        try
        {
            if (request != generation.get()) { return; }
            if (volume == 0) { return; }
            String path = doubled ? "/audio/double-tench.wav" : "/audio/single-tench.wav";
            InputStream resource = TenchAudio.class.getResourceAsStream(path);
            if (resource == null) { return; }
            try (BufferedInputStream input = new BufferedInputStream(resource);
                 AudioInputStream stream = AudioSystem.getAudioInputStream(input))
            {
                clip = AudioSystem.getClip();
                clip.open(stream);
            }
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
            {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(),
                    (float) (20 * Math.log10(volume / 100.0)))));
            }
            else if (volume < 100)
            {
                // Never unexpectedly play at full volume when the requested gain is unsupported.
                clip.close();
                return;
            }
            Clip ready = clip;
            clip.addLineListener(event ->
            {
                if (event.getType() == LineEvent.Type.STOP)
                {
                    executor.execute(() ->
                    {
                        ready.close();
                        active.compareAndSet(ready, null);
                    });
                }
            });
            synchronized (playbackLock)
            {
                if (request != generation.get()) { clip.close(); return; }
                Clip previous = active.getAndSet(clip);
                if (previous != null) { previous.close(); }
                if (request != generation.get())
                {
                    active.compareAndSet(clip, null);
                    clip.close();
                    return;
                }
                started.run();
                clip.start();
                if (request != generation.get()) { clip.close(); active.compareAndSet(clip, null); }
            }
        }
        catch (Exception ex)
        {
            if (clip != null) { clip.close(); }
            LOG.warn("Unable to play tench celebration audio", ex);
        }
        finally
        {
            // Silent/device-failure paths still display the visual notification.
            if (request == generation.get()) { started.run(); }
        }
    }
}
