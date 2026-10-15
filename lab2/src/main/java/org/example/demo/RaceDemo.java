package org.example.demo;

import org.example.store.Trie;
import org.example.store.TrieStore;

public class RaceDemo {

    private static final int THREADS = 8;
    private static final int PER_THREAD = 20_000;

    private static int brokenLookups;

    public static void main(String[] args) throws InterruptedException {
        int expected = THREADS * PER_THREAD;

        Trie<Integer> unsafe = new Trie<>();
        int[] failures = new int[THREADS];
        runWriters(i -> unsafe.put(key(i), i), failures);
        System.out.println("Без синхронизации (голый Trie):");
        System.out.println("  ожидали " + expected + ", size() = " + unsafe.size()
                + ", упавших вставок: " + sum(failures));
        System.out.println("  реально находится по ключу: " + countFound(unsafe, expected)
                + ", поиск упал с исключением: " + brokenLookups);

        TrieStore<Integer> safe = new TrieStore<>(v -> key(v), Integer[]::new);
        int[] safeFailures = new int[THREADS];
        runWriters(safe::add, safeFailures);
        System.out.println("С ReadWriteLock (TrieStore):");
        System.out.println("  ожидали " + expected + ", size() = " + safe.size()
                + ", упавших вставок: " + sum(safeFailures));
    }

    private interface Writer {
        void write(int value);
    }

    private static void runWriters(Writer writer, int[] failures) throws InterruptedException {
        Thread[] threads = new Thread[THREADS];
        for (int t = 0; t < THREADS; t++) {
            int id = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    try {
                        writer.write(i * THREADS + id);
                    } catch (RuntimeException e) {
                        failures[id]++;
                    }
                }
            });
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private static int countFound(Trie<Integer> trie, int expected) {
        int found = 0;
        for (int i = 0; i < expected; i++) {
            try {
                if (trie.get(key(i)) != null) {
                    found++;
                }
            } catch (RuntimeException e) {
                brokenLookups++;
            }
        }
        return found;
    }

    private static String key(int value) {
        return "k" + value;
    }

    private static int sum(int[] values) {
        int s = 0;
        for (int v : values) {
            s += v;
        }
        return s;
    }
}
