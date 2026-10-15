package org.example.tasks;

public record GenerationResult(int added, int requested) {

    public boolean stopped() {
        return added < requested;
    }
}
