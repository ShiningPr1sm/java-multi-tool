package ui.settings;

import ui.UIStyle;
import util.AppLogger;
import util.ConfigManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AdvancedSettingsDialog extends JDialog {

    private record SettingDef(String label, String key, boolean defaultValue) {}

    private static final List<SettingDef> SETTINGS = List.of(
            new SettingDef("Enable Auto-update", "autoUpdate", true),
            new SettingDef("Show Daily Quote", "dailyQuote", true),
            new SettingDef("Birthday Reminders", "birthdayReminders", true)
    );

    public AdvancedSettingsDialog(Frame owner) {
        super(owner, "Advanced Settings", true);
        UIStyle.setAppIcon(this);
        setBackground(UIStyle.BG_COLOR);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(UIStyle.BG_COLOR);
        content.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 10, 4, 10);

        JLabel nameHeader = new JLabel("Preference Name", SwingConstants.LEFT);
        nameHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameHeader.setForeground(UIStyle.ACCENT_COLOR);
        content.add(nameHeader, c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        JLabel valueHeader = new JLabel("Value");
        valueHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        valueHeader.setForeground(UIStyle.ACCENT_COLOR);
        content.add(valueHeader, c);

        c.insets = new Insets(4, 10, 4, 10);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        for (SettingDef def : SETTINGS) {
            c.gridy++;
            c.gridx = 0;
            JLabel label = new JLabel(def.label());
            label.setForeground(UIStyle.TEXT_COLOR);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            content.add(label, c);

            c.gridx = 1;
            c.anchor = GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.NONE;
            boolean value = Boolean.parseBoolean(ConfigManager.loadProperty(def.key(), String.valueOf(def.defaultValue())));
            JCheckBox cb = new JCheckBox();
            UIStyle.styleCheckbox(cb);
            cb.setSelected(value);
            cb.addActionListener(e -> {
                ConfigManager.saveProperty(def.key(), String.valueOf(cb.isSelected()));
                AppLogger.info("Settings: '" + def.label() + "' set to " + cb.isSelected());
            });
            content.add(cb, c);

            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
        }

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.EAST;
        JButton closeBtn = new JButton("Close");
        UIStyle.styleButton(closeBtn);
        closeBtn.addActionListener(e -> dispose());
        content.add(closeBtn, c);

        setContentPane(content);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }
}
