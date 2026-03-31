package ui.utils;

import ui.MainFrame;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TrayManager {
    private final MainFrame frame;

    public TrayManager(MainFrame frame) {
        this.frame = frame;
        AppLogger.info("TrayManager: Starting initialization...");
        setupTray();
    }

    private void setupTray() {
        if (!SystemTray.isSupported()) {
            AppLogger.error("TrayManager: SystemTray is not supported on this OS.");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            java.net.URL iconUrl = getClass().getResource("/project_icon.png");

            if (iconUrl == null) {
                AppLogger.error("TrayManager: project_icon.png NOT FOUND in resources!");
                return;
            }

            Image originalImage = new ImageIcon(iconUrl).getImage();
            Image scaledImage = originalImage.getScaledInstance(16, 16, Image.SCALE_SMOOTH);

            PopupMenu menu = new PopupMenu();

            MenuItem showItem = new MenuItem("Open MultiTool");
            showItem.addActionListener(_ -> showFrame());

            MenuItem downloaderItem = new MenuItem("Media Downloader");
            downloaderItem.addActionListener(_ -> {
                showFrame();
                frame.openTab("Media Downloader");
            });

            MenuItem bdaysItem = new MenuItem("BDays Notifier");
            bdaysItem.addActionListener(_ -> {
                showFrame();
                frame.openTab("BDays notifier");
            });

            MenuItem settingsItem = new MenuItem("Settings");
            settingsItem.addActionListener(_ -> {
                showFrame();
                frame.openTab("Settings");
            });

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(_ -> System.exit(0));

            menu.add(showItem);
            menu.addSeparator();
            menu.add(downloaderItem);
            menu.add(bdaysItem);
            menu.add(settingsItem);
            menu.addSeparator();
            menu.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(scaledImage, "MultiTool", menu);
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("MultiTool is active");

            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) showFrame();
                }
            });

            tray.add(trayIcon);
            AppLogger.info("TrayManager: TrayIcon successfully added to taskbar.");
        } catch (Exception e) {
            AppLogger.error("TrayManager: Failed to setup tray: " + e.getMessage());
        }
    }

    private void showFrame() {
        frame.setVisible(true);
        frame.setExtendedState(JFrame.NORMAL);
        frame.toFront();
        AppLogger.info("TrayManager: MainFrame restored from tray.");
    }
}