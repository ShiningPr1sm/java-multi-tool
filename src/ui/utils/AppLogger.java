package ui.utils;

import javax.swing.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AppLogger {
    private static final String LOG_PATH = System.getProperty("user.home")
            + "/Documents/MultiTool-Java-data/app_history.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> sessionLogs = new ArrayList<>();
    private static JTextArea consoleOutput;

    public static void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = String.format("[%s] [%s] %s", timestamp, level.toUpperCase(), message);

        System.out.println(logEntry);
        sessionLogs.add(logEntry);
        saveToFile(logEntry);
        if (consoleOutput != null) {
            SwingUtilities.invokeLater(() -> {
                consoleOutput.append(logEntry + "\n");
                consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
            });
        }
    }

    private static void saveToFile(String entry) {
        try (FileWriter fw = new FileWriter(LOG_PATH, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(entry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setConsoleOutput(JTextArea textArea) {
        consoleOutput = textArea;
        for (String oldLog : sessionLogs) {
            textArea.append(oldLog + "\n");
        }
    }

    public static void info(String msg) {
        log("INFO", msg);
    }
    public static void error(String msg) {
        log("ERROR", msg);
    }
    public static void admin(String msg) {
        log("ADMIN", msg);
    }
}