package org.example.store;

import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContactStoreTest {

    private ContactStore store;

    @BeforeEach
    void setUp() {
        store = new ContactStore();
        store.add(new Contact("Иван Петров", "+7913", "ivan@mail.ru", "НГТУ"));
        store.add(new CorporateContact("Ирина Котова", "+7914", "kot@corp.local", "ООО Альфа", "Инженер", "101"));
    }

    @Test
    void addDuplicateThrows() {
        assertThrows(DuplicateKeyException.class, () -> store.add(new Contact("иван петров", "", "", "")));
        assertEquals(2, store.size());
    }

    @Test
    void updateMovesContactToNewName() {
        Contact renamed = new Contact("Иван Сидоров", "+7913", "ivan@mail.ru", "НГТУ");
        store.update("Иван Петров", renamed);
        assertSame(renamed, store.find("Иван Сидоров"));
        assertNull(store.find("Иван Петров"));
    }

    @Test
    void updateWithSameNameReplacesData() {
        Contact changed = new Contact("Иван Петров", "+7000", "new@mail.ru", "НГТУ");
        store.update("иван петров", changed);
        assertSame(changed, store.find("Иван Петров"));
    }

    @Test
    void updateToTakenNameThrows() {
        Contact contact = new Contact("Ирина Котова", "", "", "");
        assertThrows(DuplicateKeyException.class, () -> store.update("Иван Петров", contact));
    }

    @Test
    void updateOfMissingContactThrows() {
        Contact contact = new Contact("Пётр", "", "", "");
        assertThrows(KeyNotFoundException.class, () -> store.update("Пётр", contact));
    }

    @Test
    void toArrayIsSortedByName() {
        assertEquals("Иван Петров", store.toArray()[0].getName());
    }
}
