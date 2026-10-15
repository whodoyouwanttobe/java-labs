package org.example.csv;

import java.util.List;

public class LoadResult {
    private final int loaded;
    private final List<CsvParseException> errors;
    private final boolean cancelled;

    public LoadResult(int loaded, List<CsvParseException> errors, boolean cancelled) {
        this.loaded = loaded;
        this.errors = errors;
        this.cancelled = cancelled;
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

    public boolean isCancelled() {
        return cancelled;
    }
}
