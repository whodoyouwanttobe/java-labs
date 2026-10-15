package org.example.store;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

public class Trie<V> {

    private static final class Node<V> {
        final char symbol;
        final Node<V> parent;

        char[] symbols = new char[0];
        Node<V>[] children = newNodes(0);
        int childCount;

        V value;
        int subtreeSize;

        Node(char symbol, Node<V> parent) {
            this.symbol = symbol;
            this.parent = parent;
        }

        int indexOf(char c) {
            int lo = 0;
            int hi = childCount - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (symbols[mid] < c) {
                    lo = mid + 1;
                } else if (symbols[mid] > c) {
                    hi = mid - 1;
                } else {
                    return mid;
                }
            }
            return -(lo + 1);
        }

        Node<V> child(char c) {
            int i = indexOf(c);
            return i >= 0 ? children[i] : null;
        }

        Node<V> addChild(char c) {
            int i = indexOf(c);
            if (i >= 0) {
                return children[i];
            }
            int pos = -(i + 1);
            if (childCount == children.length) {
                int newCapacity = childCount == 0 ? 2 : childCount * 2;
                char[] newSymbols = new char[newCapacity];
                Node<V>[] newChildren = newNodes(newCapacity);
                System.arraycopy(symbols, 0, newSymbols, 0, childCount);
                System.arraycopy(children, 0, newChildren, 0, childCount);
                symbols = newSymbols;
                children = newChildren;
            }
            System.arraycopy(symbols, pos, symbols, pos + 1, childCount - pos);
            System.arraycopy(children, pos, children, pos + 1, childCount - pos);
            Node<V> node = new Node<>(c, this);
            symbols[pos] = c;
            children[pos] = node;
            childCount++;
            return node;
        }

        void removeChild(char c) {
            int i = indexOf(c);
            if (i < 0) {
                return;
            }
            System.arraycopy(symbols, i + 1, symbols, i, childCount - i - 1);
            System.arraycopy(children, i + 1, children, i, childCount - i - 1);
            childCount--;
            children[childCount] = null;
        }

        @SuppressWarnings("unchecked")
        private static <V> Node<V>[] newNodes(int size) {
            return (Node<V>[]) new Node[size];
        }
    }

    private Node<V> root = new Node<>('\0', null);
    private int nodeCount = 1;

    public static String normalize(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Ключ не может быть null");
        }
        String result = key.trim().toLowerCase(Locale.ROOT);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Ключ не может быть пустым");
        }
        return result;
    }

    private static String normalizePrefix(String prefix) {
        return prefix == null ? "" : prefix.trim().toLowerCase(Locale.ROOT);
    }

    public int size() {
        return root.subtreeSize;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int nodeCount() {
        return nodeCount;
    }

    public void clear() {
        root = new Node<>('\0', null);
        nodeCount = 1;
    }

    public V get(String key) {
        Node<V> node = find(normalize(key));
        return node == null ? null : node.value;
    }

    public boolean containsKey(String key) {
        return get(key) != null;
    }

    public V put(String key, V value) {
        return insert(key, value, true);
    }

    public boolean putIfAbsent(String key, V value) {
        return insert(key, value, false) == null;
    }

    public V remove(String key) {
        Node<V> node = find(normalize(key));
        if (node == null || node.value == null) {
            return null;
        }
        V old = node.value;
        node.value = null;
        for (Node<V> p = node; p != null; p = p.parent) {
            p.subtreeSize--;
        }
        Node<V> current = node;
        while (current != root && current.value == null && current.childCount == 0) {
            Node<V> parent = current.parent;
            parent.removeChild(current.symbol);
            nodeCount--;
            current = parent;
        }
        return old;
    }

    public int countWithPrefix(String prefix) {
        Node<V> node = find(normalizePrefix(prefix));
        return node == null ? 0 : node.subtreeSize;
    }

    public V[] findWithPrefix(String prefix, int limit, IntFunction<V[]> arrayFactory) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit < 0");
        }
        Node<V> node = find(normalizePrefix(prefix));
        if (node == null) {
            return arrayFactory.apply(0);
        }
        V[] result = arrayFactory.apply(Math.min(limit, node.subtreeSize));
        collect(node, result, 0);
        return result;
    }

    public V[] values(IntFunction<V[]> arrayFactory) {
        V[] result = arrayFactory.apply(size());
        collect(root, result, 0);
        return result;
    }

    public void forEach(BiConsumer<String, ? super V> action) {
        walk(root, new StringBuilder(), action);
    }

    public TrieNodeView snapshot(int maxDepth) {
        return snapshot("", maxDepth, nodeCount, null);
    }

    public TrieNodeView snapshot(String prefix, int maxDepth, int maxNodes, String highlightKey) {
        String start = normalizePrefix(prefix);
        Node<V> startNode = find(start);
        if (startNode == null) {
            return null;
        }
        String highlight = highlightKey == null ? null : normalizePrefix(highlightKey);
        return new SnapshotBuilder<>(startNode, start, highlight,
                maxDepth, Math.max(1, Math.min(maxNodes, nodeCount))).build();
    }

    private V insert(String key, V value, boolean replace) {
        if (value == null) {
            throw new IllegalArgumentException("Значение не может быть null");
        }
        String k = normalize(key);
        Node<V> node = root;
        for (int i = 0; i < k.length(); i++) {
            int before = node.childCount;
            node = node.addChild(k.charAt(i));
            if (node.parent.childCount > before) {
                nodeCount++;
            }
        }
        V old = node.value;
        if (old == null) {
            node.value = value;
            for (Node<V> p = node; p != null; p = p.parent) {
                p.subtreeSize++;
            }
        } else if (replace) {
            node.value = value;
        }
        return old;
    }

    private Node<V> find(String k) {
        Node<V> node = root;
        for (int i = 0; i < k.length() && node != null; i++) {
            node = node.child(k.charAt(i));
        }
        return node;
    }

    private int collect(Node<V> node, V[] out, int filled) {
        if (filled == out.length) {
            return filled;
        }
        if (node.value != null) {
            out[filled++] = node.value;
        }
        for (int i = 0; i < node.childCount && filled < out.length; i++) {
            filled = collect(node.children[i], out, filled);
        }
        return filled;
    }

    private void walk(Node<V> node, StringBuilder path, BiConsumer<String, ? super V> action) {
        if (node.value != null) {
            action.accept(path.toString(), node.value);
        }
        for (int i = 0; i < node.childCount; i++) {
            path.append(node.symbols[i]);
            walk(node.children[i], path, action);
            path.setLength(path.length() - 1);
        }
    }

    private static final class SnapshotBuilder<V> {
        private final String highlight;
        private final int maxDepth;
        private final Node<V>[] nodes;
        private final String[] prefixes;
        private final int[] parents;
        private final int[] depths;
        private final int[] firstChild;
        private final int[] includedChildren;
        private int count;

        @SuppressWarnings("unchecked")
        SnapshotBuilder(Node<V> start, String startPrefix, String highlight, int maxDepth, int maxNodes) {
            this.highlight = highlight;
            this.maxDepth = maxDepth;
            this.nodes = (Node<V>[]) new Node[maxNodes];
            this.prefixes = new String[maxNodes];
            this.parents = new int[maxNodes];
            this.depths = new int[maxNodes];
            this.firstChild = new int[maxNodes];
            this.includedChildren = new int[maxNodes];
            nodes[0] = start;
            prefixes[0] = startPrefix;
            parents[0] = -1;
            count = 1;
        }

        TrieNodeView build() {
            collectBreadthFirst();
            TrieNodeView[] views = new TrieNodeView[count];
            TrieNodeView[][] kids = new TrieNodeView[count][];
            for (int i = 0; i < count; i++) {
                kids[i] = new TrieNodeView[includedChildren[i]];
            }
            for (int i = count - 1; i >= 0; i--) {
                Node<V> node = nodes[i];
                views[i] = new TrieNodeView(node.symbol, prefixes[i], node.value != null, node.subtreeSize,
                        isOnPath(prefixes[i]), includedChildren[i] < node.childCount, kids[i]);
                int parent = parents[i];
                if (parent >= 0) {
                    kids[parent][i - firstChild[parent]] = views[i];
                }
            }
            return views[0];
        }

        private void collectBreadthFirst() {
            for (int head = 0; head < count; head++) {
                Node<V> node = nodes[head];
                if (depths[head] >= maxDepth) {
                    continue;
                }
                for (int c = 0; c < node.childCount; c++) {
                    if (count == nodes.length) {
                        return;
                    }
                    if (includedChildren[head] == 0) {
                        firstChild[head] = count;
                    }
                    includedChildren[head]++;
                    nodes[count] = node.children[c];
                    prefixes[count] = prefixes[head] + node.symbols[c];
                    parents[count] = head;
                    depths[count] = depths[head] + 1;
                    count++;
                }
            }
        }

        private boolean isOnPath(String prefix) {
            return highlight != null && highlight.startsWith(prefix);
        }
    }
}
