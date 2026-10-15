package org.example.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Serializer<T> {

    int SCHEMA_VERSION = 1;

    void write(T[] items, OutputStream out) throws IOException;

    T[] read(InputStream in) throws IOException, SerializationException;
}
