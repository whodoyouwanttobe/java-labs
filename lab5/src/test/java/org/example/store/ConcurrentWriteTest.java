package org.example.store;

import org.example.model.Contact;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcurrentWriteTest {

    @Test
    void parallelInsertsAreNotLost() throws InterruptedException {
        ContactStore store = new ContactStore();
        int threadCount = 4;
        int perThread = 5000;
        Thread[] threads = new Thread[threadCount];
        for (int t = 0; t < threadCount; t++) {
            int id = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < perThread; i++) {
                    store.add(new Contact("k" + (i * threadCount + id), "", "", ""));
                }
            });
            threads[t].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertEquals(threadCount * perThread, store.size());
    }
}

