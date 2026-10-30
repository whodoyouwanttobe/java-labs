package org.example.tasks;

import javafx.concurrent.Task;
import org.example.generator.ContactGenerator;
import org.example.store.ContactStore;

public class GenerateTask extends Task<Integer> {

    private static final int PROGRESS_STEP = 500;

    private final ContactStore store;
    private final int count;

    public GenerateTask(ContactStore store, int count) {
        this.store = store;
        this.count = count;
    }

    @Override
    protected Integer call() {
        updateMessage("Генерация " + count + " контактов");
        ContactGenerator generator = new ContactGenerator();
        int added = 0;
        while (added < count && !isCancelled()) {
            store.add(generator.next(store));
            added++;
            if (added % PROGRESS_STEP == 0) {
                updateProgress(added, count);
            }
        }
        return added;
    }
}
