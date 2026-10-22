package org.example.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.example.tasks.StoppableTask;
import org.example.io.SerializationException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

class BackgroundRunner {

    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "contacts-worker");
        thread.setDaemon(true);
        return thread;
    });
    private Task<?> current;

    BackgroundRunner(ProgressBar progressBar, Label statusLabel) {
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
    }

    ReadOnlyBooleanProperty busyProperty() {
        return busy;
    }

    <T> void run(Task<T> task, Consumer<T> onSuccess, Runnable onCancel) {
        if (busy.get()) {
            return;
        }
        current = task;
        busy.set(true);
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            finish();
            onSuccess.accept(task.getValue());
        });
        task.setOnCancelled(e -> {
            finish();
            onCancel.run();
        });
        task.setOnFailed(e -> {
            finish();
            Throwable error = task.getException();
            statusLabel.setText("Ошибка");
            if (error instanceof SerializationException se) {
                String where = se.getLocation() == null ? "—" : se.getLocation();
                Alerts.warningWithDetails(
                        "Файл не прочитан",
                        se.getMessage(),
                        "Код: " + se.getError() + "\nГде: " + where);
            } else {
                Alerts.error(error == null ? "Неизвестная ошибка" : error.getMessage());
            }

        });

        executor.execute(task);
    }

    void cancel() {
        if (current instanceof StoppableTask<?> stoppable) {
            stoppable.requestStop();
        } else if (current != null) {
            current.cancel();
        }
    }

    void shutdown() {
        cancel();
        executor.shutdownNow();
    }

    private void finish() {
        progressBar.progressProperty().unbind();
        statusLabel.textProperty().unbind();
        progressBar.setProgress(0);
        current = null;
        busy.set(false);
    }
}
