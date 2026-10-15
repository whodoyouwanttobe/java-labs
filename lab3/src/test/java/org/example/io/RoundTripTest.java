package org.example.io;

import org.example.generator.ContactGenerator;
import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.model.EmergencyContact;
import org.example.store.ContactStores;
import org.example.store.TrieStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RoundTripTest {

    @TempDir
    Path dir;

    private static final Contact[] TRICKY = {
            new Contact("Анна \"Белка\" Белова", "+7 (913) 000-00-04", "a@mail.ru", "ООО Рога; Копыта"),
            new EmergencyContact("Служба спасения", "112", "", ""),
            new CorporateContact("John O'Neil", "", "john@corp.local", "Ü-Tech GmbH", "Lead\\Dev", "007"),
            new Contact("Ёжик 🦔", "", "", "Туманный лес")
    };

    @Test
    void jsonKeepsAllFieldsAndTypes() throws Exception {
        assertArrayEquals(TRICKY, roundTrip(new JsonContactSerializer(), TRICKY));
    }

    @Test
    void binaryKeepsAllFieldsAndTypes() throws Exception {
        assertArrayEquals(TRICKY, roundTrip(new BinaryContactSerializer(), TRICKY));
    }

    @Test
    void emptyArrayRoundTripsInBothFormats() throws Exception {
        for (ContactFormat format : ContactFormat.values()) {
            assertEquals(0, roundTrip(format.serializer(), new Contact[0]).length);
        }
    }

    @Test
    void typesSurviveAsSubclasses() throws Exception {
        for (ContactFormat format : ContactFormat.values()) {
            Contact[] back = roundTrip(format.serializer(), TRICKY);
            assertTrue(back[1] instanceof EmergencyContact);
            assertTrue(back[2] instanceof CorporateContact);
        }
    }

    @Test
    void storageSaveAndLoadGivesSameStore() throws Exception {
        TrieStore<Contact> store = ContactStores.create();
        new ContactGenerator(7).generateInto(store, 2000);
        for (ContactFormat format : ContactFormat.values()) {
            Path file = dir.resolve("all." + format.extension());
            FileStorage<Contact> storage = new FileStorage<>(format.serializer());
            storage.save(file, store);

            TrieStore<Contact> loaded = ContactStores.create();
            assertEquals(2000, storage.loadInto(file, loaded));
            assertArrayEquals(store.toArray(), loaded.toArray());
        }
    }

    @Test
    void binaryIsSmallerThanJson() throws Exception {
        TrieStore<Contact> store = ContactStores.create();
        new ContactGenerator(8).generateInto(store, 500);
        Contact[] all = store.toArray();
        assertTrue(bytes(new BinaryContactSerializer(), all).length < bytes(new JsonContactSerializer(), all).length);
    }

    private static Contact[] roundTrip(Serializer<Contact> serializer, Contact[] items)
            throws IOException, SerializationException {
        return serializer.read(new ByteArrayInputStream(bytes(serializer, items)));
    }

    private static byte[] bytes(Serializer<Contact> serializer, Contact[] items) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.write(items, out);
        return out.toByteArray();
    }
}
