import db.DB;
import ui.AuthFrame;
import ui.MainFrame;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Инициализация базы данных
        DB.initializeDatabase();

        // Автовход, если пользователь сохранил логин
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
