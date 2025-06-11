package ui;

import db.AchievementDB;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class AchievementsPanel extends JPanel {
    public AchievementsPanel(String login) {
        setLayout(new BorderLayout());
        setBackground(new Color(25, 25, 25));

        JPanel grid = new JPanel(new GridLayout(0, 2, 20, 20));
        grid.setBackground(new Color(25, 25, 25));
        grid.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // XP для уровня берём из achievement_levels
        String sql = """
            SELECT a.code,
                   a.title,
                   a.description,
                   al.xp_reward AS xp_reward,
                   ua.level,
                   ua.progress,
                   al.required_progress
              FROM achievements a
              JOIN user_achievements ua
                ON a.code = ua.achievement_code
              LEFT JOIN achievement_levels al
                ON a.code = al.achievement_code
               AND ua.level = al.level
             WHERE ua.user_login = ?
        """;

        try (Connection conn = AchievementDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, login);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("code");
                    String title = rs.getString("title");
                    String description = rs.getString("description");
                    int xpReward = rs.getInt("xp_reward");  // теперь берётся из achievement_levels (или из achievements, если уровневой нет)
                    int level = rs.getInt("level");
                    Integer reqObj = (Integer) rs.getObject("required_progress");

                    int required;
                    int progressVal;
                    if (reqObj == null) {
                        // ачивка выполнена, максимум уровня
                        required    = 1;
                        progressVal = 1;
                    } else {
                        required    = reqObj;
                        progressVal = rs.getInt("progress");
                    }

                    String iconPath = "/icons/achievements/" + code + ".png";

                    // добавляем подпись уровня, если уровней больше одного
                    String displayTitle = title;
                    if (AchievementDB.getMaxAchievementLevel(code) > 0) {
                        displayTitle += " (Level " + level + ")";
                    }

                    grid.add(createAchievementCard(
                            displayTitle,
                            description,
                            progressVal,
                            required,
                            iconPath,
                            xpReward
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JPanel container = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        container.setBackground(new Color(25, 25, 25));
        container.add(grid);

        JScrollPane scrollPane = new JScrollPane(container);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createAchievementCard(String title, String description, int progress, int max, String iconPath, int xpReward) {
        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(380, 110));
        card.setMaximumSize(new Dimension(380, 110));
        boolean completed = progress >= max;
        Color bg = completed ? new Color(60, 120, 60) : new Color(40, 40, 40);
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60), 1, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        // Иконка
        JLabel icon = new JLabel();
        Dimension iconSize = new Dimension(70, 70);
        icon.setPreferredSize(iconSize);
        icon.setOpaque(true);
        icon.setBackground(Color.DARK_GRAY);
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setVerticalAlignment(SwingConstants.CENTER);

        ImageIcon imageIcon = new ImageIcon(
                Objects.requireNonNullElseGet(
                        getClass().getResource(iconPath),
                        () -> getClass().getResource("/icons/achievements/no_achievement.png")
                )
        );
        Image scaled = imageIcon.getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH);
        icon.setIcon(new ImageIcon(scaled));

        JPanel iconWrapper = new JPanel(new BorderLayout());
        iconWrapper.setPreferredSize(new Dimension(80, 80));
        iconWrapper.setBackground(bg);
        iconWrapper.add(icon, BorderLayout.CENTER);

        // Инфо
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(bg);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));

        JLabel xpLabel = new JLabel("+" + xpReward + " XP");
        xpLabel.setForeground(new Color(180, 255, 180));
        xpLabel.setFont(xpLabel.getFont().deriveFont(12f));

        JLabel descLabel = new JLabel(
                "<html><body style='width:200px'>" + description + "</body></html>");
        descLabel.setForeground(Color.LIGHT_GRAY);
        descLabel.setFont(descLabel.getFont().deriveFont(11f));

        JProgressBar progressBar = new JProgressBar(0, max) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(d.width, 16);
            }
        };
        progressBar.setValue(progress);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(100, 150, 100));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(titleLabel);
        info.add(xpLabel);
        info.add(Box.createVerticalStrut(5));
        info.add(descLabel);
        info.add(Box.createVerticalStrut(10));
        info.add(progressBar);

        card.add(iconWrapper, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);

        return card;
    }
}
