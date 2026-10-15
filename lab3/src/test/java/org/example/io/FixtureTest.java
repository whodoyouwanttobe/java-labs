package org.example.io;

import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.store.ContactStores;
import org.example.store.TrieStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FixtureTest {

    private final FileStorage<Contact> json = new FileStorage<>(new JsonContactSerializer());
    private final FileStorage<Contact> binary = new FileStorage<>(new BinaryContactSerializer());

    @Test
    void validJsonLoads() throws Exception {
        Contact[] contacts = json.read(Fixtures.path("valid.json"));
        assertEquals(4, contacts.length);
        CorporateContact igor = (CorporateContact) contacts[1];
        assertEquals("ООО \"Альфа\"; филиал", igor.getOrganization());
        assertEquals("204", igor.getInternalNumber());
    }

    @Test
    void validBinaryEqualsValidJson() throws Exception {
        assertArrayEquals(json.read(Fixtures.path("valid.json")), binary.read(Fixtures.path("valid.ctb")));
    }

    @Test
    void emptyJson() {
        assertError(json, "empty.json", SerializationError.EMPTY_FILE);
    }

    @Test
    void brokenJson() {
        SerializationException e = assertError(json, "broken.json", SerializationError.MALFORMED);
        assertTrue(e.getLocation().startsWith("строка "));
    }

    @Test
    void jsonWithoutVersion() {
        assertError(json, "no-version.json", SerializationError.MISSING_FIELD);
    }

    @Test
    void jsonFromNewerVersion() {
        assertError(json, "future-version.json", SerializationError.UNSUPPORTED_VERSION);
    }

    @Test
    void jsonWithMissingName() {
        SerializationException e = assertError(json, "missing-name.json", SerializationError.MISSING_FIELD);
        assertEquals("$.contacts[1]", e.getLocation());
    }

    @Test
    void jsonWithUnknownType() {
        assertError(json, "unknown-type.json", SerializationError.UNKNOWN_TYPE);
    }

    @Test
    void jsonWithBadInternalNumber() {
        SerializationException e = assertError(json, "bad-number.json", SerializationError.INVALID_VALUE);
        assertEquals("$.contacts[0].internal_number", e.getLocation());
    }

    @Test
    void jsonWithContactsNotArray() {
        assertError(json, "contacts-not-array.json", SerializationError.MALFORMED);
    }

    @Test
    void jsonWithDuplicateNames() {
        TrieStore<Contact> store = ContactStores.create();
        SerializationException e = assertThrows(SerializationException.class,
                () -> json.loadInto(Fixtures.path("duplicate.json"), store));
        assertEquals(SerializationError.DUPLICATE_KEY, e.getError());
    }

    @Test
    void emptyBinary() {
        assertError(binary, "empty.ctb", SerializationError.EMPTY_FILE);
    }

    @Test
    void binaryWithWrongMagic() {
        assertError(binary, "bad-magic.ctb", SerializationError.BAD_MAGIC);
    }

    @Test
    void jsonFileOpenedAsBinary() {
        assertError(binary, "valid.json", SerializationError.BAD_MAGIC);
    }

    @Test
    void binaryFromNewerVersion() {
        assertError(binary, "future-version.ctb", SerializationError.UNSUPPORTED_VERSION);
    }

    @Test
    void truncatedBinary() {
        assertError(binary, "truncated.ctb", SerializationError.TRUNCATED);
    }

    @Test
    void binaryWithUnknownTypeCode() {
        SerializationException e = assertError(binary, "unknown-type.ctb", SerializationError.UNKNOWN_TYPE);
        assertEquals("запись 0, байт 10", e.getLocation());
    }

    @Test
    void binaryWithNegativeCount() {
        assertError(binary, "bad-count.ctb", SerializationError.CORRUPTED);
    }

    @Test
    void binaryWithTrailingBytes() {
        assertError(binary, "trailing-bytes.ctb", SerializationError.CORRUPTED);
    }

    @Test
    void failedImportLeavesTargetUntouchedWhenFileIsBroken() {
        TrieStore<Contact> store = ContactStores.create();
        assertThrows(SerializationException.class, () -> binary.loadInto(Fixtures.path("truncated.ctb"), store));
        assertEquals(0, store.size());
    }

    private static SerializationException assertError(FileStorage<Contact> storage, String fixture,
                                                      SerializationError expected) {
        SerializationException e = assertThrows(SerializationException.class,
                () -> storage.read(Fixtures.path(fixture)));
        assertEquals(expected, e.getError());
        return e;
    }
}
