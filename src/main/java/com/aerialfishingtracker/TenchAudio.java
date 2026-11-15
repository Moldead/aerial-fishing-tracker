package com.aerialfishingtracker;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.audio.AudioPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Delegates device management to RuneLite; loading never blocks the game or Swing thread. */
@Singleton
final class TenchAudio
{
    private static final Logger LOG = LoggerFactory.getLogger(TenchAudio.class);
    private final ScheduledExecutorService executor;
    private final AudioPlayer player;
    private final AtomicLong generation = new AtomicLong();

    @Inject
    TenchAudio(ScheduledExecutorService executor, AudioPlayer player)
    {
        this.executor = executor;
        this.player = player;
    }

    void play(boolean doubled, int volume, Runnable started)
    {
        long request = generation.incrementAndGet();
        int level = Math.max(0, Math.min(100, volume));
        executor.execute(() ->
        {
            if (request != generation.get()) { return; }
            try
            {
                if (level > 0)
                {
                    float gain = (float) (20 * Math.log10(level / 100.0));
                    player.play(TenchAudio.class,
                        doubled ? "/audio/double-tench.wav" : "/audio/single-tench.wav", gain);
                }
            }
            catch (Exception ex)
            {
                LOG.warn("Unable to play tench celebration audio", ex);
            }
            finally
            {
                // AudioPlayer returns after starting playback. Failure/mute still shows the banner.
                if (request == generation.get()) { started.run(); }
            }
        });
    }

    void stop()
    {
        // AudioPlayer has no stop handle. Cancel queued requests; an already-started short
        // sound finishes under RuneLite's ownership, including a call currently opening a device.
        generation.incrementAndGet();
    }
}
