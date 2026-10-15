package org.example.tasks;

import javafx.concurrent.Task;
import org.example.csv.CsvSaver;
import org.example.model.Contact;
import org.example.store.EntityStore;

import java.nio.file.Path;

public class SaveCsvTask extends Task<Integer> {

    private final Path file;
    private final EntityStore<String, Contact> source;

    public SaveCsvTask(Path file, EntityStore<String, Contact> source) {
        this.file = file;
        this.source = source;
    }

    @Override
    protected Integer call() throws Exception {
        updateMessage("Сохранение в " + file.getFileName());
        int saved = new CsvSaver().save(file, source);
        updateMessage("Сохранено: " + saved);
        return saved;
    }
}
