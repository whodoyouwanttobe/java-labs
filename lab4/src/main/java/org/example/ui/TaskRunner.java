package org.example.ui;

import javafx.concurrent.Task;

import java.util.function.Consumer;

public class TaskRunner {

    private final StatusBarView statusBar;
    private final Consumer<Boolean> onBusy;
    private Runnable onSuccess;
    private Runnable onCancelled;
    private Consumer<Throwable> onError;
    private Task<?> currentTask;

    public TaskRunner(StatusBarView statusBar, Consumer<Boolean> onBusy) {
        this.statusBar = statusBar;
        this.onBusy = onBusy;
        statusBar.setOnCancel(() -> {
            if (currentTask != null) {
                currentTask.cancel();
            }
        });
    }

    public void setOnSuccess(Runnable handler) {
        this.onSuccess = handler;
    }

    public void setOnCancelled(Runnable handler) {
        this.onCancelled = handler;
    }

    public void setOnError(Consumer<Throwable> handler) {
        this.onError = handler;
    }

    public void run(Task<?> task) {
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
            onError.accept(task.getException());
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

