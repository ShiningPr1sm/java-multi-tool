package util;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JavaFxFileChooser {

    private JavaFxFileChooser() {}

    public static File openFile(String title, FileChooser.ExtensionFilter... filters) {
        FileChooser chooser = new FileChooser();
        if (title != null) chooser.setTitle(title);
        if (filters.length > 0) chooser.getExtensionFilters().addAll(filters);
        return show(stage -> chooser.showOpenDialog(stage));
    }

    public static List<File> openFiles(String title, FileChooser.ExtensionFilter... filters) {
        FileChooser chooser = new FileChooser();
        if (title != null) chooser.setTitle(title);
        if (filters.length > 0) chooser.getExtensionFilters().addAll(filters);
        return show(stage -> chooser.showOpenMultipleDialog(stage));
    }

    public static File saveFile(String title, String initialFileName, FileChooser.ExtensionFilter... filters) {
        FileChooser chooser = new FileChooser();
        if (title != null) chooser.setTitle(title);
        if (initialFileName != null) chooser.setInitialFileName(initialFileName);
        if (filters.length > 0) chooser.getExtensionFilters().addAll(filters);
        return show(stage -> chooser.showSaveDialog(stage));
    }

    public static File chooseDirectory(String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        if (title != null) chooser.setTitle(title);
        return show(stage -> chooser.showDialog(stage));
    }

    @FunctionalInterface
    private interface DialogCall<T> {
        T call(Stage stage);
    }

    private static <T> T show(DialogCall<T> call) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            Stage stage = emptyStage();
            try {
                T result = call.call(stage);
                future.complete(result);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            } finally {
                stage.close();
            }
        });
        try {
            return future.get();
        } catch (Exception e) {
            AppLogger.error("JavaFxFileChooser: dialog error - " + e.getMessage());
            return null;
        }
    }

    private static Stage emptyStage() {
        Stage stage = new Stage();
        stage.setWidth(0);
        stage.setHeight(0);
        stage.setOpacity(0);
        stage.setTitle("");
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        stage.setX(screen.getWidth() / 2);
        stage.setY(screen.getHeight() / 2);
        return stage;
    }
}
