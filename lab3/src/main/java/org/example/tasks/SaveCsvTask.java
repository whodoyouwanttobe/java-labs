package org.example.tasks;

import javafx.concurrent.Task;
import org.example.csv.CsvSaver;
import org.example.model.Contact;

import java.nio.file.Path;

public class SaveCsvTask extends Task<Void> {

    private final Path file;
    private final Contact[] contacts;

    public SaveCsvTask(Path file, Contact[] contacts) {
        this.file = file;
        this.contacts = contacts;
    }

    @Override
    protected Void call() throws Exception {
        updateMessage("Сохранение в " + file.getFileName());
        new CsvSaver().save(file, contacts);
        return null;
    }
}
