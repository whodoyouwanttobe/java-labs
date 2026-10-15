package org.example.store;

public class DuplicateKeyException extends RuntimeException {
    private final String key;

    public DuplicateKeyException(String key) {
        super("Запись с ключом '" + key + "' уже есть");
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
