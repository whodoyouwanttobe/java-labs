package org.example.store;

public record TrieNodeView(char symbol, boolean hasValue, int count, TrieNodeView[] children) {
}
