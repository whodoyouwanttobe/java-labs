package org.example.ui;

import javafx.concurrent.Task;
import java.util.function.Consumer;


class TaskRunner {
    private final StatusBarView statusBar;
    private final Consumer<Boolean> onBusy;
    private Runnable onSuccess;
    private Runnable onCancelled;
    private Consumer<Throwable> onError;
    private Task<?> currentTask;
    TaskRunner(StatusBarView statusBar, Consumer<Boolean> onBusy) {
        this.statusBar = statusBar;
        this.onBusy = onBusy;
        statusBar.setOnCancel(() -> {
            if (currentTask != null) {
                currentTask.cancel();
            }
        });
    }
    void setOnSuccess(Runnable handler) {
        this.onSuccess = handler;
    }

    void setOnCancelled(Runnable handler) {
        this.onCancelled = handler;
    }

    void setOnError(Consumer<Throwable> handler) {
        this.onError = handler;
    }
    void run(Task<?> task) {
        currentTask = task;
        onBusy.accept(true);
        statusBar.bindProgress(task.progressProperty());
        statusBar.bindStatus(task.messageProperty());

        task.setOnSucceeded(e -> {
            finish();
            onSuccess.run();
        });
        task.setOnCancelled(e -> {
            finish();
            onCancelled.run();
        });
        task.setOnFailed(e -> {
            finish();
            Throwable error = task.getException();
            onError.accept(error);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    private void finish() {
        statusBar.unbindProgress();
        statusBar.unbindStatus();
        onBusy.accept(false);
    }
}

