package db;

import java.sql.*;

public class DB {
    private static final String DB_PATH = System.getProperty("user.home") + "/Documents/MultiTool-Java-data/Databases/user_data.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    login TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    nickname TEXT DEFAULT '',
                    reg_time TEXT DEFAULT CURRENT_TIMESTAMP,
                    last_login TEXT DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // Добавляем недостающие поля, если таблица уже существовала
            try (Statement alterStmt = conn.createStatement()) {
                alterStmt.execute("ALTER TABLE users ADD COLUMN achievements INTEGER DEFAULT 0;");
            } catch (SQLException ignored) {}

            try (Statement alterStmt = conn.createStatement()) {
                alterStmt.execute("ALTER TABLE users ADD COLUMN save_login INTEGER DEFAULT 0;");
            } catch (SQLException ignored) {}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     *Регистрирует нового пользователя в системе.
     */
    public static boolean register(String login, String password) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (login, password, nickname) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, login);
            stmt.setString(2, password);
            stmt.setString(3, login);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     *Запрос к БД на возврат логина и пароля.
     */
    public static boolean checkLogin(String login, String password) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE login = ? AND password = ?")) {
            stmt.setString(1, login);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     *Возвращает значение ник-нейма пользователя.
     */
    public static String getNickname(String login) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT nickname FROM users WHERE login = ?")) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("nickname");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return login;
    }

    /**
     *Запрос на смену значения для авто-входа.
     */
    public static void setSaveLogin(String login, boolean enabled) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE users SET save_login = ? WHERE login = ?")) {
            stmt.setInt(1, enabled ? 1 : 0);
            stmt.setString(2, login);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     *Запрос к БД для проверки значения для авто-входа.
     */
    public static boolean isSaveLoginEnabled(String login) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT save_login FROM users WHERE login = ?")) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("save_login") == 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     *Запрос к БД для проверки пароля пользователя.
     */
    public static boolean checkPassword(String login, String password) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT password FROM users WHERE login = ?")) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String currentPassword = rs.getString("password");
                return currentPassword.equals(password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     *Обновление пароля пользователя.
     */
    public static void updatePassword(String login, String newPassword) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE users SET password = ? WHERE login = ?")) {
            stmt.setString(1, newPassword);
            stmt.setString(2, login);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     *Возвращает дату регистрации пользователя.
     */
    public static String getRegistrationDate(String login) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT reg_time FROM users WHERE login = ?")) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("reg_time");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    /**
     * Возвращает значение поля last_login для данного пользователя.
     * Если ничего не найдено — возвращает "Unknown".
     */
    public static String getLastLoginDate(String login) {
        String sql = "SELECT last_login FROM users WHERE login = ?";
        String lastLogin = "Unknown";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    lastLogin = rs.getString("last_login");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lastLogin;
    }

    /**
     * Устанавливает поле last_login = CURRENT_TIMESTAMP для данного пользователя.
     * Вызывайте этот метод сразу после успешного логина.
     */
    public static void updateLastLoginDate(String login) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE login = ?";
        //TODO: УСТАНАВЛИВАЕТ ВРЕМЯ ПО ГРИНВИЧУ, А НЕ ПО ЛОКАЛЬНОМУ У ПОЛЬЗОВАТЕЛЯ

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Получает значение 1, если пользователь захотел автовход в программу.
     * Если значение равно 0, то автовход будет выключен.
     */
    public static String getAutoLoginUser() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT login FROM users WHERE save_login = 1 LIMIT 1")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("login");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}