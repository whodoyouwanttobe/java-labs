package org.example.store;

import org.example.model.Contact;

public class ContactStore extends TrieStore<Contact> {

    public ContactStore() {
        super(Contact::getName, Contact[]::new);
    }
}
