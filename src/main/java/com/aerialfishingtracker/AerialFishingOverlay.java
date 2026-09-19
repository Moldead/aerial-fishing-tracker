package com.aerialfishingtracker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.FontManager;

final class AerialFishingOverlay extends OverlayPanel
{
    static final Color PEARL = new Color(190, 255, 202);
    static final Color STREAK = new Color(184, 226, 255);
    static final Color TENCH = new Color(255, 188, 223);
    private final AerialFishingPlugin plugin;
    private final AerialFishingConfig config;
    private final Client client;
    private Font font;

    @Inject
    AerialFishingOverlay(AerialFishingPlugin plugin, AerialFishingConfig config, Client client)
    {
        super(plugin);
        this.plugin = plugin;
        this.config = config;
        this.client = client;
        setPosition(OverlayPosition.TOP_LEFT);
        panelComponent.setPreferredSize(new Dimension(170, 0));
        panelComponent.setGap(new Point(0, 4));
        addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Reset session only", "Aerial Fishing Tracker",
            event -> plugin.resetSession(plugin.profile()));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.shouldDisplayOverlay() || plugin.stats() == null)
        {
            return null;
        }
        return renderStats(graphics, plugin.stats(), plugin.session(),
            client.getBoostedSkillLevel(Skill.FISHING), client.getBoostedSkillLevel(Skill.HUNTER), plugin.error());
    }

    // Package visibility allows the real renderer to be exercised headlessly.
    Dimension renderStats(Graphics2D graphics, FishingStats stats, FishingSession session, int fishing, int hunter, String error)
    {
        int size = Math.max(11, Math.min(22, config.fontSize()));
        if (font == null || font.getSize() != size)
        {
            font = FontManager.getRunescapeFont().deriveFont(Font.PLAIN, (float) size);
        }
        panelComponent.setBackgroundColor(new Color(23, 29, 36, Math.max(0, Math.min(255, config.backgroundOpacity()))));
        if (error != null)
        {
            row("Tracking paused", "Check sidebar", TENCH);
        }
        if (config.lifetimeCatches()) { row("Lifetime fish", number(stats.totalCatches()), Color.WHITE); }
        if (config.sessionCatches()) { row("Sessions fish", number(session.catches), Color.WHITE); }
        if (config.sincePearl()) { row("Rolls since pearl", number(stats.sincePearl), Color.WHITE); }
        long now = System.nanoTime();
        if (config.fishPerHour()) { row("Fish / hour", hourly(session.fishPerHour(now)), Color.WHITE); }
        if (config.sessionPearls()) { row("Session pearls", number(session.pearls), PEARL); }
        if (config.lifetimePearls()) { row("Lifetime pearls", number(stats.totalPearls()), PEARL); }
        if (config.observedRate())
        {
            double actual = stats.actualPearlDenominator();
            row("Actual pearl rate", Double.isNaN(actual) ? "--" : ratio(actual), PEARL);
        }
        if (config.expectedRate()) { row("Wiki pearl rate", ratio(FishingMath.estimatedPearlDenominator(fishing, hunter)), PEARL); }
        if (config.pearlsPerHour()) { row("Pearls / hour", hourly(session.pearlsPerHour(now)), PEARL); }
        if (config.longestDry()) { row("Longest dry", number(stats.longestDry), STREAK); }
        if (config.shortestGap()) { row("Most spooned", stats.shortestGap == 0 ? "--" : number(stats.shortestGap), STREAK); }
        if (config.tenches()) { row("Golden tenches", number(stats.totalTenches()), TENCH); }
        if (config.tenchChance())
        {
            boolean lifetime = config.tenchScope() == TenchScope.LIFETIME;
            double percent = 100 * FishingMath.tenchChance(lifetime ? stats.totalCatches() : session.catches);
            // Never round a non-certain event up to a displayed guarantee.
            String chance = percent >= 99.9999 ? ">99.9999%" : String.format(Locale.US, "%.4f%%", percent);
            row(lifetime ? "Tench odds (L)" : "Tench odds (S)", chance, TENCH);
        }
        if (panelComponent.getChildren().isEmpty())
        {
            return null;
        }
        Graphics2D copy = (Graphics2D) graphics.create();
        try
        {
            copy.setFont(font);
            return super.render(copy);
        }
        finally
        {
            copy.dispose();
        }
    }

    private void row(String label, String value, Color color)
    {
        panelComponent.getChildren().add(LineComponent.builder().left(label + ":").right(value)
            .leftColor(color).rightColor(color).leftFont(font).rightFont(font).build());
    }

    private static String number(long value) { return String.format(Locale.US, "%,d", value); }
    private static String ratio(double denominator) { return String.format(Locale.US, "1 / %.1f", denominator); }
    private static String hourly(double rate) { return Double.isNaN(rate) ? "--" : String.format(Locale.US, "%,.0f", rate); }
}
