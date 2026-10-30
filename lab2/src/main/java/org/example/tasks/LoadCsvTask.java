package org.example.tasks;

import javafx.concurrent.Task;
import org.example.csv.CsvLoader;
import org.example.csv.LoadResult;
import org.example.store.ContactStore;

import java.nio.file.Path;

public class LoadCsvTask extends Task<LoadResult> {

    private final Path file;
    private final ContactStore store;

    public LoadCsvTask(Path file, ContactStore store) {
        this.file = file;
        this.store = store;
    }

    @Override
    protected LoadResult call() throws Exception {
        updateMessage("Загрузка " + file.getFileName());
        return new CsvLoader().load(file, store, this::updateProgress);
    }
}
