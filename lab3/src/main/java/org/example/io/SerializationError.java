package org.example.io;

public enum SerializationError {
    EMPTY_FILE,
    MALFORMED,
    MISSING_FIELD,
    INVALID_VALUE,
    UNKNOWN_TYPE,
    DUPLICATE_KEY,
    BAD_MAGIC,
    UNSUPPORTED_VERSION,
    TRUNCATED,
    CORRUPTED
}
