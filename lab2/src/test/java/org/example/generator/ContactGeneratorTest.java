package org.example.generator;

import org.example.model.Contact;
import org.example.store.ContactStores;
import org.example.store.TrieStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContactGeneratorTest {

    @Test
    void generatesExactCountEvenWithRepeatedNames() {
        TrieStore<Contact> store = ContactStores.create();
        new ContactGenerator(1).generateInto(store, 20_000);
        assertEquals(20_000, store.size());
    }

    @Test
    void stopsWhenCancelled() {
        TrieStore<Contact> store = ContactStores.create();
        int added = new ContactGenerator(1).generateInto(store, 1000, (d, t) -> { }, () -> store.size() >= 10);
        assertEquals(10, added);
    }
}
