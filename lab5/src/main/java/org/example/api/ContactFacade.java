package org.example.api;

import org.example.model.Contact;

import java.util.Optional;

public interface ContactFacade {

    boolean isEmpty();

    int size();

    int count(String prefix);

    int nodeCount();

    Contact[] findAll(String prefix);

    Contact[] all();

    Optional<Contact> find(String name);

    TreeNode snapshot(String prefix, int maxNodes);

    void add(Contact contact);

    void update(String oldKey, Contact contact);

    void remove(String name);

    void clear();

    void replaceAll(Contact[] contacts);

    void addChangeListener(Runnable listener);
}
