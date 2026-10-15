package org.example.csv;

import org.example.generator.ContactGenerator;
import org.example.model.Contact;
import org.example.store.ContactStores;
import org.example.store.TrieStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvLoaderTest {

    @TempDir
    Path dir;

    @Test
    void duplicateNameIsReportedAndSkipped() throws IOException {
        Path file = dir.resolve("c.csv");
        Files.writeString(file, CsvSaver.HEADER + "\n"
                + "Contact;Иван;+7;;НГТУ\n"
                + "Contact;иван;+8;;НГТУ\n", StandardCharsets.UTF_8);
        TrieStore<Contact> store = ContactStores.create();

        LoadResult result = new CsvLoader().load(file, store);

        assertEquals(CsvErrorCode.DUPLICATE_NAME, result.getErrors().get(0).getErrorCode());
    }

    @Test
    void saveThenLoadKeepsAllContacts() throws IOException {
        TrieStore<Contact> store = ContactStores.create();
        store.add(new Contact("Анна", "+7", "a@mail.ru", "НГТУ"));
        store.add(new Contact("Борис", "+8", "b@mail.ru", "НГТУ"));
        Path file = dir.resolve("out.csv");
        new CsvSaver().save(file, store);

        TrieStore<Contact> loaded = ContactStores.create();
        new CsvLoader().load(file, loaded);

        assertEquals(2, loaded.size());
    }

    @Test
    void progressIsReportedInBytesUpToFileSize() throws IOException {
        TrieStore<Contact> store = ContactStores.create();
        new ContactGenerator(3).generateInto(store, 3000);
        Path file = dir.resolve("big.csv");
        new CsvSaver().save(file, store);
        long size = Files.size(file);
        long[] last = new long[2];
        boolean[] monotonic = {true};

        new CsvLoader().load(file, ContactStores.create(), (done, total) -> {
            monotonic[0] &= done >= last[0];
            last[0] = done;
            last[1] = total;
        }, () -> false);

        assertTrue(monotonic[0]);
        assertEquals(size, last[0]);
        assertEquals(size, last[1]);
    }

    @Test
    void cancelledLoadReportsCancelled() throws IOException {
        TrieStore<Contact> store = ContactStores.create();
        new ContactGenerator(4).generateInto(store, 100);
        Path file = dir.resolve("c100.csv");
        new CsvSaver().save(file, store);
        TrieStore<Contact> target = ContactStores.create();

        LoadResult result = new CsvLoader().load(file, target, (d, t) -> { }, () -> target.size() >= 20);

        assertTrue(result.isCancelled());
        assertEquals(20, result.getLoaded());
    }
}
