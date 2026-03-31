package ui.daytab;

import db.WorkflowDB;
import ui.UIStyle;
import ui.utils.BarChartPanel;
import ui.utils.PieChartPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class WorkflowPanel extends JPanel {
    private int activeTaskId = -1;
    private String activeTaskName = null;

    private DefaultListModel<String> appListModel;
    private JList<String> appList;
    private long lastTickTime = System.currentTimeMillis();

    private JPanel taskContainer;

    private JComboBox<String> dateSelector;
    private PieChartPanel pieChart;
    private BarChartPanel hourChart, weekChart, monthChart;

    public WorkflowPanel() {
        setLayout(new BorderLayout());
        setBackground(UIStyle.BG_COLOR);

        JTabbedPane tabs = new JTabbedPane();
        UIStyle.styleTabbedPane(tabs);

        tabs.addTab(" Tracker ", createTrackerUI());
        tabs.addTab(" Overview ", createOverviewUI());
        tabs.addTab(" Edit ", createEditUI());

        add(tabs, BorderLayout.CENTER);

        Timer globalTimer = new Timer(5000, _ -> runTrackingCycle());
        globalTimer.start();

        loadDataFromDB();
        loadTasksFromDB();
    }

    private JPanel createTrackerUI() {
        JPanel main = new JPanel(new GridLayout(1, 2, 20, 0));
        main.setBackground(UIStyle.BG_COLOR);
        main.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        main.add(createAppTrackerPanel());
        main.add(createTaskTrackerPanel());

        return main;
    }

    private JPanel createOverviewUI() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIStyle.BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        top.setBackground(UIStyle.BG_COLOR);

        dateSelector = new JComboBox<>();
        UIStyle.styleComboBox(dateSelector);
        refreshDateSelector();
        dateSelector.setPreferredSize(new Dimension(120, 30));

        JComboBox<String> appFilter = new JComboBox<>();
        appFilter.addItem("ALL");
        UIStyle.styleComboBox(appFilter);

        JButton loadBtn = new JButton("Show Stats");
        UIStyle.styleButton(loadBtn);
        loadBtn.addActionListener(_ -> {
            String selectedDate = (String) dateSelector.getSelectedItem();
            String selectedApp = (String) appFilter.getSelectedItem();
            if (selectedDate != null) {
                updateCharts(selectedDate, selectedApp);
            }
        });

        List<Object[]> apps = WorkflowDB.getTrackedAppsFull();
        for (Object[] app : apps)
            appFilter.addItem((String) app[1]);
        top.add(new JLabel("<html><b style='color:white'>Date:</b></html>"));
        top.add(dateSelector);
        top.add(new JLabel("<html><b style='color:white'>App:</b></html>"));
        top.add(appFilter);
        top.add(loadBtn);

        JPanel chartsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        chartsPanel.setBackground(UIStyle.BG_COLOR);

        pieChart = new PieChartPanel();
        pieChart.setBackground(UIStyle.SECONDARY_BG);
        hourChart = new BarChartPanel();
        hourChart.setBackground(UIStyle.SECONDARY_BG);
        weekChart = new BarChartPanel();
        weekChart.setBackground(UIStyle.SECONDARY_BG);
        monthChart = new BarChartPanel();
        monthChart.setBackground(UIStyle.SECONDARY_BG);

        chartsPanel.add(pieChart);
        chartsPanel.add(hourChart);
        chartsPanel.add(weekChart);
        chartsPanel.add(monthChart);

        panel.add(top, BorderLayout.NORTH);
        panel.add(chartsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createEditUI() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIStyle.BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] columns = {"EXE Name", "Display Name", "ID"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 1; }
        };

        JTable editTable = new JTable(model);
        editTable.setBackground(UIStyle.SECONDARY_BG);
        editTable.setForeground(Color.WHITE);
        editTable.setGridColor(UIStyle.BORDER_COLOR);
        editTable.setRowHeight(30);

        JTableHeader header = editTable.getTableHeader();
        header.setBackground(UIStyle.BG_COLOR);
        header.setForeground(Color.GRAY);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                lbl.setBackground(UIStyle.BG_COLOR);
                lbl.setForeground(Color.GRAY);
                lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, UIStyle.BORDER_COLOR));
                return lbl;
            }
        });

        editTable.removeColumn(editTable.getColumnModel().getColumn(2));

        model.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                WorkflowDB.updateAppName((int) model.getValueAt(row, 2), (String) model.getValueAt(row, 1));
                loadDataFromDB();
            }
        });

        JButton refreshBtn = new JButton("Refresh List");
        UIStyle.styleButton(refreshBtn);
        refreshBtn.addActionListener(_ -> {
            model.setRowCount(0);
            List<Object[]> apps = WorkflowDB.getTrackedAppsFull();
            for (Object[] a : apps)
                model.addRow(new Object[]{a[2], a[1], a[0]});
        });

        JButton deleteBtn = new JButton("Delete Selected");
        UIStyle.styleButton(deleteBtn);
        deleteBtn.setForeground(new Color(255, 100, 100));
        deleteBtn.addActionListener(_ -> {
            int row = editTable.getSelectedRow();
            if (row != -1) {
                WorkflowDB.deleteTrackedAppFromDB((int) model.getValueAt(row, 2));
                model.removeRow(row);
                loadDataFromDB();
            }
        });

        JScrollPane sp = new JScrollPane(editTable);
        UIStyle.styleScrollBar(sp);
        sp.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(UIStyle.BG_COLOR);
        btnPanel.add(deleteBtn); btnPanel.add(refreshBtn);

        panel.add(new JLabel("<html><body style='width: 300px; color: white;'>Double-click on 'Display Name' to rename an application.</body></html>"), BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        refreshBtn.doClick();
        return panel;
    }

    private JPanel createAppTrackerPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIStyle.BG_COLOR);

        JLabel title = new JLabel("App Tracker | " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        title.setForeground(UIStyle.ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        appListModel = new DefaultListModel<>();
        appList = new JList<>(appListModel);
        appList.setBackground(UIStyle.SECONDARY_BG);
        appList.setForeground(Color.WHITE);
        appList.setFixedCellHeight(30);

        JScrollPane sp = new JScrollPane(appList);
        sp.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        UIStyle.styleScrollBar(sp);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        btnPanel.setBackground(UIStyle.BG_COLOR);

        JButton addManualBtn = new JButton("Browse EXE");
        UIStyle.styleButton(addManualBtn);
        addManualBtn.addActionListener(_ -> {
            FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this), "Select EXE", FileDialog.LOAD);
            fd.setFile("*.exe");
            fd.setVisible(true);
            if (fd.getFile() != null) {
                String name = fd.getFile();
                String pretty = name.replace(".exe", "");
                pretty = pretty.substring(0,1).toUpperCase() + pretty.substring(1).toLowerCase();
                WorkflowDB.addTrackedApp(pretty, name);
                loadDataFromDB();
            }
        });

        JButton addRunningBtn = new JButton("Add Running");
        UIStyle.styleButton(addRunningBtn);
        addRunningBtn.addActionListener(_ -> showRunningAppsSelector());

        btnPanel.add(addManualBtn); btnPanel.add(addRunningBtn);

        panel.add(title, BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTaskTrackerPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIStyle.BG_COLOR);

        JLabel title = new JLabel("Task Worklog (Manual)");
        title.setForeground(UIStyle.ACCENT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        taskContainer = new JPanel();
        taskContainer.setLayout(new BoxLayout(taskContainer, BoxLayout.Y_AXIS));
        taskContainer.setBackground(UIStyle.BG_COLOR);

        JScrollPane sp = new JScrollPane(taskContainer);
        sp.setBorder(null);
        UIStyle.styleScrollBar(sp);

        JPanel addForm = new JPanel(new GridBagLayout());
        addForm.setBackground(UIStyle.HEADER_COLOR);
        addForm.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);

        JTextField nameInp = new JTextField();
        nameInp.setBackground(UIStyle.SIDE_BOX);
        nameInp.setForeground(Color.WHITE);
        nameInp.setCaretColor(Color.WHITE);
        nameInp.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));

        JTextField descInp = new JTextField();
        descInp.setBackground(UIStyle.SIDE_BOX);
        descInp.setForeground(Color.WHITE);
        descInp.setCaretColor(Color.WHITE);
        descInp.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));

        JButton addBtn = new JButton("ADD NEW TASK");
        UIStyle.styleButton(addBtn);
        addBtn.addActionListener(_ -> {
            if (!nameInp.getText().trim().isEmpty()) {
                WorkflowDB.addTask(nameInp.getText().trim(), descInp.getText().trim());
                nameInp.setText(""); descInp.setText("");
                loadTasksFromDB();
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        addForm.add(new JLabel("<html><b style='color:gray'>Task Name:</b></html>"), gbc);
        gbc.gridy = 1;
        addForm.add(nameInp, gbc);
        gbc.gridy = 2;
        addForm.add(new JLabel("<html><b style='color:gray'>Description:</b></html>"), gbc);
        gbc.gridy = 3;
        addForm.add(descInp, gbc);
        gbc.gridy = 4;
        gbc.insets = new Insets(10, 2, 2, 2);
        addForm.add(addBtn, gbc);

        panel.add(title, BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);
        panel.add(addForm, BorderLayout.SOUTH);

        return panel;
    }

    private void runTrackingCycle() {
        long currentTime = System.currentTimeMillis();
        int secondsSinceLastTick = (int) ((currentTime - lastTickTime) / 1000);
        lastTickTime = currentTime;
        if (secondsSinceLastTick <= 0) return;

        List<Object[]> appsToTrack = WorkflowDB.getTrackedAppsFull();
        java.util.Set<String> runningExes = ProcessHandle.allProcesses()
                .map(ph -> ph.info().command().orElse(""))
                .filter(cmd -> !cmd.isEmpty())
                .map(cmd -> new File(cmd).getName().toLowerCase())
                .collect(java.util.stream.Collectors.toSet());
        boolean anyAppFound = false;
        for (Object[] app : appsToTrack) {
            if (runningExes.contains(((String)app[2]).toLowerCase().trim())) {
                WorkflowDB.addTime((int)app[0], 0, secondsSinceLastTick);
                anyAppFound = true;
            }
        }

        if (activeTaskId != -1) {
            WorkflowDB.addTime(activeTaskId, 1, secondsSinceLastTick);
            for (Component c : taskContainer.getComponents()) {
                if (c instanceof TaskCard card && card.taskId == activeTaskId)
                    card.updateTime();
            }
        }
        if (anyAppFound)
            SwingUtilities.invokeLater(this::loadDataFromDB);
    }

    private void toggleTask(int id, String name) {
        if (activeTaskId == id) {
            activeTaskId = -1;
            activeTaskName = null;
        } else {
            activeTaskId = id;
            activeTaskName = name;
        }
        loadTasksFromDB();
    }

    private void loadTasksFromDB() {
        taskContainer.removeAll();
        List<Object[]> tasks = WorkflowDB.getTasksFull();
        for (Object[] t : tasks) {
            taskContainer.add(new TaskCard((int)t[0], (String)t[1], (String)t[2]));
            taskContainer.add(Box.createVerticalStrut(10));
        }
        taskContainer.revalidate();
        taskContainer.repaint();
    }

    private void loadDataFromDB() {
        if (appList == null) return;
        int lastSelected = appList.getSelectedIndex();
        appListModel.clear();
        for (Object[] app : WorkflowDB.getTrackedAppsFull()) {
            int sec = WorkflowDB.getSecondsToday((int)app[0], 0);
            appListModel.addElement(app[1] + "  [" + formatSeconds(sec) + "]");
        }
        if (lastSelected != -1 && lastSelected < appListModel.size())
            appList.setSelectedIndex(lastSelected);
    }

    private void refreshDateSelector() {
        dateSelector.removeAllItems();
        for (String d : WorkflowDB.getAvailableDates()) dateSelector.addItem(d);
    }

    private void updateCharts(String date, String filter) {
        new Thread(() -> {
            int[] hVal = WorkflowDB.getHourlyStats(date, filter);
            String[] hLab = new String[24];
            for(int i=0; i<24; i++)
                hLab[i] = "Time: " + i + ":00";
            WorkflowDB.StatResult wData = WorkflowDB.getPeriodStats(date, filter, 7);
            WorkflowDB.StatResult mData = WorkflowDB.getCalendarMonthStats(date, filter);
            SwingUtilities.invokeLater(() -> {
                hourChart.setData("Hourly Distribution (" + date + ")", hVal, hLab);
                weekChart.setData("Last 7 Days", wData.values, wData.labels);
                monthChart.setData("Monthly Activity", mData.values, mData.labels);
                pieChart.setData(WorkflowDB.getDaySummary(date));
            });
        }).start();
    }

    private void showRunningAppsSelector() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Select Running App", true);
        dialog.getContentPane().setBackground(UIStyle.BG_COLOR);
        dialog.setLayout(new BorderLayout());

        DefaultListModel<String> listModel = new DefaultListModel<>();
        Map<String, String> apps = new TreeMap<>();
        ProcessHandle.allProcesses().forEach(ph -> ph.info().command().ifPresent(cmd -> {
            String exe = new File(cmd).getName();
            if (exe.toLowerCase().endsWith(".exe") && !exe.equalsIgnoreCase("java.exe"))
                apps.put(exe, exe);
        }));
        apps.keySet().forEach(listModel::addElement);

        JList<String> list = new JList<>(listModel);
        list.setBackground(UIStyle.SECONDARY_BG);
        list.setForeground(Color.WHITE);
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(BorderFactory.createLineBorder(UIStyle.BORDER_COLOR));
        UIStyle.styleScrollBar(sp);

        JButton addBtn = new JButton("Add to Tracking");
        UIStyle.styleButton(addBtn);
        addBtn.addActionListener(_ -> {
            String sel = list.getSelectedValue();
            if (sel != null) {
                String pretty = sel.replace(".exe", "");
                pretty = pretty.substring(0, 1).toUpperCase() + pretty.substring(1).toLowerCase();
                WorkflowDB.addTrackedApp(pretty, sel);
                loadDataFromDB();
                dialog.dispose();
            }
        });

        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(UIStyle.BG_COLOR);
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JLabel("<html><b style='color:gray'>Select an application:</b></html>"), BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        p.add(addBtn, BorderLayout.SOUTH);
        dialog.add(p);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String formatSeconds(int totalSeconds) {
        return String.format("%02d:%02d:%02d", totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60);
    }

    // ===================
    // КАРТОЧКА ЗАДАЧИ
    // ===================

    private class TaskCard extends JPanel {
        private final int taskId;
        private final JLabel timeLabel;

        public TaskCard(int id, String name, String desc) {
            this.taskId = id;
            setLayout(new BorderLayout(10, 5));
            setBackground(UIStyle.SECONDARY_BG);
            Color accent = (activeTaskId == id) ? Color.RED : new Color(194, 0, 255);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(accent, 1),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));
            setMaximumSize(new Dimension(Short.MAX_VALUE, 110));

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            JLabel nameLbl = new JLabel(name.toUpperCase());
            nameLbl.setForeground(new Color(194, 0, 255));
            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            JLabel descLbl = new JLabel("<html><body style='width: 150px'>" + (desc.isEmpty() ? "No description" : desc) + "</body></html>");
            descLbl.setForeground(Color.LIGHT_GRAY);
            descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            textPanel.add(nameLbl);
            textPanel.add(Box.createVerticalStrut(3));
            textPanel.add(descLbl);

            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setOpaque(false);
            timeLabel = new JLabel("Time: " + formatSeconds(WorkflowDB.getSecondsToday(id, 1)));
            timeLabel.setForeground(Color.WHITE);
            timeLabel.setFont(new Font("Monospaced", Font.BOLD, 12));

            JButton startBtn = new JButton(activeTaskId == id ? "STOP" : "START");
            UIStyle.styleButton(startBtn);
            if (activeTaskId == id)
                startBtn.setForeground(Color.RED);
            startBtn.addActionListener(_ -> toggleTask(id, name));

            JButton delBtn = new JButton("DEL");
            UIStyle.styleButton(delBtn);
            delBtn.setForeground(Color.GRAY);
            delBtn.addActionListener(_ -> {
                if (activeTaskId == id) return;
                WorkflowDB.deleteTask(id); loadTasksFromDB();
            });

            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
            btns.setOpaque(false);
            btns.add(startBtn);
            btns.add(delBtn);
            bottomPanel.add(timeLabel, BorderLayout.WEST);
            bottomPanel.add(btns, BorderLayout.EAST);
            add(textPanel, BorderLayout.CENTER);
            add(bottomPanel, BorderLayout.SOUTH);
        }

        public void updateTime() {
            timeLabel.setText("Time: " + formatSeconds(WorkflowDB.getSecondsToday(taskId, 1)));
        }
    }
}