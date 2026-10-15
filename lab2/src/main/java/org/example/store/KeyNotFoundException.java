package org.example.store;

public class KeyNotFoundException extends RuntimeException {
    private final String key;

    public KeyNotFoundException(String key) {
        super("Записи с ключом '" + key + "' нет");
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
