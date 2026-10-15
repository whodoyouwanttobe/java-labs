package org.example.tasks;

@FunctionalInterface
public interface ProgressListener {
    ProgressListener NONE = (done, total) -> { };

    void onProgress(long done, long total);
}
