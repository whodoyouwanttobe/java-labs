package org.example.ui;

import org.example.api.ContactAlreadyExistsException;
import org.example.model.Contact;
import org.example.server.ContactService;
import org.example.store.ContactStore;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceContactFacadeTest {

    private final ContactService service = new ContactService(new ContactStore());
    private final ServiceContactFacade facade = new ServiceContactFacade(service);

    private static Contact contact(String name) {
        return new Contact(name, "+70000000000", "mail@example.com", "НГТУ");
    }

    @Test
    void addedContactIsFoundByName() {
        facade.add(contact("Анна"));

        assertTrue(facade.find("Анна").isPresent());
        assertEquals(1, facade.size());
        assertFalse(facade.isEmpty());
    }

    @Test
    void duplicateAddThrowsPortException() {
        facade.add(contact("Анна"));

        assertThrows(ContactAlreadyExistsException.class, () -> facade.add(contact("Анна")));
        assertEquals(1, facade.size());
    }

    @Test
    void findAllAndCountFilterByPrefix() {
        facade.add(contact("Анна"));
        facade.add(contact("Антон"));
        facade.add(contact("Борис"));

        assertEquals(2, facade.count("Ан"));
        assertEquals(2, facade.findAll("Ан").length);
        assertEquals(3, facade.all().length);
    }

    @Test
    void replaceAllReplacesContentAndFiresSingleEvent() {
        facade.add(contact("Анна"));
        AtomicInteger events = new AtomicInteger();
        facade.addChangeListener(events::incrementAndGet);

        facade.replaceAll(new Contact[] {contact("Вера")});

        assertEquals(1, facade.size());
        assertTrue(facade.find("Анна").isEmpty());
        assertTrue(facade.find("Вера").isPresent());
        assertEquals(1, events.get());
    }

    @Test
    void mutationsNotifyListener() {
        AtomicInteger events = new AtomicInteger();
        facade.addChangeListener(events::incrementAndGet);

        facade.add(contact("Анна"));
        facade.remove("Анна");

        assertEquals(2, events.get());
    }

    @Test
    void snapshotIsNullForUnknownPrefix() {
        facade.add(contact("Анна"));

        assertNull(facade.snapshot("ъъъ", 300));
        assertNotNull(facade.snapshot("", 300));
    }
}
