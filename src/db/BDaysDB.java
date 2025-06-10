package db;

import java.sql.*;
import java.time.LocalDate;

public class BDaysDB {
    private static final String DB_PATH = System.getProperty("user.home")
            + "/Documents/MultiTool-Java-data/Databases/bdays.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    /** Создаём таблицу, если её нет */
    public static void initializeDatabase() {
        String sql = """
            CREATE TABLE IF NOT EXISTS birthdays (
              id         INTEGER PRIMARY KEY AUTOINCREMENT,
              name       TEXT    NOT NULL,
              bday_date  TEXT    NOT NULL  -- храним в ISO-формате, например 2004-05-02
            );
            """;
        try (Connection conn = getConnection();
             Statement  st  = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Возвращает все записи в виде ResultSet или мапы */
    public static ResultSet getAllBirthdays() throws SQLException {
        Connection conn = getConnection();
        PreparedStatement st = conn.prepareStatement(
                "SELECT id, name, bday_date FROM birthdays ORDER BY bday_date");
        return st.executeQuery();
    }

    public static void addBirthday(String name, LocalDate date) {
        String sql = "INSERT INTO birthdays(name,bday_date) VALUES(?,?)";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, name);
            st.setString(2, date.toString());
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeBirthday(int id) {
        String sql = "DELETE FROM birthdays WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, id);
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
