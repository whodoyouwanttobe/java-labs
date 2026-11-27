package org.example.store;

public class TrieNodeView {
    public final char symbol;
    public final boolean hasValue;
    public final int count;
    public final TrieNodeView[] children;

    TrieNodeView(char symbol, boolean hasValue, int count, int childCount) {
        this.symbol = symbol;
        this.hasValue = hasValue;
        this.count = count;
        this.children = new TrieNodeView[childCount];
    }
}

