package org.example.csv;

import java.util.List;

public class LoadResult {
    private final int loaded;
    private final List<CsvParseException> errors;

    public LoadResult(int loaded, List<CsvParseException> errors) {
        this.loaded = loaded;
        this.errors = errors;
    }

    public int getLoaded() {
        return loaded;
    }

    public List<CsvParseException> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
