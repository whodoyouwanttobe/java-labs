package org.example.io;

import org.example.model.Contact;
import org.example.store.ContactStores;
import org.example.store.TrieStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageTest {

    @TempDir
    Path dir;

    @Test
    void saveOverwritesAndLeavesNoTempFiles() throws Exception {
        TrieStore<Contact> store = ContactStores.create();
        store.add(new Contact("Анна", "+7", "", ""));
        FileStorage<Contact> storage = new FileStorage<>(new JsonContactSerializer());
        Path file = dir.resolve("book.json");

        storage.save(file, store);
        store.add(new Contact("Борис", "+8", "", ""));
        storage.save(file, store);

        assertEquals(2, storage.read(file).length);
        assertEquals(1, countFiles());
    }

    @Test
    void formatIsChosenByExtension() {
        assertEquals(ContactFormat.JSON, ContactFormat.of(Path.of("a/b/Contacts.JSON")));
        assertEquals(ContactFormat.BINARY, ContactFormat.of(Path.of("c.ctb")));
        assertThrows(IllegalArgumentException.class, () -> ContactFormat.of(Path.of("c.txt")));
    }

    @Test
    void missingFileIsIoErrorNotFormatError() {
        FileStorage<Contact> storage = new FileStorage<>(new BinaryContactSerializer());
        assertThrows(IOException.class, () -> storage.read(dir.resolve("nope.ctb")));
    }

    private long countFiles() throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.count();
        }
    }
}
