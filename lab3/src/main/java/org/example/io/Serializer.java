package org.example.io;

import org.example.model.Contact;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public interface Serializer {

    int SCHEMA_VERSION = 1;

    String getFormat();

    void save(Contact[] contacts, Path file) throws IOException;

    Contact[] load(Path file) throws IOException, SerializationException;

    static Serializer forFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".json")) {
            return new JsonSerializer();
        }
        if (name.endsWith(".ctb")) {
            return new BinarySerializer();
        }
        throw new IllegalArgumentException("Поддерживаются только файлы .json и .ctb");
    }
}
