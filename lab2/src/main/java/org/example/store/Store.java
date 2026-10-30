package org.example.store;

public interface Store<T> {

    void add(T item);

    void update(String oldKey, T item);

    T remove(String key);

    T find(String key);

    boolean contains(String key);

    T[] findByPrefix(String prefix, int limit);

    int countByPrefix(String prefix);

    T[] toArray();

    int size();

    void clear();
}
