package util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigManager {
    private static final Path CONFIG_PATH = Path.of(AppPaths.CONFIG_FILE);

    public static String loadSkippedVersion() {
        return load().getProperty("skippedVersion", "");
    }

    public static void saveSkippedVersion(String version) {
        Properties props = load();
        props.setProperty("skippedVersion", version);
        save(props);
    }

    public static String loadProperty(String key, String defaultValue) {
        return load().getProperty(key, defaultValue);
    }

    public static boolean isInternetEnabled() {
        return Boolean.parseBoolean(loadProperty("useInternet", "true"));
    }

    private static final String REG_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_REG_NAME = "JavaMultiTool";

    public static boolean isStartupEnabled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query", REG_KEY, "/v", APP_REG_NAME);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes());
            return p.waitFor() == 0 && output.contains(APP_REG_NAME);
        } catch (Exception e) {
            return false;
        }
    }

    public static void setStartup(boolean enabled) {
        try {
            if (enabled) {
                String jarPath = getJarPath();
                if (jarPath == null) {
                    AppLogger.error("ConfigManager: cannot determine JAR path for startup");
                    return;
                }
                new ProcessBuilder("reg", "add", REG_KEY, "/v", APP_REG_NAME,
                        "/t", "REG_SZ", "/d", "javaw -jar \"" + jarPath + "\"", "/f")
                        .start().waitFor();
                AppLogger.info("ConfigManager: startup enabled, path=" + jarPath);
            } else {
                new ProcessBuilder("reg", "delete", REG_KEY, "/v", APP_REG_NAME, "/f")
                        .start().waitFor();
                AppLogger.info("ConfigManager: startup disabled");
            }
        } catch (Exception e) {
            AppLogger.error("ConfigManager: failed to set startup: " + e.getMessage());
        }
    }

    private static String getJarPath() {
        try {
            return new File(ConfigManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    public static void saveProperty(String key, String value) {
        Properties props = load();
        props.setProperty(key, value);
        save(props);
    }

    private static Properties load() {
        Properties props = new Properties();
        if (Files.exists(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);
            } catch (IOException e) {
                AppLogger.error("ConfigManager: failed to load config: " + e.getMessage());
            }
        }
        return props;
    }

    private static void save(Properties props) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                props.store(out, "JavaMultiTool preferences");
            }
        } catch (IOException e) {
            AppLogger.error("ConfigManager: failed to save config: " + e.getMessage());
        }
    }
}
