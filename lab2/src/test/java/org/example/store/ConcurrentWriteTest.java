package org.example.store;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcurrentWriteTest {

    @Test
    void storeKeepsAllParallelInserts() throws InterruptedException {
        TrieStore<Integer> store = new TrieStore<>(v -> "k" + v, Integer[]::new);
        int threads = 4;
        int perThread = 5_000;
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            int id = t;
            workers[t] = new Thread(() -> {
                for (int i = 0; i < perThread; i++) {
                    store.add(i * threads + id);
                }
            });
            workers[t].start();
        }
        for (Thread w : workers) {
            w.join();
        }
        assertEquals(threads * perThread, store.size());
    }

    @Test
    void readersRunWhileWriterInserts() throws InterruptedException {
        TrieStore<Integer> store = new TrieStore<>(v -> "k" + v, Integer[]::new);
        int total = 20_000;
        AtomicInteger readerErrors = new AtomicInteger();
        Thread writer = new Thread(() -> {
            for (int i = 0; i < total; i++) {
                store.add(i);
            }
        });
        Thread[] readers = new Thread[3];
        for (int r = 0; r < readers.length; r++) {
            readers[r] = new Thread(() -> {
                for (int i = 0; i < total; i++) {
                    try {
                        store.countByPrefix("k1");
                        store.findByPrefix("k2", 5);
                    } catch (RuntimeException e) {
                        readerErrors.incrementAndGet();
                    }
                }
            });
        }
        writer.start();
        for (Thread r : readers) {
            r.start();
        }
        writer.join();
        for (Thread r : readers) {
            r.join();
        }
        assertEquals(0, readerErrors.get());
        assertEquals(total, store.size());
    }
}
