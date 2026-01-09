package ui.photovideotab;

import ui.UIStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MediaDownloaderPanel extends JPanel {
    // Константы путей
    private static final String APPDATA = System.getenv("APPDATA");
    private static final File YTDLP_DIR = new File(APPDATA, "yt-dlp-app");
    private static final File YTDLP_EXE = new File(YTDLP_DIR, "yt-dlp.exe");
    private static final File YTDLP_VERSION_FILE = new File(YTDLP_DIR, "version.txt");
    private static final String YTDLP_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe";
    private static final File FFMPEG_DIR = new File(YTDLP_DIR, "ffmpeg");
    private static final File FFMPEG_EXE = new File(FFMPEG_DIR, "ffmpeg.exe");
    private static final String FFMPEG_ZIP_URL = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";

    private JTextArea textArea;
    private JProgressBar progressBar;
    private JComboBox<String> formatBox;
    private JComboBox<String> browserComboBox;
    private JButton downloadButton;
    private final File downloadFolder;

    public MediaDownloaderPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(25, 25, 25));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        downloadFolder = new File(System.getProperty("user.home"), "Downloads/JavaVideoDownloader");
        if (!downloadFolder.exists()) downloadFolder.mkdirs();

        initUI();
    }

    private void initUI() {
        JPanel centralPanel = new JPanel(new GridBagLayout());
        centralPanel.setBackground(new Color(25, 25, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 50, 10, 50);

        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(new Color(35, 35, 35));
        textArea.setForeground(Color.GRAY);
        textArea.setCaretColor(Color.WHITE);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textArea.setText("Paste or Enter links to social media here...");

        textArea.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (textArea.getText().equals("Paste or Enter links to social media here...")) {
                    textArea.setText("");
                    textArea.setForeground(Color.WHITE);
                }
            }
            public void focusLost(FocusEvent e) {
                if (textArea.getText().isEmpty()) {
                    textArea.setForeground(Color.GRAY);
                    textArea.setText("Paste or Enter links to social media here...");
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(450, 150)); // Фиксированная высота ввода
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        UIStyle.styleScrollBar(scrollPane);
        centralPanel.add(scrollPane, gbc);

        // 2. Прогресс-бар
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(450, 25)); // Фиксированная высота бара
        progressBar.setBackground(new Color(35, 35, 35));
        progressBar.setForeground(new Color(100, 200, 100));
        progressBar.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        centralPanel.add(progressBar, gbc);

        // 3. Панель управления (Кнопки и выбор формата)
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        controls.setBackground(new Color(25, 25, 25));

        formatBox = new JComboBox<>(new String[]{
                "(dual) Video+Audio",
                "TikTok/Insta/X",
                "Video only",
                "Audio Only"
        });
        UIStyle.styleComboBox(formatBox);
        formatBox.setPreferredSize(new Dimension(150, 30));

        downloadButton = new JButton("Download");
        styleTabButton(downloadButton); // ПРИМЕНЯЕМ ОБНОВЛЕННЫЙ СТИЛЬ
        downloadButton.setPreferredSize(new Dimension(120, 30));
        downloadButton.addActionListener(e -> startDownloadTask());

        browserComboBox = new JComboBox<>(new String[]{"None", "Firefox", "Chrome", "Edge", "Opera", "Brave"});
        UIStyle.styleComboBox(browserComboBox);
        browserComboBox.setPreferredSize(new Dimension(100, 30));

        controls.add(formatBox);
        controls.add(downloadButton);
        controls.add(browserComboBox);

        centralPanel.add(controls, gbc);

        // Добавляем центральную панель в основной контейнер
        add(centralPanel, BorderLayout.CENTER);
    }

    private void startDownloadTask() {
        String input = textArea.getText().trim();
        if (input.isEmpty() || input.startsWith("Paste or Enter")) {
            JOptionPane.showMessageDialog(this, "Please enter at least one video URL!");
            return;
        }

        downloadButton.setEnabled(false);
        new Thread(() -> {
            try {
                checkAndDownloadYTDLP();
                checkAndDownloadFFMPEG();

                String[] urls = input.split("\\r?\\n");
                String format = (String) formatBox.getSelectedItem();
                String browser = browserComboBox.getSelectedItem().toString().toLowerCase();

                for (int i = 0; i < urls.length; i++) {
                    String url = urls[i].trim();
                    if (url.isEmpty()) continue;

                    executeYtDlp(url, format, browser, i + 1, urls.length);
                }

                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(100);
                    progressBar.setString("All downloads completed!");
                    downloadButton.setEnabled(true);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    downloadButton.setEnabled(true);
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                });
            }
        }).start();
    }

    private void executeYtDlp(String videoUrl, String selectedFormat, String browser, int current, int total) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(YTDLP_EXE.getAbsolutePath());
        command.add("--remote-components");
        command.add("ejs:github");

        switch (selectedFormat) {
            case "(dual) Video+Audio/YT music" -> {
                command.add("-f"); command.add("bestvideo[ext=mp4]+bestaudio[ext=m4a]");
                command.add("--merge-output-format"); command.add("mp4");
                command.add("--ffmpeg-location"); command.add(FFMPEG_EXE.getAbsolutePath());
            }
            case "Video only" -> {
                command.add("-f"); command.add("bestvideo[ext=mp4]");
            }
            case "Audio/YT music" -> {
                command.add("-f"); command.add("bestaudio");
                command.add("--extract-audio"); command.add("--audio-format"); command.add("mp3");
                command.add("--ffmpeg-location"); command.add(FFMPEG_EXE.getAbsolutePath());
            }
            case "TikTok, Instagram, X.com" -> {
                command.add("-f"); command.add("best[ext=mp4]");
            }
        }

        if (!browser.equals("none")) {
            command.add("--cookies-from-browser");
            command.add(browser);
        }

        command.add("--impersonate"); command.add("chrome");
        command.add("-o"); command.add(downloadFolder.getAbsolutePath() + "/%(title)s.%(ext)s");
        command.add(videoUrl);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        Pattern pattern = Pattern.compile("(\\d{1,3}\\.\\d)%");

        while ((line = reader.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                int progress = (int) Float.parseFloat(matcher.group(1));
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(progress);
                    progressBar.setString("Video " + current + "/" + total + " - " + progress + "%");
                });
            }
        }
        process.waitFor();
    }

    // --- МЕТОДЫ СТИЛИЗАЦИИ (копируем из BDaysNotifierPanel) ---

    private void styleTabButton(JButton btn) {
        btn.setOpaque(true);
        btn.setBackground(new Color(40, 40, 40));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false); // Убираем стандартную рамку
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));

        btn.getModel().addChangeListener(e -> {
            ButtonModel m = btn.getModel();
            if (m.isPressed()) btn.setBackground(new Color(60, 60, 60));
            else if (m.isRollover()) btn.setBackground(new Color(55, 55, 55));
            else btn.setBackground(new Color(40, 40, 40));
        });
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ СКАЧИВАНИЯ (yt-dlp / ffmpeg) ---
    // (Оставляем твою логику без изменений, просто убираем static где не нужно)

    private void checkAndDownloadYTDLP() throws IOException, InterruptedException {
        if (!YTDLP_DIR.exists()) YTDLP_DIR.mkdirs();
        String latestVersion = getLatestYtDlpVersion();
        if (!YTDLP_EXE.exists() || latestVersion != null) {
            // Здесь можно добавить проверку версий как в твоем коде
            try (InputStream in = new URL(YTDLP_URL).openStream();
                 FileOutputStream out = new FileOutputStream(YTDLP_EXE)) {
                in.transferTo(out);
            }
            YTDLP_EXE.setExecutable(true);
        }
    }

    private String getLatestYtDlpVersion() {
        try (InputStream in = new URL("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest").openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder res = new StringBuilder();
            String l; while((l = reader.readLine()) != null) res.append(l);
            Matcher m = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").matcher(res.toString());
            return m.find() ? m.group(1) : null;
        } catch (Exception e) { return null; }
    }

    private void checkAndDownloadFFMPEG() throws IOException {
        if (!FFMPEG_EXE.exists()) {
            if (!FFMPEG_DIR.exists()) FFMPEG_DIR.mkdirs();
            File zip = new File(FFMPEG_DIR, "ffmpeg.zip");
            try (InputStream in = new URL(FFMPEG_ZIP_URL).openStream();
                 FileOutputStream out = new FileOutputStream(zip)) {
                in.transferTo(out);
            }
            extractFfmpegFromZip(zip, FFMPEG_EXE);
            zip.delete();
        }
    }

    private void extractFfmpegFromZip(File zipFile, File outFile) throws IOException {
        try (ZipFile zf = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (!e.isDirectory() && e.getName().toLowerCase().endsWith("ffmpeg.exe")) {
                    try (InputStream is = zf.getInputStream(e);
                         FileOutputStream fos = new FileOutputStream(outFile)) {
                        is.transferTo(fos);
                    }
                    return;
                }
            }
        }
    }
}