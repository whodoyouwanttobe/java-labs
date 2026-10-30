package org.example.io;

public enum SerializationError {
    EMPTY_FILE,
    BAD_FORMAT,
    BAD_VERSION,
    MISSING_FIELD,
    BAD_VALUE,
    UNKNOWN_TYPE,
    DUPLICATE_NAME,
    TRUNCATED
}
