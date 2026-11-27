package org.example.io;

import org.example.model.Contact;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public interface Serializer {

    int MIN_SCHEMA_VERSION = 1;
    int SCHEMA_VERSION = 3;

    /**
     * Версии, поддерживаемые и при чтении, и при записи: от текущей вниз до минимальной.
     * Единственный источник истины для GUI (например, список версий в диалоге экспорта):
     * при переходе на v4 список сам станет {@code [4, 3, 2, 1]}.
     */
    List<Integer> SUPPORTED_VERSIONS = supportedVersions();

    static List<Integer> supportedVersions() {
        List<Integer> versions = new ArrayList<>(SCHEMA_VERSION - MIN_SCHEMA_VERSION + 1);
        for (int version = SCHEMA_VERSION; version >= MIN_SCHEMA_VERSION; version--) {
            versions.add(version);
        }
        return List.copyOf(versions);
    }

    String getFormat();

    default void save(Contact[] contacts, Path file) throws IOException {
        save(contacts, file, SCHEMA_VERSION);
    }

    void save(Contact[] contacts, Path file, int version) throws IOException;

    int readVersion(Path file) throws IOException, SerializationException;

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

