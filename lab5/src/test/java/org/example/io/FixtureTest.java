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
    void emptyJsonIsRejected() {
        assertThrows(SerializationException.class, () -> json.load(fixture("empty.json")));
    }

    @Test
    void brokenJsonIsRejected() {
        assertThrows(SerializationException.class, () -> json.load(fixture("broken.json")));
    }

    @Test
    void jsonFromNewerVersionIsRejected() {
        assertThrows(SerializationException.class, () -> json.load(fixture("future-version.json")));
    }

    @Test
    void contactWithoutNameIsRejected() {
        assertThrows(SerializationException.class, () -> json.load(fixture("missing-name.json")));
    }

    @Test
    void unknownTypeIsRejected() {
        assertThrows(SerializationException.class, () -> json.load(fixture("unknown-type.json")));
    }

    @Test
    void lettersInInternalNumberAreRejected() {
        assertThrows(SerializationException.class, () -> json.load(fixture("bad-number.json")));
    }

    @Test
    void duplicateNamesAreRejectedByStore() throws Exception {
        Contact[] contacts = json.load(fixture("duplicate.json"));
        ContactStore store = new ContactStore();
        store.add(contacts[0]);
        assertThrows(DuplicateKeyException.class, () -> store.add(contacts[1]));
    }

    @Test
    void emptyBinaryIsRejected() {
        assertThrows(SerializationException.class, () -> binary.load(fixture("empty.ctb")));
    }

    @Test
    void binaryWithWrongSignatureIsRejected() {
        assertThrows(SerializationException.class, () -> binary.load(fixture("bad-signature.ctb")));
    }

    @Test
    void jsonOpenedAsBinaryIsRejected() {
        assertThrows(SerializationException.class, () -> binary.load(fixture("valid.json")));
    }

    @Test
    void binaryFromNewerVersionIsRejected() {
        assertThrows(SerializationException.class, () -> binary.load(fixture("future-version.ctb")));
    }

    @Test
    void truncatedBinaryIsRejected() {
        assertThrows(SerializationException.class, () -> binary.load(fixture("truncated.ctb")));
    }

    private Path fixture(String name) throws URISyntaxException {
        return Path.of(getClass().getResource("/fixtures/" + name).toURI());
    }
}

