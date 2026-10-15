package org.example.store;

import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrieStoreTest {

    private TrieStore<Contact> store;

    @BeforeEach
    void setUp() {
        store = ContactStores.create();
        store.add(new Contact("Иван Петров", "+7913", "ivan@mail.ru", "НГТУ"));
        store.add(new CorporateContact("Ирина Котова", "+7914", "kot@corp.local", "ООО Альфа", "Инженер", "101"));
    }

    @Test
    void addDuplicateThrows() {
        DuplicateKeyException e = assertThrows(DuplicateKeyException.class,
                () -> store.add(new Contact("иван петров", "", "", "")));
        assertEquals("иван петров", e.getKey());
    }

    @Test
    void tryAddDuplicateReturnsFalse() {
        assertFalse(store.tryAdd(new Contact("Иван Петров", "", "", "")));
        assertEquals(2, store.size());
    }

    @Test
    void updateMovesContactToNewName() {
        Contact renamed = store.findByKey("Иван Петров").withName("Иван Сидоров");
        store.update("Иван Петров", renamed);
        assertSame(renamed, store.findByKey("Иван Сидоров"));
        assertNull(store.findByKey("Иван Петров"));
    }

    @Test
    void updateKeepsSameNameWithNewData() {
        Contact changed = new Contact("Иван Петров", "+7000", "new@mail.ru", "НГТУ");
        store.update("иван петров", changed);
        assertSame(changed, store.findByKey("Иван Петров"));
    }

    @Test
    void updateToOccupiedNameThrowsDuplicate() {
        Contact c = store.findByKey("Иван Петров").withName("Ирина Котова");
        assertThrows(DuplicateKeyException.class, () -> store.update("Иван Петров", c));
        assertEquals(2, store.size());
    }

    @Test
    void updateOfMissingKeyThrowsNotFound() {
        Contact c = new Contact("Пётр", "", "", "");
        KeyNotFoundException e = assertThrows(KeyNotFoundException.class, () -> store.update("Пётр", c));
        assertEquals("Пётр", e.getKey());
    }

    @Test
    void withNameKeepsCorporateFields() {
        CorporateContact c = (CorporateContact) store.findByKey("Ирина Котова").withName("Ирина Белова");
        assertEquals("101", c.getInternalNumber());
    }

    @Test
    void toArrayIsSortedByName() {
        Contact[] all = store.toArray();
        assertEquals("Иван Петров", all[0].getName());
    }

    @Test
    void clearEmptiesStore() {
        store.clear();
        assertEquals(0, store.size());
    }
}
