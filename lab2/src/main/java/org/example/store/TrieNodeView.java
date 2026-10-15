package org.example.store;

public record TrieNodeView(char symbol,
                           String prefix,
                           boolean terminal,
                           int subtreeSize,
                           boolean visited,
                           boolean truncated,
                           TrieNodeView[] children) {

    public boolean isRoot() {
        return prefix.isEmpty();
    }

    public String label() {
        return isRoot() ? "root" : String.valueOf(symbol);
    }
}
