package db;

import ui.utils.AppLogger;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class WorkflowDB {
    private static final String DB_PATH = System.getProperty("user.home") + "/Documents/MultiTool-Java-data/Databases/workflow.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS tracked_apps (id INTEGER PRIMARY KEY AUTOINCREMENT, app_name TEXT, exe_name TEXT UNIQUE)");
            stmt.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "task_name TEXT UNIQUE, " +
                    "description TEXT, " +
                    "created_at TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS daily_stats (" +
                    "date TEXT, hour INTEGER, item_id INTEGER, type INTEGER, seconds_spent INTEGER, " +
                    "PRIMARY KEY (date, hour, item_id, type))");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addTime(int itemId, int type, int seconds) {
        String today = LocalDate.now().toString();
        int hour = java.time.LocalTime.now().getHour();

        String sql = "INSERT INTO daily_stats(date, hour, item_id, type, seconds_spent) VALUES(?,?,?,?,?) " +
                "ON CONFLICT(date, hour, item_id, type) DO UPDATE SET seconds_spent = seconds_spent + excluded.seconds_spent";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, today);
            pstmt.setInt(2, hour);
            pstmt.setInt(3, itemId);
            pstmt.setInt(4, type);
            pstmt.setInt(5, seconds);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteTrackedAppFromDB(int id) {
        try (Connection conn = WorkflowDB.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tracked_apps WHERE id = ?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addTrackedApp(String appName, String exeName) {
        String sql = "INSERT OR IGNORE INTO tracked_apps(app_name, exe_name) VALUES(?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, appName);
            pstmt.setString(2, exeName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addTask(String name, String desc) {
        String sql = "INSERT OR IGNORE INTO tasks(task_name, description, created_at) VALUES(?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, desc);
            pstmt.setString(3, LocalDate.now().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Object[]> getTrackedAppsFull() {
        List<Object[]> apps = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, app_name, exe_name FROM tracked_apps")) {
            while (rs.next()) {
                apps.add(new Object[]{rs.getInt("id"), rs.getString("app_name"), rs.getString("exe_name")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return apps;
    }

    public static int getSecondsToday(int itemId, int type) {
        String sql = "SELECT SUM(seconds_spent) FROM daily_stats WHERE date = ? AND item_id = ? AND type = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, LocalDate.now().toString());
            pstmt.setInt(2, itemId);
            pstmt.setInt(3, type);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            AppLogger.error("DB Error in getSecondsToday: " + e.getMessage());
        }
        return 0;
    }

    public static void updateAppName(int id, String newName) {
        String sql = "UPDATE tracked_apps SET app_name = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Integer> getDaySummary(String date) {
        Map<String, Integer> data = new LinkedHashMap<>();
        String sql = """
        SELECT COALESCE(ta.app_name, t.task_name) as name, SUM(ds.seconds_spent) as total
        FROM daily_stats ds
        LEFT JOIN tracked_apps ta ON ds.item_id = ta.id AND ds.type = 0
        LEFT JOIN tasks t ON ds.item_id = t.id AND ds.type = 1
        WHERE ds.date = ?
        GROUP BY name ORDER BY total DESC
        """;
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) data.put(rs.getString("name"), rs.getInt("total"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static int[] getHourlyStats(String date, String appName) {
        int[] hours = new int[24];
        String sql = "SELECT hour, SUM(seconds_spent) FROM daily_stats ds " +
                "LEFT JOIN tracked_apps ta ON ds.item_id = ta.id AND ds.type = 0 " +
                "WHERE ds.date = ? " +
                (appName.equals("ALL") ? "" : "AND ta.app_name = ? ") +
                "GROUP BY hour";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date);
            if (!appName.equals("ALL")) pstmt.setString(2, appName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) hours[rs.getInt(1)] = rs.getInt(2);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return hours;
    }

    public static List<String> getAvailableDates() {
        List<String> dates = new ArrayList<>();
        String sql = "SELECT DISTINCT date FROM daily_stats ORDER BY date DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                dates.add(rs.getString("date"));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        if (dates.isEmpty()) {
            dates.add(LocalDate.now().toString());
        }
        return dates;
    }

    public static class StatResult {
        public int[] values;
        public String[] labels;
        public StatResult(int size) {
            values = new int[size];
            labels = new String[size];
        }
    }

    public static StatResult getPeriodStats(String endDateStr, String appName, int days) {
        StatResult result = new StatResult(days);
        LocalDate endDate = LocalDate.parse(endDateStr);

        for (int i = 0; i < days; i++) {
            LocalDate target = endDate.minusDays(days - 1 - i);
            result.labels[i] = target.toString(); // "2023-10-27"

            String sql = "SELECT SUM(seconds_spent) FROM daily_stats ds " +
                    "LEFT JOIN tracked_apps ta ON ds.item_id = ta.id AND ds.type = 0 " +
                    "WHERE ds.date = ? " +
                    (appName.equals("ALL") ? "" : "AND ta.app_name = ? ");

            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, target.toString());
                if (!appName.equals("ALL"))
                    pstmt.setString(2, appName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next())
                    result.values[i] = rs.getInt(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static StatResult getCalendarMonthStats(String dateStr, String appName) {
        LocalDate selectedDate = LocalDate.parse(dateStr);
        int daysInMonth = selectedDate.lengthOfMonth();
        StatResult result = new StatResult(daysInMonth);
        LocalDate firstDay = selectedDate.withDayOfMonth(1);

        for (int i = 0; i < daysInMonth; i++) {
            LocalDate target = firstDay.plusDays(i);
            result.labels[i] = String.valueOf(i + 1);

            String sql = "SELECT SUM(seconds_spent) FROM daily_stats ds " +
                    "LEFT JOIN tracked_apps ta ON ds.item_id = ta.id AND ds.type = 0 " +
                    "WHERE ds.date = ? " +
                    (appName.equals("ALL") ? "" : "AND ta.app_name = ? ");
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, target.toString());
                if (!appName.equals("ALL"))
                    pstmt.setString(2, appName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next())
                    result.values[i] = rs.getInt(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static List<Object[]> getTasksFull() {
        List<Object[]> tasks = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, task_name, description FROM tasks")) {
            while (rs.next()) {
                tasks.add(new Object[]{rs.getInt("id"), rs.getString("task_name"), rs.getString("description")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    public static void deleteTask(int id) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            try (PreparedStatement pstmt2 = conn.prepareStatement("DELETE FROM daily_stats WHERE item_id = ? AND type = 1")) {
                pstmt2.setInt(1, id);
                pstmt2.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}