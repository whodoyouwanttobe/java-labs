package org.example.demo;

import org.example.store.Trie;
import org.example.store.TrieStore;

public class RaceDemo {

    private static final int THREADS = 8;
    private static final int PER_THREAD = 20_000;

    public static void main(String[] args) throws InterruptedException {
        Trie<Integer> trie = new Trie<>();
        int failed = runThreads(number -> trie.put("k" + number, number));
        System.out.println("Без синхронизации: ожидали " + THREADS * PER_THREAD
                + ", в дереве " + trie.size() + ", упавших вставок " + failed);

        TrieStore<Integer> store = new TrieStore<>(number -> "k" + number, Integer[]::new);
        failed = runThreads(store::add);
        System.out.println("С synchronized: ожидали " + THREADS * PER_THREAD
                + ", в дереве " + store.size() + ", упавших вставок " + failed);
    }

    private static int runThreads(Inserter inserter) throws InterruptedException {
        int[] failed = new int[THREADS];
        Thread[] threads = new Thread[THREADS];
        for (int t = 0; t < THREADS; t++) {
            int id = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    try {
                        inserter.insert(i * THREADS + id);
                    } catch (RuntimeException e) {
                        failed[id]++;
                    }
                }
            });
            threads[t].start();
        }
        int total = 0;
        for (int t = 0; t < THREADS; t++) {
            threads[t].join();
            total += failed[t];
        }
        return total;
    }

    private interface Inserter {
        void insert(int number);
    }
}
