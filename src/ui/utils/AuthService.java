package ui.utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;

public class AuthService {
    // Твои секретные хэши (сгенерируй их сам, ниже напишу как)
    private static final String HASH_ADMIN = "qTT3k5tq6r56wTLec/QmvVSIGsaYMof9MltnvpJG78k=";
    private static final String HASH_TESTER = "tSd5GB9MtC/BV8pY5kMlLuAQHQDjU/17Z6Cmimd7ADU=";

    public enum Role { USER, TESTER, ADMIN }

    private static Role currentRole = Role.USER;

    // Путь к файлу прав (там же, где твои базы данных)
    private static final String RIGHTS_FILE_PATH = System.getProperty("user.home")
            + "/Documents/MultiTool-Java-data/rights.ini";

    /**
     * Вызывается один раз при старте программы
     */
    public static void initializeRights() {
        File file = new File(RIGHTS_FILE_PATH);
        if (!file.exists()) {
            currentRole = Role.USER;
            return;
        }

        try {
            // Читаем ключ из файла (первую строку, убираем пробелы)
            String key = Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
            String inputHash = hash(key);

            if (HASH_ADMIN.equals(inputHash)) {
                currentRole = Role.ADMIN;
                System.out.println("System: Logged in as ADMIN");
            } else if (HASH_TESTER.equals(inputHash)) {
                currentRole = Role.TESTER;
                System.out.println("System: Logged in as TESTER");
            } else {
                currentRole = Role.USER;
            }
        } catch (Exception e) {
            currentRole = Role.USER;
        }
    }

    public static Role getRole() { return currentRole; }
    public static boolean isAdmin() { return currentRole == Role.ADMIN; }
    public static boolean isTester() { return currentRole == Role.TESTER || currentRole == Role.ADMIN; }

    public static String hash(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}