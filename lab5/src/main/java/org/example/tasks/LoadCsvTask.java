package org.example.tasks;

import javafx.concurrent.Task;
import org.example.csv.CsvLoader;
import org.example.csv.CsvParseException;
import org.example.store.ContactStore;

import java.nio.file.Path;
import java.util.List;

public class LoadCsvTask extends Task<List<CsvParseException>> {

    private final Path file;
    private final ContactStore store;

    public LoadCsvTask(Path file, ContactStore store) {
        this.file = file;
        this.store = store;
    }

    @Override
    protected List<CsvParseException> call() throws Exception {
        updateMessage("Загрузка " + file.getFileName());
        return new CsvLoader().load(file, store, this::updateProgress);
    }
}

