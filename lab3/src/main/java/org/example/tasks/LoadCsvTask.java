package org.example.tasks;

import javafx.concurrent.Task;
import org.example.csv.CsvLoader;
import org.example.csv.LoadResult;
import org.example.model.Contact;
import org.example.store.EntityStore;

import java.nio.file.Path;

public class LoadCsvTask extends Task<LoadResult> {

    private final Path file;
    private final EntityStore<String, Contact> target;

    public LoadCsvTask(Path file, EntityStore<String, Contact> target) {
        this.file = file;
        this.target = target;
    }

    @Override
    protected LoadResult call() throws Exception {
        updateMessage("Загрузка " + file.getFileName());
        LoadResult result = new CsvLoader().load(file, target, this::updateProgress, this::isCancelled);
        updateMessage("Загружено: " + result.getLoaded() + ", пропущено строк: " + result.getErrors().size());
        return result;
    }
}
