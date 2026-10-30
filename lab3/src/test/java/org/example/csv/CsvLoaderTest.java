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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CsvLoaderTest {

    @TempDir
    Path dir;

    @Test
    void savedContactsAreLoadedBack() throws IOException {
        Contact[] contacts = {
                new Contact("Анна", "+7", "a@mail.ru", "НГТУ"),
                new CorporateContact("Борис; мл.", "+8", "b@mail.ru", "ООО \"Рога\"", "Инженер", "12")
        };
        Path file = dir.resolve("contacts.csv");
        new CsvSaver().save(file, contacts);

        ContactStore store = new ContactStore();
        LoadResult result = new CsvLoader().load(file, store);

        assertFalse(result.hasErrors());
        assertEquals(contacts[1], store.find("Борис; мл."));
    }

    @Test
    void duplicateNameIsSkipped() throws IOException {
        Path file = write("Contact;Иван;+7;;НГТУ", "Contact;иван;+8;;НГТУ");
        LoadResult result = new CsvLoader().load(file, new ContactStore());
        assertEquals(CsvErrorCode.DUPLICATE_NAME, result.getErrors().get(0).getErrorCode());
    }

    @Test
    void unknownTypeIsSkipped() throws IOException {
        Path file = write("Friend;Иван;+7;;НГТУ");
        LoadResult result = new CsvLoader().load(file, new ContactStore());
        assertEquals(CsvErrorCode.UNKNOWN_TYPE, result.getErrors().get(0).getErrorCode());
    }

    @Test
    void lettersInInternalNumberAreError() throws IOException {
        Path file = write("Corporate;Иван;+7;;НГТУ;Инженер;12a");
        LoadResult result = new CsvLoader().load(file, new ContactStore());
        assertEquals(CsvErrorCode.BAD_NUMBER, result.getErrors().get(0).getErrorCode());
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
