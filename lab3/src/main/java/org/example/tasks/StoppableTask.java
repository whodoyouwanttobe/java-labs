package org.example.tasks;

import javafx.concurrent.Task;

public abstract class StoppableTask<T> extends Task<T> {

    private volatile boolean stopRequested;

    public void requestStop() {
        stopRequested = true;
    }

    protected boolean shouldStop() {
        return stopRequested || isCancelled();
    }
}
