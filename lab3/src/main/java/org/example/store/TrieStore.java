package org.example.store;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class TrieStore<T> implements PrefixStore<T> {

    private final Trie<T> trie = new Trie<>();
    private final Function<? super T, String> keyOf;
    private final IntFunction<T[]> arrayFactory;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public TrieStore(Function<? super T, String> keyOf, IntFunction<T[]> arrayFactory) {
        this.keyOf = keyOf;
        this.arrayFactory = arrayFactory;
    }

    @Override
    public void add(T entity) {
        if (!tryAdd(entity)) {
            throw new DuplicateKeyException(keyOf.apply(entity));
        }
    }

    @Override
    public boolean tryAdd(T entity) {
        String key = keyOf.apply(entity);
        return write(() -> trie.putIfAbsent(key, entity));
    }

    @Override
    public T findByKey(String key) {
        return read(() -> trie.get(key));
    }

    @Override
    public boolean removeByKey(String key) {
        return write(() -> trie.remove(key) != null);
    }

    @Override
    public void update(String oldKey, T entity) {
        String oldNorm = Trie.normalize(oldKey);
        String newNorm = Trie.normalize(keyOf.apply(entity));
        Lock w = lock.writeLock();
        w.lock();
        try {
            if (!trie.containsKey(oldNorm)) {
                throw new KeyNotFoundException(oldKey);
            }
            if (!oldNorm.equals(newNorm) && trie.containsKey(newNorm)) {
                throw new DuplicateKeyException(keyOf.apply(entity));
            }
            trie.remove(oldNorm);
            trie.put(newNorm, entity);
        } finally {
            w.unlock();
        }
    }

    @Override
    public int size() {
        return read(trie::size);
    }

    @Override
    public void clear() {
        write(() -> {
            trie.clear();
            return null;
        });
    }

    @Override
    public T[] toArray() {
        return read(() -> trie.values(arrayFactory));
    }

    @Override
    public T[] findByPrefix(String prefix, int limit) {
        return read(() -> trie.findWithPrefix(prefix, limit, arrayFactory));
    }

    @Override
    public int countByPrefix(String prefix) {
        return read(() -> trie.countWithPrefix(prefix));
    }

    public int nodeCount() {
        return read(trie::nodeCount);
    }

    public TrieNodeView snapshot(int maxDepth) {
        return read(() -> trie.snapshot(maxDepth));
    }

    public TrieNodeView snapshot(String prefix, int maxDepth, int maxNodes, String highlightKey) {
        return read(() -> trie.snapshot(prefix, maxDepth, maxNodes, highlightKey));
    }

    private <R> R read(Supplier<R> action) {
        Lock r = lock.readLock();
        r.lock();
        try {
            return action.get();
        } finally {
            r.unlock();
        }
    }

    private <R> R write(Supplier<R> action) {
        Lock w = lock.writeLock();
        w.lock();
        try {
            return action.get();
        } finally {
            w.unlock();
        }
    }
}
