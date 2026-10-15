package org.example.store;

public interface PrefixStore<T> extends EntityStore<String, T> {

    T[] findByPrefix(String prefix, int limit);

    int countByPrefix(String prefix);
}
