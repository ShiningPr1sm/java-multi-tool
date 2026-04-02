import db.DB;

import ui.MainFrame;
import ui.AuthFrame;

import db.AchievementDB;
import db.WorkflowDB;

import ui.UIStyle;
import ui.settings.SettingsPanel;
import ui.utils.AppLogger;
import ui.utils.AuthService;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        AppLogger.info("=== Application Starting ===");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            AppLogger.error("UIManager initialization: " + e.getMessage());
        }

        try {
            DB.initializeDatabase();
            AuthService.initializeRights();
            WorkflowDB.initializeDatabase();
            AppLogger.info("Core databases and rights initialized.");
        } catch (Exception e) {
            AppLogger.error("Core initialization failed: " + e.getMessage());
        }

        try {
            AchievementDB.initializeDatabase();
            AchievementDB.insertDefaultAchievements();
            AppLogger.info("Achievements system ready.");
        } catch (Exception e) {
            AppLogger.error("Achievements system failed: " + e.getMessage());
        }

        String savedLogin = DB.getAutoLoginUser();
        SwingUtilities.invokeLater(() -> {
            if (savedLogin != null && !savedLogin.isBlank()) {
                String userTheme = DB.getTheme(savedLogin);
                UIStyle.applyTheme(userTheme);

                MainFrame mf = new MainFrame(savedLogin);
                AchievementDB.setMainFrame(mf);
                AchievementDB.syncUserAchievements(savedLogin);
                AppLogger.info("Auto-login: User '" + savedLogin + "' entered the system.");
            } else {
                new AuthFrame();
                AppLogger.info("Waiting for manual login...");
            }
        });
        new Thread(SettingsPanel::prepareSystemInfo).start();
    }
}