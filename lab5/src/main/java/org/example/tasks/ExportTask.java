package org.example.tasks;

import javafx.concurrent.Task;
import org.example.io.Serializer;
import org.example.model.Contact;

import java.nio.file.Path;

public class ExportTask extends Task<Void> {

    private final Path file;
    private final Contact[] contacts;
    private final int version;

    public ExportTask(Path file, Contact[] contacts, int version) {
        this.file = file;
        this.contacts = contacts;
        this.version = version;
    }

    @Override
    protected Void call() throws Exception {
        updateMessage("Экспорт в " + file.getFileName() + " (v" + version + ")");
        Serializer.forFile(file).save(contacts, file, version);
        return null;
    }
}

