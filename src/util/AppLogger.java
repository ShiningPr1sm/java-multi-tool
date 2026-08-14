package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

public class AppLogger {
    public static final String ISSUES_URL = "https://github.com/ShiningPr1sm/JavaMultiTool/issues";

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FOLDER_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter TIME_FILE_FORMATTER = DateTimeFormatter.ofPattern("HH_mm_ss");

    private static final List<String> sessionLogs = new ArrayList<>();
    private static Consumer<String> consoleOutput;
    private static Consumer<String> errorHandler;
    private static File currentLogFile;
    private static boolean saveErrorReported;

    public static synchronized void init() {
        if (currentLogFile != null) return;
        compressOldLogs();

        LocalDateTime now = LocalDateTime.now();
        String dateFolder = now.format(DATE_FOLDER_FORMATTER);
        String timeFile = now.format(TIME_FILE_FORMATTER);

        File dayDir = new File(util.AppPaths.LOGS_DIR, dateFolder);
        if (!dayDir.exists() && !dayDir.mkdirs()) {
            System.err.println("[AppLogger] Could not create log directory: " + dayDir);
            return;
        }

        File logFile = new File(dayDir, timeFile + ".txt");
        currentLogFile = logFile;

        StringBuilder header = new StringBuilder();
        header.append("===== JavaMultiTool Log =====\n");
        header.append("Launch time: ").append(now.format(TIMESTAMP_FORMATTER)).append('\n');
        header.append("Version: ").append(VersionInfo.getVersion()).append('\n');
        header.append("OS: ").append(osInfo()).append('\n');
        header.append("CPU: ").append(cpuInfo()).append('\n');
        header.append("GPU: ").append(gpuInfo()).append('\n');
        header.append("If this error prevents the program from working - create an issue at ").append(ISSUES_URL).append('\n');
        header.append("=============================\n");

        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(header);
            saveErrorReported = false;
        } catch (IOException e) {
            reportFileError(e.getMessage());
        }
    }

    private static void compressOldLogs() {
        File logsDir = new File(util.AppPaths.LOGS_DIR);
        File[] dayDirs = logsDir.listFiles(File::isDirectory);
        if (dayDirs == null) return;
        for (File dayDir : dayDirs) {
            File[] files = dayDir.listFiles((dir, name) -> name.endsWith(".txt"));
            if (files == null) continue;
            for (File txt : files) {
                gzipFile(txt);
            }
        }
    }

    private static void gzipFile(File txt) {
        File gz = new File(txt.getParentFile(), txt.getName() + ".gz");
        try (InputStream in = new FileInputStream(txt);
             OutputStream out = new GZIPOutputStream(new FileOutputStream(gz))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            AppLogger.error("Can't compress log file: " + txt.getAbsolutePath());
            return;
        }
        txt.delete();
    }

    private static String osInfo() {
        String name = System.getProperty("os.name", "Unknown");
        String version = System.getProperty("os.version", "");
        String arch = System.getProperty("os.arch", "");
        return name + " " + version + " (" + arch + ")";
    }

    private static String cpuInfo() {
        List<String> wmic = wmicQuery("cpu", "get", "name");
        if (wmic != null && !wmic.isEmpty()) return String.join(", ", wmic);
        String env = System.getenv("PROCESSOR_IDENTIFIER");
        return (env != null && !env.isBlank()) ? env : "Unknown";
    }

    private static String gpuInfo() {
        List<String> active = new ArrayList<>();
        List<String> inactive = new ArrayList<>();
        try {
            Process process = new ProcessBuilder("wmic", "path", "win32_VideoController", "get", "name,CurrentHorizontalResolution")
                    .redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("CurrentHorizontalResolution")) continue;
                    String[] parts = trimmed.split("\\s{2,}");
                    boolean isActive = false;
                    String name;
                    if (parts.length >= 2) {
                        isActive = !parts[0].trim().isEmpty();
                        name = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
                    } else {
                        name = trimmed;
                    }
                    (isActive ? active : inactive).add(name);
                }
            }
            process.waitFor();
        } catch (Exception e) {
            return "Unknown";
        }
        if (active.isEmpty() && inactive.isEmpty()) return "Unknown";
        active.addAll(inactive);
        return String.join("\n", active);
    }

    private static List<String> wmicQuery(String... args) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("wmic");
            cmd.addAll(List.of(args));
            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            List<String> values = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.equalsIgnoreCase(args[args.length - 1])) continue;
                    values.add(trimmed);
                }
            }
            process.waitFor();
            return values.isEmpty() ? null : values;
        } catch (Exception e) {
            return null;
        }
    }

    public static void log(String level, String message) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StackTraceElement caller = stackTrace[3];
        String fileName = caller.getFileName();
        int lineNumber = caller.getLineNumber();

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String logEntry = String.format("[%s] [%s] [%s:%d] %s",
                timestamp, level.toUpperCase(), fileName, lineNumber, message);

        System.out.println(logEntry);
        sessionLogs.add(logEntry);

        saveToFile(logEntry);
        if (consoleOutput != null) {
            consoleOutput.accept(logEntry + "\n");
        }
        if ("ERR".equalsIgnoreCase(level) && errorHandler != null) {
            errorHandler.accept(message);
        }
    }

    private static synchronized void saveToFile(String entry) {
        init();
        if (currentLogFile == null) {
            reportFileError("log file unavailable");
            return;
        }
        try (FileWriter fw = new FileWriter(currentLogFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(entry);
            saveErrorReported = false;
        } catch (IOException e) {
            reportFileError(e.getMessage());
        }
    }

    private static void reportFileError(String details) {
        if (saveErrorReported) return;
        saveErrorReported = true;
        System.err.println("[AppLogger] Could not save log to file: " + details);
    }

    public static void setConsoleOutput(Consumer<String> output) {
        consoleOutput = output;
        for (String oldLog : sessionLogs) {
            output.accept(oldLog + "\n");
        }
    }

    public static void setOnError(Consumer<String> handler) {
        errorHandler = handler;
    }

    public static File getCurrentLogFile() {
        return currentLogFile;
    }

    public static void info(String msg) {
        log("INF", msg);
    }
    public static void error(String msg) {
        log("ERR", msg);
    }
    public static void admin(String msg) {
        log("ADM", msg);
    }
}
