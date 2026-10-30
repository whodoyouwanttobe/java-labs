package org.example.generator;

import org.example.store.ContactStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContactGeneratorTest {

    @Test
    void generatesExactCountEvenWithRepeatedNames() {
        ContactStore store = new ContactStore();
        new ContactGenerator(1).fill(store, 20_000);
        assertEquals(20_000, store.size());
    }
}
