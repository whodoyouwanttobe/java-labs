package org.example.io;

public class SerializationException extends Exception {

    private final SerializationError error;
    private final String location;

    public SerializationException(SerializationError error, String message, String location) {
        this(error, message, location, null);
    }

    public SerializationException(SerializationError error, String message, String location, Throwable cause) {
        super(location == null ? message : message + " (" + location + ")", cause);
        this.error = error;
        this.location = location;
    }

    public SerializationError getError() {
        return error;
    }

    public String getLocation() {
        return location;
    }
}
