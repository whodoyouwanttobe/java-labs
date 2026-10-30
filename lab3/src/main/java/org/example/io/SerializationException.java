package org.example.io;

public class SerializationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final SerializationError error;

    public SerializationException(SerializationError error, String message) {
        super(message);
        this.error = error;
    }

    public SerializationError getError() {
        return error;
    }
}
