package org.example.store;

import org.example.model.Contact;

public class ContactStore {

    private final Trie<Contact> trie = new Trie<>();

    public synchronized void add(Contact contact) {
        if (!trie.put(contact.getName(), contact)) {
            throw new DuplicateKeyException(contact.getName());
        }
    }

    public synchronized void update(String oldName, Contact contact) {
        boolean renamed = !Trie.normalize(oldName).equals(Trie.normalize(contact.getName()));
        if (renamed && trie.get(contact.getName()) != null) {
            throw new DuplicateKeyException(contact.getName());
        }
        trie.remove(oldName);
        trie.put(contact.getName(), contact);
    }

    public synchronized Contact remove(String name) {
        return trie.remove(name);
    }

    public synchronized Contact find(String name) {
        return trie.get(name);
    }

    public synchronized boolean contains(String name) {
        return trie.get(name) != null;
    }

    public synchronized Contact[] findByPrefix(String prefix, int limit) {
        Contact[] result = new Contact[Math.min(limit, trie.countWithPrefix(prefix))];
        trie.collect(prefix, result);
        return result;
    }

    public synchronized int countByPrefix(String prefix) {
        return trie.countWithPrefix(prefix);
    }

    public synchronized Contact[] toArray() {
        return findByPrefix("", trie.size());
    }

    public synchronized int size() {
        return trie.size();
    }

    public synchronized int nodeCount() {
        return trie.nodeCount();
    }

    public synchronized TrieNodeView snapshot(String prefix, int maxNodes) {
        return trie.snapshot(prefix, maxNodes);
    }
}

