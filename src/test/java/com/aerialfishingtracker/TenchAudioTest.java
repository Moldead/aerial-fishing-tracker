package com.aerialfishingtracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import net.runelite.client.audio.AudioPlayer;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

public class TenchAudioTest
{
    @Test
    public void singleUsesRuneLiteResourcePlaybackAndDecibelVolume() throws Exception
    {
        AudioPlayer player = mock(AudioPlayer.class);
        List<Runnable> queue = new ArrayList<>();
        TenchAudio audio = audio(player, queue);
        Runnable started = mock(Runnable.class);
        audio.play(false, 60, started);
        verifyNoInteractions(player, started);
        queue.get(0).run();
        org.mockito.ArgumentCaptor<Float> gain = org.mockito.ArgumentCaptor.forClass(Float.class);
        verify(player).play(eq(TenchAudio.class), eq("/audio/single-tench.wav"), gain.capture());
        assertEquals(20 * Math.log10(.6), gain.getValue(), .0001);
        verify(started).run();
    }

    @Test
    public void mutedSoundStillStartsVisualsWithoutAudio()
    {
        AudioPlayer player = mock(AudioPlayer.class);
        List<Runnable> queue = new ArrayList<>();
        TenchAudio audio = audio(player, queue);
        Runnable started = mock(Runnable.class);
        audio.play(true, 0, started);
        queue.get(0).run();
        verifyNoInteractions(player);
        verify(started).run();
    }

    @Test
    public void playbackFailureStillStartsVisuals() throws Exception
    {
        AudioPlayer player = mock(AudioPlayer.class);
        doThrow(new IOException("test device failure"))
            .when(player).play(any(Class.class), anyString(), anyFloat());
        List<Runnable> queue = new ArrayList<>();
        TenchAudio audio = audio(player, queue);
        Runnable started = mock(Runnable.class);
        audio.play(true, 60, started);
        queue.get(0).run();
        verify(started).run();
    }

    @Test
    public void newerRequestSupersedesQueuedSoundAndClampsVolume() throws Exception
    {
        AudioPlayer player = mock(AudioPlayer.class);
        List<Runnable> queue = new ArrayList<>();
        TenchAudio audio = audio(player, queue);
        Runnable old = mock(Runnable.class);
        Runnable latest = mock(Runnable.class);
        audio.play(false, 60, old);
        audio.play(true, 200, latest);
        queue.get(0).run();
        queue.get(1).run();
        verifyNoInteractions(old);
        verify(player).play(TenchAudio.class, "/audio/double-tench.wav", 0f);
        verifyNoMoreInteractions(player);
        verify(latest).run();
    }

    private static TenchAudio audio(AudioPlayer player, List<Runnable> queue)
    {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        doAnswer(call -> { queue.add(call.getArgument(0)); return null; })
            .when(executor).execute(any(Runnable.class));
        return new TenchAudio(executor, player);
    }
}