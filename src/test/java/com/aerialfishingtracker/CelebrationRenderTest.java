package com.aerialfishingtracker;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import static org.mockito.Mockito.mock;

/** Headless artifact from the production renderer, not a player-facing preview. */
public class CelebrationRenderTest
{
    @Test
    public void renderReviewFrames() throws Exception
    {
        TenchCelebration overlay = new TenchCelebration(mock(Client.class), new AerialFishingConfig() { },
            mock(TenchAudio.class), mock(ItemManager.class));
        Files.createDirectories(Path.of("build", "celebration-review"));
        for (int width : new int[]{320, 765})
        {
            BufferedImage image = new BufferedImage(width, 400, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            try
            {
                g.setColor(new Color(38, 56, 60));
                g.fillRect(0, 0, width, 400);
                overlay.draw(g, width, 400, new TenchCelebration.Event(123, true, 18432, false), 1.5);
            }
            finally { g.dispose(); }
            ImageIO.write(image, "png", Path.of("build", "celebration-review", "double-" + width + ".png").toFile());
        }
    }
}
