package org.example.store;

public interface EntityStore<K, T> {

    void add(T entity);

    boolean tryAdd(T entity);

    T findByKey(K key);

    boolean removeByKey(K key);

    void update(K oldKey, T entity);

    int size();

    void clear();

    T[] toArray();
}
