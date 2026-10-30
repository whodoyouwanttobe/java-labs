package org.example.store;

public class KeyNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public KeyNotFoundException(String key) {
        super("Контакта с именем \"" + key + "\" нет");
    }
}
