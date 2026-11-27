package org.example.api;

public record TreeNode(char symbol, boolean hasValue, int count, TreeNode[] children) {
}
