package org.example.store;

import org.example.model.Contact;

public final class ContactStores {

    private ContactStores() {
    }

    public static TrieStore<Contact> create() {
        return new TrieStore<>(Contact::getName, Contact[]::new);
    }
}
