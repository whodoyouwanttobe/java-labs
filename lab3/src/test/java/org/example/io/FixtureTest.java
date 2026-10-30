package org.example.io;

import org.example.model.Contact;
import org.example.store.ContactStore;
import org.example.store.DuplicateKeyException;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FixtureTest {

    private final Serializer json = new JsonSerializer();
    private final Serializer binary = new BinarySerializer();

    @Test
    void validJsonIsLoaded() throws Exception {
        assertEquals(4, json.load(fixture("valid.json")).length);
    }

    @Test
    void validBinaryEqualsValidJson() throws Exception {
        assertArrayEquals(json.load(fixture("valid.json")), binary.load(fixture("valid.ctb")));
    }

    @Test
    void emptyJson() {
        assertError(json, "empty.json", SerializationError.EMPTY_FILE);
    }

    @Test
    void brokenJson() {
        assertError(json, "broken.json", SerializationError.BAD_FORMAT);
    }

    @Test
    void contactsNotArray() {
        assertError(json, "contacts-not-array.json", SerializationError.BAD_FORMAT);
    }

    @Test
    void jsonWithoutVersion() {
        assertError(json, "no-version.json", SerializationError.MISSING_FIELD);
    }

    @Test
    void jsonFromNewerVersion() {
        assertError(json, "future-version.json", SerializationError.BAD_VERSION);
    }

    @Test
    void contactWithoutName() {
        assertError(json, "missing-name.json", SerializationError.MISSING_FIELD);
    }

    @Test
    void unknownContactType() {
        assertError(json, "unknown-type.json", SerializationError.UNKNOWN_TYPE);
    }

    @Test
    void lettersInInternalNumber() {
        assertError(json, "bad-number.json", SerializationError.BAD_VALUE);
    }

    @Test
    void duplicateNamesAreRejectedByStore() throws Exception {
        Contact[] contacts = json.load(fixture("duplicate.json"));
        ContactStore store = new ContactStore();
        store.add(contacts[0]);
        assertThrows(DuplicateKeyException.class, () -> store.add(contacts[1]));
    }

    @Test
    void emptyBinary() {
        assertError(binary, "empty.ctb", SerializationError.EMPTY_FILE);
    }

    @Test
    void binaryWithWrongSignature() {
        assertError(binary, "bad-signature.ctb", SerializationError.BAD_FORMAT);
    }

    @Test
    void jsonOpenedAsBinary() {
        assertError(binary, "valid.json", SerializationError.BAD_FORMAT);
    }

    @Test
    void binaryFromNewerVersion() {
        assertError(binary, "future-version.ctb", SerializationError.BAD_VERSION);
    }

    @Test
    void truncatedBinary() {
        assertError(binary, "truncated.ctb", SerializationError.TRUNCATED);
    }

    @Test
    void binaryWithNegativeCount() {
        assertError(binary, "bad-count.ctb", SerializationError.BAD_FORMAT);
    }

    @Test
    void binaryWithExtraBytes() {
        assertError(binary, "extra-bytes.ctb", SerializationError.BAD_FORMAT);
    }

    private void assertError(Serializer serializer, String name, SerializationError expected) {
        SerializationException e = assertThrows(SerializationException.class,
                () -> serializer.load(fixture(name)));
        assertEquals(expected, e.getError());
    }

    private Path fixture(String name) throws URISyntaxException {
        return Path.of(getClass().getResource("/fixtures/" + name).toURI());
    }
}
