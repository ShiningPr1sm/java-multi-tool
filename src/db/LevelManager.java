// === ФАЙЛ: db/LevelManager.java ===
package db;

import java.sql.*;

public class LevelManager {
    private static final String DB_PATH = System.getProperty("user.home") + "/Documents/MultiTool-Java-data/Databases/levels.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_levels (
                    user_login TEXT PRIMARY KEY,
                    xp INTEGER NOT NULL DEFAULT 0
                );
            """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void ensureUserEntry(String login) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT OR IGNORE INTO user_levels (user_login, xp) VALUES (?, 0)")) {
            stmt.setString(1, login);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int getXP(String login) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT xp FROM user_levels WHERE user_login = ?")) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("xp");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getLevel(String login) {
        return calculateLevel(getXP(login));
    }

    public static void addXP(String login, int amount) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE user_levels SET xp = xp + ? WHERE user_login = ?")) {
            stmt.setInt(1, amount);
            stmt.setString(2, login);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int calculateLevel(int xp) {
        // Примерная система: каждый уровень требует +50 XP больше
        int level = 0;
        int threshold = 100;
        while (xp >= threshold) {
            xp -= threshold;
            threshold += 50;
            level++;
        }
        return level;
    }
}
