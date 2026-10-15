package org.example.tasks;

import javafx.concurrent.Task;
import org.example.io.ContactFormat;
import org.example.io.FileStorage;
import org.example.model.Contact;
import org.example.store.EntityStore;

import java.nio.file.Path;

public class ImportTask extends Task<Integer> {

    private final Path file;
    private final EntityStore<String, Contact> target;
    private final ContactFormat format;

    public ImportTask(Path file, EntityStore<String, Contact> target, ContactFormat format) {
        this.file = file;
        this.target = target;
        this.format = format;
    }

    @Override
    protected Integer call() throws Exception {
        updateMessage("Импорт " + file.getFileName() + " (" + format.title() + ")");
        updateProgress(-1, 1);
        int loaded = new FileStorage<>(format.serializer()).loadInto(file, target);
        updateMessage("Импортировано: " + loaded);
        return loaded;
    }
}
