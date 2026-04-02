package ui.utils;

import db.WorkflowDB;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class WorkflowService {
    private static int activeTaskId = -1;
    private static long lastTickTime = System.currentTimeMillis();
    private static boolean isRunning = false;

    public static void startTracking() {
        if (isRunning) return;
        isRunning = true;
        new Thread(() -> {
            AppLogger.info("Background Workflow Service started.");
            while (isRunning) {
                try {
                    runTrackingCycle();
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private static void runTrackingCycle() {
        long currentTime = System.currentTimeMillis();
        int secondsSinceLastTick = (int) ((currentTime - lastTickTime) / 1000);
        lastTickTime = currentTime;

        if (secondsSinceLastTick <= 0) return;

        List<Object[]> appsToTrack = WorkflowDB.getTrackedAppsFull();
        java.util.Set<String> runningExes = ProcessHandle.allProcesses()
                .map(ph -> ph.info().command().orElse(""))
                .filter(cmd -> !cmd.isEmpty())
                .map(cmd -> new File(cmd).getName().toLowerCase())
                .collect(Collectors.toSet());
        for (Object[] app : appsToTrack) {
            String exeName = (String) app[2];
            if (runningExes.contains(exeName.toLowerCase().trim())) {
                WorkflowDB.addTime((int) app[0], 0, secondsSinceLastTick);
            }
        }

        if (activeTaskId != -1) {
            WorkflowDB.addTime(activeTaskId, 1, secondsSinceLastTick);
        }
    }

    public static void setActiveTaskId(int id) {
        activeTaskId = id;
    }
    public static int getActiveTaskId() {
        return activeTaskId;
    }
}