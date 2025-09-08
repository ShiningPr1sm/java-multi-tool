package ui.daytab;

import db.BDaysDB;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DateFormatter;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.Locale;

public class BDaysNotifierPanel extends JPanel {
    private static final String EDIT_CARD = "EDIT";
    private static final String OVERVIEW_CARD = "OVERVIEW";
    private static final DateTimeFormatter STORAGE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private DefaultTableModel model;
    private JTable table;
    private JPanel cards;
    private JComboBox<String> modeSelector;
    private JPanel overviewContainer;

    public BDaysNotifierPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(25, 25, 25));

        // Top panel
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setBackground(new Color(25, 25, 25));

        // Mode selector
        modeSelector = new JComboBox<>(new String[]{"Upcoming", "List", "Reverse List"});
        styleTabButton(modeSelector);
        top.add(modeSelector);

        // Overview/Edit buttons
        JButton overviewBtn = new JButton("Overview");
        JButton editBtn     = new JButton("Edit");
        styleTabButton(overviewBtn);
        styleTabButton(editBtn);
        top.add(overviewBtn);
        top.add(editBtn);
        add(top, BorderLayout.NORTH);

        // Cards
        cards = new JPanel(new CardLayout());
        cards.setBackground(new Color(25, 25, 25));
        cards.add(createEditPanel(), EDIT_CARD);
        cards.add(createOverviewPanel(), OVERVIEW_CARD);
        add(cards, BorderLayout.CENTER);

        // Listeners
        overviewBtn.addActionListener(e -> switchCard(OVERVIEW_CARD));
        editBtn.addActionListener(e -> switchCard(EDIT_CARD));
        modeSelector.addActionListener(e -> {
            if (OVERVIEW_CARD.equals(getCurrentCard())) {
                refreshOverview();
            }
        });

        // Default view
        switchCard(OVERVIEW_CARD);
    }

    private void switchCard(String card) {
        CardLayout cl = (CardLayout) cards.getLayout();
        if (OVERVIEW_CARD.equals(card)) {
            refreshOverview();
        } else {
            refreshTable();
        }
        cl.show(cards, card);
    }

    private String getCurrentCard() {
        for (Component comp : cards.getComponents()) {
            if (comp.isVisible()) {
                return ((JComponent) comp).getName();
            }
        }
        return EDIT_CARD;
    }

    private JPanel createEditPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setName(EDIT_CARD);
        panel.setBackground(new Color(25, 25, 25));

        // Table setup
        model = new DefaultTableModel(new String[]{"ID", "Name", "Birthday"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0; // только ID нельзя редактировать
            }
        };
        model.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int id = (int) model.getValueAt(row, 0);
                String name = model.getValueAt(row, 1).toString();
                String dateStr = model.getValueAt(row, 2).toString();
                try {
                    LocalDate date = LocalDate.parse(dateStr, DISPLAY_FORMAT);
                    BDaysDB.updateBirthday(id, name, date); // создаёшь этот метод
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Invalid date format", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setBackground(new Color(25, 25, 25));
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(60, 60, 60));
        table.setFont(table.getFont().deriveFont(15f));

        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                                                           boolean isSel, boolean hasFocus,
                                                           int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, isSel, hasFocus, row, col);
                lbl.setBackground(new Color(40, 40, 40));
                lbl.setForeground(Color.WHITE);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                return lbl;
            }
        });
        header.setOpaque(true);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        scroll.getViewport().setBackground(new Color(25, 25, 25));
        panel.add(scroll, BorderLayout.CENTER);

        // Form panel
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setBackground(new Color(30, 30, 30));
        JTextField nameField = new JTextField(10);
        JFormattedTextField dateField = new JFormattedTextField(new DateFormatter(new SimpleDateFormat("dd.MM.yyyy")));
        dateField.setColumns(8);
        JButton addBtn = new JButton("Add");
        JButton removeBtn = new JButton("Remove");
        addBtn.addActionListener(e -> {
            try {
                LocalDate d = LocalDate.parse(dateField.getText(), DISPLAY_FORMAT);
                BDaysDB.addBirthday(nameField.getText(), d);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format, use DD.MM.YYYY", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        removeBtn.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel >= 0) {
                int id = (int) model.getValueAt(sel, 0);
                BDaysDB.removeBirthday(id);
                refreshTable();
            }
        });
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(Color.WHITE);
        form.add(nameLabel);
        form.add(nameField);
        JLabel dateLabel = new JLabel("Date (dd.MM.yyyy):");
        dateLabel.setForeground(Color.WHITE);
        form.add(dateLabel);
        form.add(dateField);
        form.add(addBtn); form.add(removeBtn);
        panel.add(form, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setName(OVERVIEW_CARD);
        panel.setBackground(new Color(25, 25, 25));

        overviewContainer = new JPanel();
        overviewContainer.setLayout(new BoxLayout(overviewContainer, BoxLayout.Y_AXIS));
        overviewContainer.setBackground(new Color(25, 25, 25));

        JScrollPane sp = new JScrollPane(overviewContainer);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setViewportBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setBorder(null);
        sp.getHorizontalScrollBar().setBorder(null);
        sp.getViewport().setBackground(new Color(25, 25, 25));
        panel.add(sp, BorderLayout.CENTER);
        sp.setBorder(null);
        sp.setViewportBorder(null);
        sp.getViewport().setBackground(new Color(25, 25, 25));
        panel.add(sp, BorderLayout.CENTER);

        return panel;
    }

    private void refreshTable() {
        model.setRowCount(0);
        try (var rs = BDaysDB.getAllBirthdays()) {
            while (rs.next()) {
                LocalDate d = LocalDate.parse(rs.getString("bday_date"), STORAGE_FORMAT);
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("name"), d.format(DISPLAY_FORMAT)});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshOverview() {
        overviewContainer.removeAll();
        LocalDate today = LocalDate.now();
        String mode = (String) modeSelector.getSelectedItem();
        boolean upcoming = "Upcoming".equals(mode);

        Map<String, Map<Integer, List<Object[]>>> grouping = new LinkedHashMap<>();
        String[] seasons = {"Spring", "Summer", "Autumn", "Winter"};
        for (String s : seasons) grouping.put(s, new LinkedHashMap<>());

        try (var rs = BDaysDB.getAllBirthdays()) {
            while (rs.next()) {
                String name = rs.getString("name");
                LocalDate bd = LocalDate.parse(rs.getString("bday_date"), STORAGE_FORMAT);
                LocalDate candidate = bd.withYear(today.getYear());
                boolean expired = upcoming && candidate.isBefore(today);

                int m = candidate.getMonthValue();
                String season;
                if (m >= 3 && m <= 5) season = "Spring";
                else if (m >= 6 && m <= 8) season = "Summer";
                else if (m >= 9 && m <= 11) season = "Autumn";
                else season = "Winter";

                grouping.get(season)
                        .computeIfAbsent(m, k -> new ArrayList<>())
                        .add(new Object[]{name, bd, candidate, expired});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String season : seasons) {
            var months = grouping.get(season);
            if (months.isEmpty()) continue;

            Color seasonColor = switch (season) {
                case "Winter" -> new Color(64, 224, 208);
                case "Spring" -> new Color(100, 200, 100);
                case "Summer" -> new Color(255, 215, 0);
                case "Autumn" -> new Color(255, 140, 0);
                default -> Color.WHITE;
            };

            JLabel sl = new JLabel("   " + season);
            sl.setForeground(seasonColor);
            sl.setFont(sl.getFont().deriveFont(Font.BOLD, 16f));
            overviewContainer.add(sl);

            List<Integer> mks = new ArrayList<>(months.keySet());
            Collections.sort(mks);

            for (int mk : mks) {
                String mn = Month.of(mk).getDisplayName(TextStyle.FULL, Locale.getDefault()).toUpperCase();
                JLabel ml = new JLabel("      " + mn);
                ml.setForeground(seasonColor);
                ml.setFont(ml.getFont().deriveFont(Font.BOLD, 14f));
                overviewContainer.add(ml);

                List<Object[]> entries = new ArrayList<>(months.get(mk));
                entries.sort(Comparator.comparing(o -> (LocalDate) o[2])); // sort by candidate
                if ("Reverse List".equals(mode)) Collections.reverse(entries);

                for (Object[] entry : entries) {
                    String name = (String) entry[0];
                    LocalDate originalBD = (LocalDate) entry[1];
                    LocalDate candidate = (LocalDate) entry[2];
                    boolean expired = (boolean) entry[3];

                    int age = originalBD.until(today).getYears();
                    String ageText = expired ? "(already " + age + " y.o.)" : "(will be " + age + " y.o.)";

                    JLabel lbl = new JLabel("     — " + name + " — " + candidate.format(DISPLAY_FORMAT) + " " + ageText);
                    lbl.setFont(lbl.getFont().deriveFont(15f));
                    lbl.setForeground(upcoming && expired ? Color.GRAY : Color.WHITE);
                    overviewContainer.add(lbl);
                }
            }
        }

        overviewContainer.revalidate();
        overviewContainer.repaint();
    }


    private void styleTabButton(JComponent comp) {
        comp.setOpaque(true);
        comp.setBackground(new Color(40, 40, 40));
        comp.setForeground(Color.WHITE);
        if (comp instanceof JComboBox) {
            @SuppressWarnings("unchecked")
            JComboBox<String> cb = (JComboBox<String>) comp;
            cb.setBorder(BorderFactory.createEmptyBorder());
            cb.setFocusable(false);
            cb.setBackground(new Color(40, 40, 40));
            cb.setForeground(Color.WHITE);
            cb.setLightWeightPopupEnabled(true);
            cb.setUI(new BasicComboBoxUI() {
                @Override
                protected JButton createArrowButton() {
                    JButton btn = new JButton("▼");
                    btn.setBorder(BorderFactory.createEmptyBorder());
                    btn.setContentAreaFilled(false);
                    btn.setFocusPainted(false);
                    btn.setBackground(new Color(40, 40, 40));
                    btn.setForeground(Color.WHITE);
                    btn.setFont(btn.getFont().deriveFont(Font.PLAIN, 12f));
                    return btn;
                }

                @Override
                public void installDefaults() {
                    super.installDefaults();
                    comboBox.setBorder(BorderFactory.createEmptyBorder());
                    comboBox.setBackground(new Color(40, 40, 40));
                }

                @Override
                public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                    g.setColor(new Color(40, 40, 40));
                    g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                }
            });
            cb.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    lbl.setOpaque(true);
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    lbl.setBackground(isSelected ? new Color(60, 60, 60) : new Color(40, 40, 40));
                    lbl.setForeground(Color.WHITE);
                    lbl.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                    return lbl;
                }
            });
        } else if (comp instanceof AbstractButton) {
            AbstractButton btn = (AbstractButton) comp;
            btn.setOpaque(true);
            btn.setBackground(new Color(40, 40, 40));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            // leave content area filled for proper button background
            btn.getModel().addChangeListener(e -> {
                ButtonModel m = btn.getModel();
                if (m.isPressed()) btn.setBackground(new Color(60, 60, 60));
                else if (m.isRollover()) btn.setBackground(new Color(55, 55, 55));
                else btn.setBackground(new Color(40, 40, 40));
            });
        }
    }
}
