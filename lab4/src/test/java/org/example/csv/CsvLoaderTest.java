package org.example.csv;

import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.store.ContactStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvLoaderTest {

    @TempDir
    Path dir;

    @Test
    void savedContactsAreLoadedBack() throws IOException {
        Contact[] contacts = {
                new Contact("Анна", "+7", "a@mail.ru", "НГТУ"),
                new CorporateContact("Борис", "+8", "b@mail.ru", "ООО \"Рога\"", "Инженер", "12")
        };
        Path file = dir.resolve("contacts.csv");
        new CsvSaver().save(file, contacts);

        ContactStore store = new ContactStore();
        List<CsvParseException> errors = new CsvLoader().load(file, store);

        assertTrue(errors.isEmpty());
        assertEquals(contacts[1], store.find("Борис"));
    }

    @Test
    void separatorInFieldIsNotSaved() {
        Contact[] contacts = {new Contact("Борис; мл.", "+8", "", "")};
        assertThrows(IOException.class, () -> new CsvSaver().save(dir.resolve("bad.csv"), contacts));
    }

    @Test
    void duplicateNameIsSkipped() throws IOException {
        Path file = write("Contact;Иван;+7;;НГТУ", "Contact;иван;+8;;НГТУ");
        List<CsvParseException> errors = new CsvLoader().load(file, new ContactStore());
        assertEquals(CsvErrorCode.DUPLICATE_NAME, errors.get(0).getErrorCode());
    }

    @Test
    void unknownTypeIsSkipped() throws IOException {
        Path file = write("Friend;Иван;+7;;НГТУ");
        List<CsvParseException> errors = new CsvLoader().load(file, new ContactStore());
        assertEquals(CsvErrorCode.UNKNOWN_TYPE, errors.get(0).getErrorCode());
    }

    @Test
    void lettersInInternalNumberAreError() throws IOException {
        Path file = write("Corporate;Иван;+7;;НГТУ;Инженер;12a");
        List<CsvParseException> errors = new CsvLoader().load(file, new ContactStore());
        assertEquals(CsvErrorCode.BAD_NUMBER, errors.get(0).getErrorCode());
    }

    @Test
    void progressEndsWithFileSize() throws IOException {
        Path file = write("Contact;Иван;+7;;НГТУ");
        long[] last = new long[1];
        new CsvLoader().load(file, new ContactStore(), (done, total) -> last[0] = done);
        assertEquals(Files.size(file), last[0]);
    }

    private Path write(String... lines) throws IOException {
        Path file = dir.resolve("test.csv");
        Files.writeString(file, CsvSaver.HEADER + "\n" + String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        return file;
    }
}

