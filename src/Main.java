import db.DB;
import ui.AuthFrame;
import ui.MainFrame;
import ui.utils.AuthService;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        AuthService.initializeRights();
        DB.initializeDatabase();

        String savedLogin = DB.getAutoLoginUser();
        SwingUtilities.invokeLater(() -> {
            if (savedLogin != null && !savedLogin.isBlank()) {
                new MainFrame(savedLogin);
            } else {
                new AuthFrame();
            }
        });
    }
}