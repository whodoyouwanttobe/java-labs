package org.example.csv;

import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.store.ContactStores;
import org.example.store.TrieStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CsvFormatTest {

    @TempDir
    Path dir;

    @Test
    void plainLineSplitsBySemicolon() throws CsvParseException {
        assertArrayEquals(new String[]{"a", "b", "", "c"}, CsvFormat.split("a;b;;c", 1));
    }

    @Test
    void quotedFieldKeepsSemicolon() throws CsvParseException {
        assertArrayEquals(new String[]{"Contact", "ООО Рога; Копыта", "x"},
                CsvFormat.split("Contact;\"ООО Рога; Копыта\";x", 1));
    }

    @Test
    void doubledQuoteBecomesOneQuote() throws CsvParseException {
        assertArrayEquals(new String[]{"ООО \"Альфа\""}, CsvFormat.split("\"ООО \"\"Альфа\"\"\"", 1));
    }

    @Test
    void unclosedQuoteIsError() {
        CsvParseException e = assertThrows(CsvParseException.class, () -> CsvFormat.split("a;\"bc;d", 7));
        assertEquals(CsvErrorCode.BAD_QUOTES, e.getErrorCode());
        assertEquals(7, e.getLineNumber());
    }

    @Test
    void textAfterClosingQuoteIsError() {
        CsvParseException e = assertThrows(CsvParseException.class, () -> CsvFormat.split("\"ab\"c;d", 3));
        assertEquals(CsvErrorCode.BAD_QUOTES, e.getErrorCode());
    }

    @Test
    void escapeLeavesPlainTextAlone() {
        assertEquals("НГТУ", CsvFormat.escape("НГТУ"));
    }

    @Test
    void roundTripWithSeparatorAndQuotesInFields() throws IOException {
        TrieStore<Contact> store = ContactStores.create();
        CorporateContact tricky = new CorporateContact("Анна; Белова", "+7", "a@mail.ru",
                "ООО \"Рога и копыта\"", "Инженер; ведущий", "12");
        store.add(tricky);
        Path file = dir.resolve("q.csv");
        new CsvSaver().save(file, store);

        TrieStore<Contact> loaded = ContactStores.create();
        LoadResult result = new CsvLoader().load(file, loaded);

        assertFalse(result.hasErrors());
        assertEquals(tricky, loaded.findByKey("Анна; Белова"));
    }
}
