package com.aerialfishingtracker;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Bounded, click-through rendering. No game state queries outside the client render thread. */
@Singleton
final class TenchCelebration extends Overlay
{
    static final double SECOND_BURST = 0.75;
    static final double PARTICLE_SECONDS = 4.8;
    private static final Color[] GOLD = {new Color(255, 209, 102), new Color(255, 240, 181), new Color(212, 154, 53)};
    private final Client client;
    private final AerialFishingConfig config;
    private final TenchAudio audio;
    private final ItemManager items;
    private volatile Event event;

    @Inject
    TenchCelebration(Client client, AerialFishingConfig config, TenchAudio audio, ItemManager items)
    {
        this.client = client;
        this.config = config;
        this.audio = audio;
        this.items = items;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGH);
    }

    void show(int quantity, long interval, boolean known)
    {
        boolean sound = config.celebrationSound() && config.celebrationVolume() > 0;
        Event next = new Event(System.nanoTime(), quantity == 2, interval, known);
        next.awaitingAudio = sound;
        event = next;
        audio.stop();
        if (sound)
        {
            audio.play(next.doubled, config.celebrationVolume(), () ->
            {
                if (event == next && next.awaitingAudio)
                {
                    next.started = System.nanoTime();
                    next.awaitingAudio = false;
                }
            });
        }
    }

    void clear() { event = null; audio.stop(); }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Event current = event;
        if (current == null) { return null; }
        double elapsed = (System.nanoTime() - current.started) / 1_000_000_000.0;
        if (current.awaitingAudio)
        {
            // A malfunctioning audio driver must not hide a catch notification indefinitely.
            if (elapsed < 2) { return null; }
            current.started = System.nanoTime();
            current.awaitingAudio = false;
            audio.stop();
            elapsed = 0;
        }
        if (elapsed >= current.duration()) { event = null; return null; }
        if (client.getGameState() != GameState.LOGGED_IN) { return null; }
        Rectangle viewport = new Rectangle(client.getViewportXOffset(), client.getViewportYOffset(),
            client.getViewportWidth(), client.getViewportHeight());
        if (viewport.width < 100 || viewport.height < 100) { return null; }
        Graphics2D g = (Graphics2D) graphics.create();
        try
        {
            g.clip(viewport);
            g.translate(viewport.x, viewport.y);
            draw(g, viewport.width, viewport.height, current, elapsed);
        }
        finally { g.dispose(); }
        return null;
    }

    void draw(Graphics2D g, int width, int height, Event current, double elapsed)
    {
        if (elapsed < 0 || elapsed >= current.duration()) { return; }
        if (config.celebrationConfetti() && config.celebrationMotion())
        {
            AffineTransform original = g.getTransform();
            for (Particle p : current.particles)
            {
                double t = elapsed - p.delay;
                if (t < 0 || t >= PARTICLE_SECONDS) { continue; }
                double x = (p.left ? 0 : width) + p.x(t, width);
                double y = height * 0.68 + p.y(t) * height / 390.0;
                float fade = (float) Math.min(1, (PARTICLE_SECONDS - t) / 0.5);
                g.setComposite(AlphaComposite.SrcOver.derive(fade));
                g.translate(x, y);
                g.rotate(p.phase + p.spin * t);
                g.scale(0.25 + 0.75 * Math.abs(Math.cos(t * p.flutter + p.phase)), 1);
                if (current.doubled && p.color == 1)
                {
                    g.setColor(new Color(255, 209, 102, 28));
                    g.fillOval(-6, -8, 12, 16);
                    g.setColor(new Color(255, 240, 181, 60));
                    g.fillOval(-4, -6, 8, 12);
                }
                g.setColor(GOLD[p.color]);
                g.fillRect(-2, -4, 4, 8);
                g.setTransform(original);
            }
            g.setComposite(AlphaComposite.SrcOver);
        }
        // Keep the catch announcement legible while particles pass behind it.
        if (config.celebrationBanner())
        {
            drawBanner(g, width, height, current, elapsed);
        }
    }

    private void drawBanner(Graphics2D g, int width, int height, Event current, double elapsed)
    {
        int boxWidth = Math.min(350, width - 20);
        int x = (width - boxWidth) / 2;
        int y = Math.min(92, Math.max(12, height / 5));
        float fade = (float) Math.min(1, (current.duration() - elapsed) / .35);
        g.setComposite(AlphaComposite.SrcOver.derive(fade));
        double pulse = current.doubled && config.celebrationMotion()
            ? 0.7 + 0.3 * Math.sin(elapsed * Math.PI * 2 / 1.8) : 0.65;
        for (int radius = 18; radius > 0; radius -= 3)
        {
            g.setColor(new Color(255, 209, 102, (int) (pulse * (20 - radius))));
            g.fillRoundRect(x - radius, y - radius, boxWidth + radius * 2, 110 + radius * 2, 12, 12);
        }
        g.setColor(new Color(32, 28, 22, 245));
        g.fillRect(x, y, boxWidth, 110);
        g.setColor(new Color(221, 183, 104));
        g.drawRect(x, y, boxWidth, 110);
        // Use a scalable font: enlarging the game pixel font produces uneven letter strokes.
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        g.setFont(font.deriveFont(Font.PLAIN, 12f));
        g.setColor(new Color(196, 185, 159));
        center(g, "AERIAL FISHING TRACKER", width, y + 17);
        BufferedImage icon = items.getImage(ItemID.GOLDEN_TENCH);
        if (icon != null) { g.drawImage(icon, (width - icon.getWidth()) / 2, y + 20, null); }
        String title = current.doubled ? "DOUBLE GOLDEN TENCH!" : "GOLDEN TENCH!";
        g.setFont(font.deriveFont(Font.PLAIN, 20f));
        if (g.getFontMetrics().stringWidth(title) > boxWidth - 12) { g.setFont(font.deriveFont(Font.PLAIN, 15f)); }
        g.setColor(new Color(255, 218, 118));
        center(g, title, width, y + 73);
        g.setFont(font.deriveFont(Font.PLAIN, 13f));
        g.setColor(new Color(238, 227, 200));
        String count = String.format(Locale.US, "%,d fish", current.interval);
        center(g, count, width, y + 89);
        center(g, current.known ? "since last golden tench" : "since tench-interval tracking began", width, y + 104);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private static void center(Graphics2D g, String text, int width, int y)
    {
        g.drawString(text, (width - g.getFontMetrics().stringWidth(text)) / 2, y);
    }

    static final class Event
    {
        volatile long started;
        volatile boolean awaitingAudio;
        final boolean doubled;
        final long interval;
        final boolean known;
        final Particle[] particles;

        Event(long started, boolean doubled, long interval, boolean known)
        {
            this.started = started;
            this.doubled = doubled;
            this.interval = interval;
            this.known = known;
            particles = new Particle[doubled ? 152 : 76];
            Random random = new Random(started);
            for (int i = 0; i < particles.length; i++)
            {
                particles[i] = new Particle(random, i, i >= 76 ? SECOND_BURST : 0);
            }
        }

        double duration() { return doubled ? 5.75 : 5; }
    }

    static final class Particle
    {
        final boolean left;
        final int color;
        final double delay, spread, drag, fall, launch, sway, phase, spin, flutter;
        Particle(Random random, int index, double burst)
        {
            left = index % 2 == 0;
            color = index % 3;
            delay = burst + random.nextDouble() * .14;
            spread = .06 + .88 * random.nextDouble();
            drag = 2.5 + random.nextDouble() * 1.2;
            fall = 85 + random.nextDouble() * 65;
            launch = -(650 + random.nextDouble() * 300);
            sway = 8 + random.nextDouble() * 18;
            phase = random.nextDouble() * Math.PI * 2;
            spin = (random.nextDouble() - .5) * 5.2;
            flutter = 3 + random.nextDouble() * 3;
        }
        double x(double t, int width)
        {
            return (left ? 1 : -1) * spread * width * (1 - Math.exp(-2.7 * t))
                + sway * (Math.sin(t * flutter + phase) - Math.sin(phase)) * (1 - Math.exp(-2 * t));
        }
        double y(double t) { return fall * t + (launch - fall) * (1 - Math.exp(-drag * t)) / drag; }
    }
}
