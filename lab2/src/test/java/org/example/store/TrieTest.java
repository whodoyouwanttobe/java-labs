package org.example.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
    void putIfAbsentRejectsDuplicate() {
        assertFalse(trie.putIfAbsent("анна", "5"));
        assertEquals("4", trie.get("Анна"));
    }

    @Test
    void putReplacesValueWithoutChangingSize() {
        trie.put("Анна", "5");
        assertEquals(4, trie.size());
    }

    @Test
    void removeKeepsLongerKeyWithSamePrefix() {
        trie.remove("Иван");
        assertEquals("2", trie.get("Иванов"));
    }

    @Test
    void removePrunesUnusedNodes() {
        int nodesBefore = trie.nodeCount();
        trie.remove("Анна");
        assertEquals(nodesBefore - 4, trie.nodeCount());
    }

    @Test
    void removeMissingKeyReturnsNull() {
        assertNull(trie.remove("Пётр"));
    }

    @Test
    void countWithPrefixCountsWholeSubtree() {
        assertEquals(3, trie.countWithPrefix("и"));
    }

    @Test
    void countUpdatesAfterRemove() {
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
    void findWithUnknownPrefixIsEmpty() {
        assertEquals(0, trie.findWithPrefix("я", 10, String[]::new).length);
    }

    @Test
    void emptyKeyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> trie.put("  ", "x"));
    }

    @Test
    void readsDoNotChangeSnapshot() {
        trie.get("Анна");
        trie.findWithPrefix("и", 10, String[]::new);
        assertFalse(trie.snapshot(10).visited());
    }

    @Test
    void snapshotHighlightsPathOfGivenKey() {
        TrieNodeView a = trie.snapshot("", 10, 100, "Анна").children()[0];
        assertTrue(a.visited());
    }

    @Test
    void snapshotDoesNotHighlightOtherBranches() {
        TrieNodeView i = trie.snapshot("", 10, 100, "Анна").children()[1];
        assertFalse(i.visited());
    }

    @Test
    void snapshotHighlightsExistingPartOfMissingKey() {
        TrieNodeView root = trie.snapshot("", 10, 100, "Ивар");
        TrieNodeView iva = root.children()[1].children()[0].children()[0];
        assertEquals("ива", iva.prefix());
        assertTrue(iva.visited());
        assertFalse(iva.children()[0].visited());
    }

    @Test
    void snapshotRespectsNodeLimit() {
        TrieNodeView root = trie.snapshot("", 10, 3, null);
        assertEquals(2, root.children().length);
        assertTrue(root.children()[0].truncated());
    }

    @Test
    void snapshotOfUnknownPrefixIsNull() {
        assertNull(trie.snapshot("я", 10, 100, null));
    }
}
