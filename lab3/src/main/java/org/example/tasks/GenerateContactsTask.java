package org.example.tasks;

import org.example.generator.ContactGenerator;
import org.example.model.Contact;
import org.example.store.EntityStore;

public class GenerateContactsTask extends StoppableTask<GenerationResult> {

    private final EntityStore<String, Contact> target;
    private final int count;
    private final ContactGenerator generator;

    public GenerateContactsTask(EntityStore<String, Contact> target, int count) {
        this(target, count, new ContactGenerator());
    }

    public GenerateContactsTask(EntityStore<String, Contact> target, int count, ContactGenerator generator) {
        if (count <= 0) {
            throw new IllegalArgumentException("Количество должно быть больше нуля");
        }
        this.target = target;
        this.count = count;
        this.generator = generator;
    }

    @Override
    protected GenerationResult call() {
        updateMessage("Генерация " + count + " контактов");
        int added = generator.generateInto(target, count, this::updateProgress, this::shouldStop);
        GenerationResult result = new GenerationResult(added, count);
        updateMessage(result.stopped() ? "Остановлено, добавлено " + added : "Добавлено " + added);
        return result;
    }
}
