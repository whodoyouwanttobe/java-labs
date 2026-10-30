package org.example.store;

import java.util.function.Function;
import java.util.function.IntFunction;

public class TrieStore<T> implements Store<T> {

    private final Trie<T> trie = new Trie<>();
    private final Function<T, String> keyOf;
    private final IntFunction<T[]> newArray;

    public TrieStore(Function<T, String> keyOf, IntFunction<T[]> newArray) {
        this.keyOf = keyOf;
        this.newArray = newArray;
    }

    @Override
    public synchronized void add(T item) {
        String key = keyOf.apply(item);
        if (!trie.put(key, item)) {
            throw new DuplicateKeyException(key);
        }
    }

    @Override
    public synchronized void update(String oldKey, T item) {
        String newKey = keyOf.apply(item);
        if (trie.get(oldKey) == null) {
            throw new KeyNotFoundException(oldKey);
        }
        boolean renamed = !Trie.normalize(oldKey).equals(Trie.normalize(newKey));
        if (renamed && trie.get(newKey) != null) {
            throw new DuplicateKeyException(newKey);
        }
        trie.remove(oldKey);
        trie.put(newKey, item);
    }

    @Override
    public synchronized T remove(String key) {
        return trie.remove(key);
    }

    @Override
    public synchronized T find(String key) {
        return trie.get(key);
    }

    @Override
    public synchronized boolean contains(String key) {
        return trie.get(key) != null;
    }

    @Override
    public synchronized T[] findByPrefix(String prefix, int limit) {
        return trie.findWithPrefix(prefix, limit, newArray);
    }

    @Override
    public synchronized int countByPrefix(String prefix) {
        return trie.countWithPrefix(prefix);
    }

    @Override
    public synchronized T[] toArray() {
        return trie.findWithPrefix("", trie.size(), newArray);
    }

    @Override
    public synchronized int size() {
        return trie.size();
    }

    @Override
    public synchronized void clear() {
        trie.clear();
    }

    public synchronized int nodeCount() {
        return trie.nodeCount();
    }

    public synchronized TrieNodeView snapshot(String prefix, int maxNodes) {
        return trie.snapshot(prefix, maxNodes);
    }
}
