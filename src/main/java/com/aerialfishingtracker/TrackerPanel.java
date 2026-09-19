package com.aerialfishingtracker;

import java.awt.Color;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;

/** User actions capture the displayed profile; the client thread rechecks it before writing. */
final class TrackerPanel extends PluginPanel
{
    private static final long serialVersionUID = 1L;
    private final AerialFishingPlugin plugin;
    private final JLabel character = new JLabel("Log in to begin");
    private final JLabel total = new JLabel("Lifetime fish: 0");
    private final JLabel status = new JLabel(" ");
    private final JTextField fish = new JTextField("0");
    private final JTextField pearls = new JTextField("0");
    private final JTextField tenches = new JTextField("0");
    private final JButton addHistory = new JButton("Add historical counts");
    private final JButton resetSession = new JButton("Reset session only");
    private String displayedProfile;

    TrackerPanel(AerialFishingPlugin plugin)
    {
        this.plugin = plugin;
        JLabel title = new JLabel("Aerial Fishing Tracker");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        title.setForeground(Color.WHITE);
        add(title);
        add(character);
        add(total);
        add(new JLabel("<html><br>Historical catches</html>"));
        add(new JLabel("<html>Enter only amounts caught before this tracker. Additions are permanent and belong to the character shown above.</html>"));
        add(new JLabel("Previous successful fish"));
        add(fish);
        add(new JLabel("Previous pearls (optional)"));
        add(pearls);
        add(new JLabel("Previous tenches (optional)"));
        add(tenches);
        add(addHistory);
        add(new JLabel("<html>History affects lifetime totals and actual pearl rate. Sessions and tracked streaks are unchanged.</html>"));
        add(new JLabel("<html><br>Current session</html>"));
        add(resetSession);
        add(new JLabel("<html>The idle timeout is set in plugin settings. RuneLite restarts always begin a new session. Lifetime totals are kept.</html>"));
        add(status);
        addHistory.setEnabled(false);
        resetSession.setEnabled(false);

        addHistory.addActionListener(event ->
        {
            try
            {
                long fishCount = amount(fish);
                long pearlCount = amount(pearls);
                long tenchCount = amount(tenches);
                if (fishCount == 0 && pearlCount == 0 && tenchCount == 0)
                {
                    showMessage("Enter at least one amount greater than zero.");
                    return;
                }
                confirmHistory(fishCount, pearlCount, tenchCount, true);
            }
            catch (IllegalArgumentException ex)
            {
                showMessage("Use whole non-negative numbers, without commas.");
            }
        });
        resetSession.addActionListener(event -> plugin.resetSession(displayedProfile));
    }

    private static long amount(JTextField field)
    {
        long value = Long.parseLong(field.getText().trim());
        if (value < 0)
        {
            throw new IllegalArgumentException("Negative amount");
        }
        return value;
    }

    void confirmHistoryFromSettings(String fishText, String pearlText, String tenchText)
    {
        SwingUtilities.invokeLater(() ->
        {
            try
            {
                confirmHistory(amount(fishText), amount(pearlText), amount(tenchText), false);
            }
            catch (IllegalArgumentException ex)
            {
                showMessage("Historical settings need whole non-negative numbers, without commas.");
            }
        });
    }

    private void confirmHistory(long fishCount, long pearlCount, long tenchCount, boolean clearPanelFields)
    {
        final String targetProfile = displayedProfile;
        if (targetProfile == null)
        {
            showMessage("Log in before adding historical counts.");
            return;
        }
        if (fishCount == 0 && pearlCount == 0 && tenchCount == 0)
        {
            showMessage("Enter at least one amount greater than zero.");
            return;
        }
        String summary = "Add to " + character.getText() + ":\n" + fishCount + " fish\n"
            + pearlCount + " pearls\n" + tenchCount + " tenches\n\nThese are permanent additions, not replacement totals.";
        if (JOptionPane.showConfirmDialog(this, summary, "Add historical counts", JOptionPane.OK_CANCEL_OPTION)
            == JOptionPane.OK_OPTION)
        {
            if (clearPanelFields)
            {
                fish.setText("0");
                pearls.setText("0");
                tenches.setText("0");
            }
            else
            {
                plugin.clearHistorySettings();
            }
            plugin.addHistory(targetProfile, fishCount, pearlCount, tenchCount);
        }
    }

    private static long amount(String value)
    {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty())
        {
            return 0;
        }
        long parsed = Long.parseLong(trimmed);
        if (parsed < 0)
        {
            throw new IllegalArgumentException("Negative amount");
        }
        return parsed;
    }

    void update(String profile, String name, long catches, boolean editable, String error)
    {
        SwingUtilities.invokeLater(() ->
        {
            if (!Objects.equals(displayedProfile, profile))
            {
                fish.setText("0");
                pearls.setText("0");
                tenches.setText("0");
                status.setText(" ");
            }
            displayedProfile = profile;
            character.setText(name);
            total.setText("Lifetime fish: " + catches);
            addHistory.setEnabled(editable);
            resetSession.setEnabled(editable);
            if (error != null)
            {
                status.setText("<html>" + error + "</html>");
            }
        });
    }

    void showMessage(String message)
    {
        SwingUtilities.invokeLater(() -> status.setText("<html>" + message + "</html>"));
    }
}
