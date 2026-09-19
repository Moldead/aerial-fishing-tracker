package com.aerialfishingtracker;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AerialFishingOverlayTest
{
    @Test
    public void defaultRendererHasStableBoundsAndDoesNotLeakGraphicsState()
    {
        AerialFishingOverlay overlay = new AerialFishingOverlay(new AerialFishingPlugin(), new AerialFishingConfig() { }, mock(Client.class));
        Graphics2D graphics = new BufferedImage(500, 600, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try
        {
            java.awt.Font before = graphics.getFont();
            FishingStats stats = new FishingStats();
            FishingSession session = new FishingSession();
            overlay.renderStats(graphics, stats, session, 99, 99, null);
            Dimension measured = overlay.renderStats(graphics, stats, session, 99, 99, null);
            assertEquals(170, measured.width);
            assertTrue(measured.height >= 160);
            for (int i = 0; i < 10; i++)
            {
                assertEquals(measured, overlay.renderStats(graphics, stats, session, 99, 99, null));
            }
            assertEquals(before, graphics.getFont());
        }
        finally { graphics.dispose(); }
    }

    @Test
    public void allRowsDisabledHidesOverlayAndBothChanceScopesRender()
    {
        AerialFishingConfig config = mock(AerialFishingConfig.class);
        AerialFishingOverlay overlay = new AerialFishingOverlay(new AerialFishingPlugin(), config, mock(Client.class));
        Graphics2D graphics = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try
        {
            FishingStats stats = new FishingStats();
            FishingSession session = new FishingSession();
            assertNull(overlay.renderStats(graphics, stats, session, 99, 99, null));
            when(config.tenchChance()).thenReturn(true);
            when(config.tenchScope()).thenReturn(TenchScope.SESSION);
            assertNotNull(overlay.renderStats(graphics, stats, session, 99, 99, null));
            when(config.tenchScope()).thenReturn(TenchScope.LIFETIME);
            when(config.fontSize()).thenReturn(22);
            stats.addHistory(Long.MAX_VALUE, 0, 0);
            assertNotNull(overlay.renderStats(graphics, stats, session, 99, 99, null));
        }
        finally { graphics.dispose(); }
    }
}
