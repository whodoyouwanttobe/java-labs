package org.example.demo;

import org.example.store.TrieNodeView;

public final class TriePrinter {

    private TriePrinter() {
    }

    public static String print(TrieNodeView root) {
        if (root == null) {
            return "(нет такого префикса)\n";
        }
        StringBuilder out = new StringBuilder();
        out.append(root.isRoot() ? "root" : "[" + root.prefix() + "]")
           .append(" (").append(root.subtreeSize()).append(")")
           .append(root.visited() ? " *" : "")
           .append('\n');
        TrieNodeView[] kids = root.children();
        for (int i = 0; i < kids.length; i++) {
            printNode(kids[i], "", i == kids.length - 1, out);
        }
        return out.toString();
    }

    private static void printNode(TrieNodeView node, String indent, boolean last, StringBuilder out) {
        StringBuilder label = new StringBuilder().append(node.symbol());
        boolean visited = node.visited();
        TrieNodeView current = node;
        while (!current.terminal() && current.children().length == 1) {
            current = current.children()[0];
            label.append(current.symbol());
            visited |= current.visited();
        }

        out.append(indent).append(last ? "└─ " : "├─ ")
           .append(label)
           .append(current.terminal() ? " $" : "")
           .append(" (").append(current.subtreeSize()).append(")")
           .append(visited ? " *" : "")
           .append(current.truncated() ? " ..." : "")
           .append('\n');

        TrieNodeView[] kids = current.children();
        String childIndent = indent + (last ? "   " : "│  ");
        for (int i = 0; i < kids.length; i++) {
            printNode(kids[i], childIndent, i == kids.length - 1, out);
        }
    }
}
