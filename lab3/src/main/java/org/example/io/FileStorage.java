package org.example.io;

import org.example.store.EntityStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileStorage<T> {

    private final Serializer<T> serializer;

    public FileStorage(Serializer<T> serializer) {
        this.serializer = serializer;
    }

    public int save(Path file, EntityStore<?, T> source) throws IOException {
        T[] items = source.toArray();
        Path target = file.toAbsolutePath();
        Path temp = Files.createTempFile(target.getParent(), "." + target.getFileName(), ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp)) {
                serializer.write(items, out);
            }
            replace(temp, target);
        } finally {
            Files.deleteIfExists(temp);
        }
        return items.length;
    }

    public T[] read(Path file) throws IOException, SerializationException {
        try (InputStream in = Files.newInputStream(file)) {
            return serializer.read(in);
        }
    }

    public int loadInto(Path file, EntityStore<?, T> target) throws IOException, SerializationException {
        T[] items = read(file);
        for (int i = 0; i < items.length; i++) {
            if (!target.tryAdd(items[i])) {
                throw new SerializationException(SerializationError.DUPLICATE_KEY,
                        "Запись повторяет ключ одной из предыдущих: " + items[i], "запись " + i);
            }
        }
        return items.length;
    }

    private static void replace(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
