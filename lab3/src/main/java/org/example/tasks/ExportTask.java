package org.example.tasks;

import javafx.concurrent.Task;
import org.example.io.ContactFormat;
import org.example.io.FileStorage;
import org.example.model.Contact;
import org.example.store.EntityStore;

import java.nio.file.Path;

public class ExportTask extends Task<Integer> {

    private final Path file;
    private final EntityStore<String, Contact> source;
    private final ContactFormat format;

    public ExportTask(Path file, EntityStore<String, Contact> source, ContactFormat format) {
        this.file = file;
        this.source = source;
        this.format = format;
    }

    @Override
    protected Integer call() throws Exception {
        updateMessage("Экспорт в " + file.getFileName() + " (" + format.title() + ")");
        updateProgress(-1, 1);
        int saved = new FileStorage<>(format.serializer()).save(file, source);
        updateMessage("Экспортировано: " + saved);
        return saved;
    }
}
