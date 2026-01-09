package ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class UIStyle {
    public static final Color BG_COLOR = new Color(25, 25, 25);
    public static final Color SECONDARY_BG = new Color(35, 35, 35);
    public static final Color BUTTON_BG = new Color(40, 40, 40);
    public static final Color BUTTON_HOVER = new Color(55, 55, 55);
    public static final Color BUTTON_PRESSED = new Color(65, 65, 65);
    public static final Color ACCENT_COLOR = new Color(100, 200, 100);
    public static final Color TEXT_COLOR = Color.WHITE;
    public static final Color BORDER_COLOR = new Color(60, 60, 60);

    public static void styleButton(AbstractButton btn) {
        btn.setOpaque(true);
        btn.setBackground(BUTTON_BG);
        btn.setForeground(TEXT_COLOR);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));

        btn.getModel().addChangeListener(_ -> {
            ButtonModel m = btn.getModel();
            if (m.isPressed()) btn.setBackground(BUTTON_PRESSED);
            else if (m.isRollover()) btn.setBackground(BUTTON_HOVER);
            else btn.setBackground(BUTTON_BG);
        });
    }

    public static void styleScrollBar(JScrollPane sp) {
        sp.getVerticalScrollBar().setUnitIncrement(20);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = BUTTON_BG;
                this.trackColor = BG_COLOR;
            }
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b;
            }
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 8, 8);
                g2.dispose();
            }
        });
        sp.setBorder(null);
        sp.getViewport().setBackground(BG_COLOR);
    }

    public static void styleComboBox(JComboBox<?> cb) {
        cb.setBackground(BUTTON_BG);
        cb.setForeground(TEXT_COLOR);
        cb.setFocusable(false);
        cb.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        cb.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton("▼");
                btn.setBorder(BorderFactory.createEmptyBorder());
                btn.setContentAreaFilled(false);
                btn.setForeground(TEXT_COLOR);
                return btn;
            }
            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(BUTTON_BG);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setOpaque(true);
                lbl.setBackground(isSelected ? BUTTON_HOVER : BUTTON_BG);
                lbl.setForeground(TEXT_COLOR);
                lbl.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                return lbl;
            }
        });
    }
}