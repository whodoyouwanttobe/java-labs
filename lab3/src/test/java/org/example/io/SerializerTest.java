package org.example.io;

import org.example.generator.ContactGenerator;
import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.model.EmergencyContact;
import org.example.store.ContactStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializerTest {

    private static final Contact[] CONTACTS = {
            new Contact("Анна \"Белка\" Белова", "+7 (913) 000-00-04", "a@mail.ru", "ООО Рога; Копыта"),
            new EmergencyContact("Служба спасения", "112", "", ""),
            new CorporateContact("John O'Neil", "", "john@corp.local", "Ü-Tech GmbH", "Lead\\Dev", "007")
    };

    @TempDir
    Path dir;

    @Test
    void jsonRoundTripKeepsAllContacts() throws Exception {
        assertArrayEquals(CONTACTS, roundTrip(new JsonSerializer(), CONTACTS));
    }

    @Test
    void binaryRoundTripKeepsAllContacts() throws Exception {
        assertArrayEquals(CONTACTS, roundTrip(new BinarySerializer(), CONTACTS));
    }

    @Test
    void emptyArrayRoundTrip() throws Exception {
        assertEquals(0, roundTrip(new BinarySerializer(), new Contact[0]).length);
    }

    @Test
    void bigStoreRoundTrip() throws Exception {
        ContactStore store = new ContactStore();
        new ContactGenerator(7).fill(store, 2000);
        assertArrayEquals(store.toArray(), roundTrip(new JsonSerializer(), store.toArray()));
    }

    @Test
    void binaryFileIsSmallerThanJson() throws Exception {
        Path json = dir.resolve("a.json");
        Path binary = dir.resolve("a.ctb");
        new JsonSerializer().save(CONTACTS, json);
        new BinarySerializer().save(CONTACTS, binary);
        assertTrue(Files.size(binary) < Files.size(json));
    }

    @Test
    void serializerIsChosenByExtension() {
        assertEquals("ctb", Serializer.forFile(Path.of("contacts.CTB")).getFormat());
    }

    @Test
    void unknownExtensionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Serializer.forFile(Path.of("contacts.txt")));
    }

    private Contact[] roundTrip(Serializer serializer, Contact[] contacts) throws Exception {
        Path file = dir.resolve("contacts." + serializer.getFormat());
        serializer.save(contacts, file);
        return serializer.load(file);
    }
}
