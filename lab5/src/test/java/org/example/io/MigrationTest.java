package org.example.io;

import org.example.io.dto.ContactDto;
import org.example.io.dto.ContactFileDto;
import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationTest {

    @TempDir
    Path dir;

    @Test
    void stepV1ToV2FillsDefaultUpdatedAt() throws Exception {
        ContactDto dto = new ContactDto();
        dto.type = "Contact";
        dto.name = "Анна";
        ContactMigrator.migrateV1ToV2(dto);
        assertNotNull(dto.updatedAt);
        assertEquals(Contact.DEFAULT_UPDATED_AT, dto.updatedAt);
    }

    @Test
    void stepV2ToV3FillsDefaultNote() throws Exception {
        ContactDto dto = new ContactDto();
        dto.type = "Contact";
        dto.name = "Анна";
        dto.updatedAt = "2026-10-01";
        ContactMigrator.migrateV2ToV3(dto);
        assertNotNull(dto.note);
        assertEquals(Contact.DEFAULT_NOTE, dto.note);
    }

    @Test
    void chainMigrationUpgradesV1ToV3() throws Exception {
        ContactDto dto = new ContactDto();
        dto.type = "Contact";
        dto.name = "Анна";
        ContactFileDto file = new ContactFileDto();
        file.schemaVersion = 1;
        file.contacts = new ContactDto[]{dto};

        ContactMigrator.migrate(file);

        assertEquals(3, file.schemaVersion);
        assertEquals(Contact.DEFAULT_UPDATED_AT, file.contacts[0].updatedAt);
        assertEquals(Contact.DEFAULT_NOTE, file.contacts[0].note);
    }

    @Test
    void jsonV1FixtureMigratesToV3() throws Exception {
        Path v1 = fixture("valid-v1.json");
        JsonSerializer json = new JsonSerializer();
        assertEquals(1, json.readVersion(v1));

        Contact[] contacts = json.load(v1);

        assertEquals(4, contacts.length);
        assertEquals(Contact.DEFAULT_UPDATED_AT, contacts[0].getUpdatedAt());
        assertEquals(Contact.DEFAULT_NOTE, contacts[0].getNote());
    }

    @Test
    void jsonV2FixtureMigratesToV3AndKeepsUpdatedAt() throws Exception {
        Path v2 = fixture("valid-v2.json");
        JsonSerializer json = new JsonSerializer();
        assertEquals(2, json.readVersion(v2));

        Contact[] contacts = json.load(v2);

        assertEquals("2026-09-15", contacts[0].getUpdatedAt());
        assertEquals(Contact.DEFAULT_NOTE, contacts[0].getNote());
    }

    @Test
    void binaryV1MigratesToV3() throws Exception {
        Contact[] original = {
                new Contact("Анна Белова", "+79130000001", "a@mail.ru", "НГТУ", "2026-11-10", "Важный"),
                new CorporateContact("Игорь Лесной", "+79130000003", "les@corp.local", "ООО Альфа",
                        "Инженер", "204", "2026-11-12", "Отдел ИТ")
        };
        Path file = dir.resolve("v1.ctb");
        BinarySerializer binary = new BinarySerializer();
        binary.save(original, file, 1);

        assertEquals(1, binary.readVersion(file));
        Contact[] loaded = binary.load(file);

        assertEquals(2, loaded.length);
        assertEquals(Contact.DEFAULT_UPDATED_AT, loaded[0].getUpdatedAt());
        assertEquals(Contact.DEFAULT_NOTE, loaded[0].getNote());
        assertEquals("204", ((CorporateContact) loaded[1]).getInternalNumber());
    }

    @Test
    void downgradeToV2DropsNoteInJsonAndBinary() throws Exception {
        Contact[] original = {
                new Contact("Анна Белова", "+79130000001", "a@mail.ru", "НГТУ", "2026-11-10", "Секретная заметка")
        };
        Path jsonFile = dir.resolve("v2.json");
        Path binFile = dir.resolve("v2.ctb");
        JsonSerializer json = new JsonSerializer();
        BinarySerializer binary = new BinarySerializer();

        json.save(original, jsonFile, 2);
        binary.save(original, binFile, 2);

        String rawJson = Files.readString(jsonFile);
        assertTrue(rawJson.contains("\"updatedAt\""));
        assertFalse(rawJson.contains("\"note\""));

        Contact fromJson = json.load(jsonFile)[0];
        Contact fromBin = binary.load(binFile)[0];
        assertEquals("2026-11-10", fromJson.getUpdatedAt());
        assertEquals(Contact.DEFAULT_NOTE, fromJson.getNote());
        assertEquals("2026-11-10", fromBin.getUpdatedAt());
        assertEquals(Contact.DEFAULT_NOTE, fromBin.getNote());
    }

    @Test
    void downgradeToV1DropsUpdatedAtAndNote() throws Exception {
        Contact[] original = {
                new Contact("Анна Белова", "+79130000001", "a@mail.ru", "НГТУ", "2026-11-10", "Заметка")
        };
        Path jsonFile = dir.resolve("v1.json");
        JsonSerializer json = new JsonSerializer();
        json.save(original, jsonFile, 1);

        String rawJson = Files.readString(jsonFile);
        assertFalse(rawJson.contains("\"updatedAt\""));
        assertFalse(rawJson.contains("\"note\""));

        Contact loaded = json.load(jsonFile)[0];
        assertEquals(Contact.DEFAULT_UPDATED_AT, loaded.getUpdatedAt());
        assertEquals(Contact.DEFAULT_NOTE, loaded.getNote());
    }

    @Test
    void zeroOrFutureVersionIsRejected() {
        ContactFileDto zero = new ContactFileDto();
        zero.schemaVersion = 0;
        zero.contacts = new ContactDto[0];
        assertThrows(SerializationException.class, () -> ContactMigrator.migrate(zero));

        ContactFileDto future = new ContactFileDto();
        future.schemaVersion = 99;
        future.contacts = new ContactDto[0];
        assertThrows(SerializationException.class, () -> ContactMigrator.migrate(future));
    }

    @Test
    void invalidFieldInOldVersionIsRejectedOnLoad() {
        assertThrows(SerializationException.class, () -> new JsonSerializer().load(fixture("bad-number.json")));
    }

    private Path fixture(String name) throws URISyntaxException {
        return Path.of(getClass().getResource("/fixtures/" + name).toURI());
    }
}

