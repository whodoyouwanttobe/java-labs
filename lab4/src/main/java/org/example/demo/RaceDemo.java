package org.example.demo;

import org.example.model.Contact;
import org.example.store.ContactStore;
import org.example.store.Trie;

public class RaceDemo {

    private static final int THREADS = 8;
    private static final int PER_THREAD = 20_000;

    public static void main(String[] args) throws InterruptedException {
        Trie<Contact> trie = new Trie<>();
        int[] failed = new int[THREADS];
        Thread[] threads = new Thread[THREADS];
        for (int t = 0; t < THREADS; t++) {
            int id = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    try {
                        Contact contact = contact(i * THREADS + id);
                        trie.put(contact.getName(), contact);
                    } catch (RuntimeException e) {
                        failed[id]++;
                    }
                }
            });
            threads[t].start();
        }
        join(threads);
        int failedTotal = 0;
        for (int count : failed) {
            failedTotal += count;
        }
        System.out.println("Без синхронизации: ожидали " + THREADS * PER_THREAD + ", в дереве " + trie.size()
                + ", упавших вставок " + failedTotal);

        ContactStore store = new ContactStore();
        for (int t = 0; t < THREADS; t++) {
            int id = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < PER_THREAD; i++) {
                    store.add(contact(i * THREADS + id));
                }
            });
            threads[t].start();
        }
        join(threads);
        System.out.println("С synchronized: ожидали " + THREADS * PER_THREAD + ", в дереве " + store.size());
    }

    private static Contact contact(int number) {
        return new Contact("k" + number, "", "", "");
    }

    private static void join(Thread[] threads) throws InterruptedException {
        for (Thread thread : threads) {
            thread.join();
        }
    }
}

