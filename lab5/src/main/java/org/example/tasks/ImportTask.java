package org.example.tasks;

import javafx.concurrent.Task;
import org.example.io.Serializer;
import org.example.model.Contact;
import org.example.store.ContactStore;

import java.nio.file.Path;

public class ImportTask extends Task<Integer> {

    private final Path file;
    private final ContactStore store;

    public ImportTask(Path file, ContactStore store) {
        this.file = file;
        this.store = store;
    }

    @Override
    protected Integer call() throws Exception {
        updateMessage("Импорт из " + file.getFileName());
        Contact[] contacts = Serializer.forFile(file).load(file);
        for (Contact contact : contacts) {
            store.add(contact);
        }
        return contacts.length;
    }
}

