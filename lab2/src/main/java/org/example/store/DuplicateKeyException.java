package org.example.store;

public class DuplicateKeyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateKeyException(String key) {
        super("Контакт с именем \"" + key + "\" уже есть");
    }
}
