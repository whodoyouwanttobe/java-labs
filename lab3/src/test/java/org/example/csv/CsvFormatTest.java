package org.example.csv;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvFormatTest {

    @Test
    void lineIsSplitBySemicolon() throws CsvParseException {
        assertArrayEquals(new String[]{"a", "b", "", "c"}, CsvFormat.split("a;b;;c", 1));
    }

    @Test
    void quotedFieldKeepsSemicolon() throws CsvParseException {
        assertArrayEquals(new String[]{"Contact", "ООО Рога; Копыта"},
                CsvFormat.split("Contact;\"ООО Рога; Копыта\"", 1));
    }

    @Test
    void doubledQuoteBecomesOneQuote() throws CsvParseException {
        assertArrayEquals(new String[]{"ООО \"Альфа\""}, CsvFormat.split("\"ООО \"\"Альфа\"\"\"", 1));
    }

    @Test
    void unclosedQuoteIsError() {
        CsvParseException e = assertThrows(CsvParseException.class, () -> CsvFormat.split("a;\"bc;d", 7));
        assertEquals(CsvErrorCode.BAD_QUOTES, e.getErrorCode());
    }

    @Test
    void fieldWithSemicolonIsQuoted() {
        assertEquals("\"a;b\"", CsvFormat.escape("a;b"));
    }
}
