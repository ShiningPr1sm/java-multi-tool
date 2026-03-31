package ui.utils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class PieChartPanel extends JPanel {
    private Map<String, Integer> data;

    private final Color[] PALETTE = {
            new Color(100, 200, 100), // Зеленый/Green
            new Color(194, 0, 255),   // Фиолетовый/Purple
            new Color(0, 200, 255),   // Голубой/Blue
            new Color(255, 140, 0),   // Оранжевый/Orange
            new Color(255, 75, 75),   // Красный/Red
            new Color(255, 215, 0),   // Золотой/Gold
            new Color(64, 224, 208),  // Бирюзовый/Cyan
            new Color(250, 128, 114), // Лососевый/Salmon
            new Color(123, 104, 238), // Индиго/Indigo
            new Color(173, 255, 47),  // Лайм/Lime
            new Color(255, 105, 180), // Розовый/Pink
            new Color(0, 255, 127)    // Весенний зеленый/Summer Green
    };

    public void setData(Map<String, Integer> data) {
        this.data = data;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g2.drawString("Application Usage Distribution", 20, 30);

        if (data == null || data.isEmpty()) {
            drawEmptyMessage(g2);
            return;
        }

        int totalSeconds = data.values().stream().mapToInt(Integer::intValue).sum();
        double curAngle = 0;
        int padding = 60;
        int size = getHeight() - (padding * 2);
        int x = 40;

        int i = 0;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            double angle = (entry.getValue() * 360.0) / totalSeconds;
            Color segmentColor = (i < PALETTE.length) ? PALETTE[i] : Color.getHSBColor((float) i / data.size(), 0.7f, 0.9f);
            g2.setColor(segmentColor);
            g2.fillArc(x, padding, size, size, (int) Math.round(curAngle), (int) Math.round(angle) + 1);

            int legendX = x + size + 40;
            int legendY = padding + 20 + (i * 25);

            if (legendY < getHeight() - 20) {
                g2.fillRect(legendX, legendY, 12, 12);
                g2.setColor(new Color(220, 220, 220));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));

                int seconds = entry.getValue();
                double percent = (seconds * 100.0) / totalSeconds;
                String timeFormatted = formatFullTime(seconds);

                // f: {name} ({per}%/{hh:mm:ss})
                String label = String.format("%s (%.1f%% / %s)", entry.getKey(), percent, timeFormatted);
                g2.drawString(label, legendX + 22, legendY + 11);
            }
            curAngle += angle;
            i++;
        }
    }

    private String formatFullTime(int totalSec) {
        int h = totalSec / 3600;
        int m = (totalSec % 3600) / 60;
        int s = totalSec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void drawEmptyMessage(Graphics2D g2) {
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        String msg = "No data recorded for this day!";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
    }
}