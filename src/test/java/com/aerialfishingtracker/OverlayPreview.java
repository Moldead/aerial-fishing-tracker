package com.aerialfishingtracker;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import static org.mockito.Mockito.mock;
import net.runelite.api.Client;

public final class OverlayPreview
{
    private OverlayPreview() { }

    public static void main(String[] args) throws Exception
    {
        AerialFishingOverlay overlay = new AerialFishingOverlay(new AerialFishingPlugin(), new AerialFishingConfig() { }, mock(Client.class));
        FishingStats stats = new FishingStats();
        for (int i = 1; i <= 532; i++)
        {
            stats.recordCatch(i % 43 == 0, 0);
        }
        stats.addHistory(10000, 130, 0);
        FishingSession session = new FishingSession();
        long timeout = FishingSession.timeoutNanos(15);
        for (int i = 0; i < 80; i++) { session.recordCatch(i, i % 30 == 0, timeout); }
        BufferedImage image = new BufferedImage(320, 330, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.translate(12, 12);
        // RuneLite PanelComponent measures children on its first frame, then paints the measured background.
        overlay.renderStats(graphics, stats, session, 91, 85, null);
        graphics.setComposite(java.awt.AlphaComposite.Clear);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setComposite(java.awt.AlphaComposite.SrcOver);
        overlay.renderStats(graphics, stats, session, 91, 85, null);
        graphics.dispose();
        File output = new File("build/overlay-preview.png");
        if (!output.getParentFile().isDirectory() && !output.getParentFile().mkdirs())
        {
            throw new IllegalStateException("Cannot create output directory");
        }
        ImageIO.write(image, "png", output);
    }
}
