package org.example.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrieTest {

    private Trie<String> trie;

    @BeforeEach
    void setUp() {
        trie = new Trie<>();
        trie.put("Иван", "1");
        trie.put("Иванов", "2");
        trie.put("Ирина", "3");
        trie.put("Анна", "4");
    }

    @Test
    void getFindsExistingKey() {
        assertEquals("3", trie.get("Ирина"));
    }

    @Test
    void getIgnoresCaseAndSpaces() {
        assertEquals("2", trie.get("  иВАНОВ "));
    }

    @Test
    void getReturnsNullForPrefixWithoutValue() {
        assertNull(trie.get("Ива"));
    }

    @Test
    void putDoesNotReplaceExistingKey() {
        assertFalse(trie.put("анна", "5"));
        assertEquals("4", trie.get("Анна"));
    }

    @Test
    void emptyKeyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> trie.put("  ", "x"));
    }

    @Test
    void removeKeepsLongerKeyWithSamePrefix() {
        trie.remove("Иван");
        assertEquals("2", trie.get("Иванов"));
    }

    @Test
    void removeDeletesUnusedNodes() {
        int nodesBefore = trie.nodeCount();
        trie.remove("Анна");
        assertEquals(nodesBefore - 4, trie.nodeCount());
    }

    @Test
    void removeMissingKeyReturnsNull() {
        assertNull(trie.remove("Пётр"));
        assertEquals(4, trie.size());
    }

    @Test
    void countWithPrefixCountsWholeSubtree() {
        assertEquals(3, trie.countWithPrefix("и"));
    }

    @Test
    void countChangesAfterRemove() {
        trie.remove("Иванов");
        assertEquals(1, trie.countWithPrefix("ива"));
    }

    @Test
    void findWithPrefixReturnsAlphabeticalOrder() {
        assertArrayEquals(new String[]{"1", "2", "3"}, trie.findWithPrefix("и", 10, String[]::new));
    }

    @Test
    void findWithPrefixRespectsLimit() {
        assertEquals(2, trie.findWithPrefix("и", 2, String[]::new).length);
    }

    @Test
    void snapshotStopsAtNodeLimit() {
        TrieNodeView root = trie.snapshot("", 3);
        assertNull(root.children()[0].children()[0]);
    }

    @Test
    void snapshotOfUnknownPrefixIsNull() {
        assertNull(trie.snapshot("я", 100));
    }
}
