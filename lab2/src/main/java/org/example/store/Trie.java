package org.example.store;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.IntFunction;

public class Trie<V> {

    private static class Node<V> {
        char symbol;
        V value;
        int count;
        char[] letters = new char[0];
        Node<V>[] children = newArray(0);
        int childCount;

        Node(char symbol) {
            this.symbol = symbol;
        }

        int indexOf(char c) {
            int left = 0;
            int right = childCount - 1;
            while (left <= right) {
                int mid = (left + right) / 2;
                if (letters[mid] == c) {
                    return mid;
                }
                if (letters[mid] < c) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
            return -(left + 1);
        }

        Node<V> getChild(char c) {
            int index = indexOf(c);
            return index >= 0 ? children[index] : null;
        }

        Node<V> addChild(char c) {
            int position = -(indexOf(c) + 1);
            if (childCount == letters.length) {
                int newSize = Math.max(2, childCount * 2);
                letters = Arrays.copyOf(letters, newSize);
                children = Arrays.copyOf(children, newSize);
            }
            System.arraycopy(letters, position, letters, position + 1, childCount - position);
            System.arraycopy(children, position, children, position + 1, childCount - position);
            Node<V> child = new Node<>(c);
            letters[position] = c;
            children[position] = child;
            childCount++;
            return child;
        }

        void removeChild(char c) {
            int index = indexOf(c);
            System.arraycopy(letters, index + 1, letters, index, childCount - index - 1);
            System.arraycopy(children, index + 1, children, index, childCount - index - 1);
            childCount--;
            children[childCount] = null;
        }

        @SuppressWarnings("unchecked")
        static <V> Node<V>[] newArray(int size) {
            return (Node<V>[]) new Node[size];
        }
    }

    private Node<V> root = new Node<>(' ');
    private int nodeCount = 1;

    public static String normalize(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Ключ не может быть пустым");
        }
        return key.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePrefix(String prefix) {
        return prefix == null ? "" : prefix.trim().toLowerCase(Locale.ROOT);
    }

    public boolean put(String key, V value) {
        if (value == null) {
            throw new IllegalArgumentException("Значение не может быть null");
        }
        String k = normalize(key);
        Node<V> existing = find(k);
        if (existing != null && existing.value != null) {
            return false;
        }
        Node<V> node = root;
        node.count++;
        for (int i = 0; i < k.length(); i++) {
            Node<V> next = node.getChild(k.charAt(i));
            if (next == null) {
                next = node.addChild(k.charAt(i));
                nodeCount++;
            }
            node = next;
            node.count++;
        }
        node.value = value;
        return true;
    }

    public V get(String key) {
        Node<V> node = find(normalize(key));
        return node == null ? null : node.value;
    }

    public V remove(String key) {
        String k = normalize(key);
        V old = get(k);
        if (old != null) {
            removeFrom(root, k, 0);
        }
        return old;
    }

    public int countWithPrefix(String prefix) {
        Node<V> node = find(normalizePrefix(prefix));
        return node == null ? 0 : node.count;
    }

    public V[] findWithPrefix(String prefix, int limit, IntFunction<V[]> newArray) {
        Node<V> node = find(normalizePrefix(prefix));
        if (node == null) {
            return newArray.apply(0);
        }
        V[] result = newArray.apply(Math.min(limit, node.count));
        collect(node, result, 0);
        return result;
    }

    public int size() {
        return root.count;
    }

    public int nodeCount() {
        return nodeCount;
    }

    public void clear() {
        root = new Node<>(' ');
        nodeCount = 1;
    }

    public TrieNodeView snapshot(String prefix, int maxNodes) {
        Node<V> start = find(normalizePrefix(prefix));
        if (start == null) {
            return null;
        }
        Node<V>[] queue = Node.newArray(maxNodes);
        TrieNodeView[] views = new TrieNodeView[maxNodes];
        queue[0] = start;
        views[0] = toView(start);
        int size = 1;
        for (int head = 0; head < size; head++) {
            Node<V> node = queue[head];
            for (int i = 0; i < node.childCount && size < maxNodes; i++) {
                queue[size] = node.children[i];
                views[size] = toView(node.children[i]);
                views[head].children()[i] = views[size];
                size++;
            }
        }
        return views[0];
    }

    private Node<V> find(String key) {
        Node<V> node = root;
        for (int i = 0; i < key.length() && node != null; i++) {
            node = node.getChild(key.charAt(i));
        }
        return node;
    }

    private void removeFrom(Node<V> node, String key, int depth) {
        node.count--;
        if (depth == key.length()) {
            node.value = null;
            return;
        }
        char c = key.charAt(depth);
        Node<V> child = node.getChild(c);
        removeFrom(child, key, depth + 1);
        if (child.count == 0) {
            node.removeChild(c);
            nodeCount--;
        }
    }

    private int collect(Node<V> node, V[] result, int filled) {
        if (node.value != null && filled < result.length) {
            result[filled++] = node.value;
        }
        for (int i = 0; i < node.childCount && filled < result.length; i++) {
            filled = collect(node.children[i], result, filled);
        }
        return filled;
    }

    private TrieNodeView toView(Node<V> node) {
        return new TrieNodeView(node.symbol, node.value != null, node.count,
                new TrieNodeView[node.childCount]);
    }
}
