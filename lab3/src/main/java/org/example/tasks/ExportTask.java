package org.example.tasks;

import javafx.concurrent.Task;
import org.example.io.Serializer;
import org.example.model.Contact;

import java.nio.file.Path;

public class ExportTask extends Task<Void> {

    private final Path file;
    private final Contact[] contacts;

    public ExportTask(Path file, Contact[] contacts) {
        this.file = file;
        this.contacts = contacts;
    }

    @Override
    protected Void call() throws Exception {
        updateMessage("Экспорт в " + file.getFileName());
        Serializer.forFile(file).save(contacts, file);
        return null;
    }
}
