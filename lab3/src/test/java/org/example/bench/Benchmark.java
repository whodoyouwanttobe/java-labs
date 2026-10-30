package org.example.bench;

import org.example.generator.ContactGenerator;
import org.example.model.Contact;
import org.example.store.ContactStore;

import java.util.TreeMap;

public class Benchmark {

    private static final int[] SIZES = {10_000, 100_000};
    private static final int RUNS = 5;
    private static final int PREFIX_QUERIES = 1000;
    private static final int PREFIX_LIMIT = 10;

    private static long checksum;

    public static void main(String[] args) {
        System.out.println("| N | Операция | Trie, мс | TreeMap, мс |");
        System.out.println("|---|---|---|---|");
        for (int n : SIZES) {
            Contact[] contacts = generate(n);
            String[] prefixes = prefixes(contacts);
            measure(contacts, prefixes);

            double[] trie = new double[5];
            double[] treeMap = new double[5];
            for (int run = 0; run < RUNS; run++) {
                add(trie, measure(contacts, prefixes));
                add(treeMap, measureTreeMap(contacts, prefixes));
            }
            String[] operations = {"вставка", "поиск", "автодополнение x1000", "подсчёт в поддереве x1000", "удаление"};
            for (int i = 0; i < operations.length; i++) {
                System.out.printf("| %d | %s | %.2f | %.2f |%n", n, operations[i], trie[i] / RUNS, treeMap[i] / RUNS);
            }
        }
        System.out.println("Контрольная сумма: " + checksum);
    }

    private static double[] measure(Contact[] contacts, String[] prefixes) {
        ContactStore store = new ContactStore();
        double[] times = new double[5];

        long start = System.nanoTime();
        for (Contact c : contacts) {
            store.add(c);
        }
        times[0] = millis(start);

        start = System.nanoTime();
        for (Contact c : contacts) {
            checksum += store.find(c.getName()).getPhone().length();
        }
        times[1] = millis(start);

        start = System.nanoTime();
        for (String prefix : prefixes) {
            checksum += store.findByPrefix(prefix, PREFIX_LIMIT).length;
        }
        times[2] = millis(start);

        start = System.nanoTime();
        for (String prefix : prefixes) {
            checksum += store.countByPrefix(prefix);
        }
        times[3] = millis(start);

        start = System.nanoTime();
        for (Contact c : contacts) {
            store.remove(c.getName());
        }
        times[4] = millis(start);
        return times;
    }

    private static double[] measureTreeMap(Contact[] contacts, String[] prefixes) {
        TreeMap<String, Contact> map = new TreeMap<>();
        double[] times = new double[5];

        long start = System.nanoTime();
        for (Contact c : contacts) {
            map.put(c.getName().toLowerCase(), c);
        }
        times[0] = millis(start);

        start = System.nanoTime();
        for (Contact c : contacts) {
            checksum += map.get(c.getName().toLowerCase()).getPhone().length();
        }
        times[1] = millis(start);

        start = System.nanoTime();
        for (String prefix : prefixes) {
            int taken = 0;
            for (Contact c : map.subMap(prefix, prefix + Character.MAX_VALUE).values()) {
                checksum += c.getName().length();
                if (++taken == PREFIX_LIMIT) {
                    break;
                }
            }
        }
        times[2] = millis(start);

        start = System.nanoTime();
        for (String prefix : prefixes) {
            checksum += map.subMap(prefix, prefix + Character.MAX_VALUE).size();
        }
        times[3] = millis(start);

        start = System.nanoTime();
        for (Contact c : contacts) {
            map.remove(c.getName().toLowerCase());
        }
        times[4] = millis(start);
        return times;
    }

    private static Contact[] generate(int n) {
        ContactStore store = new ContactStore();
        new ContactGenerator(7).fill(store, n);
        return store.toArray();
    }

    private static String[] prefixes(Contact[] contacts) {
        String[] prefixes = new String[PREFIX_QUERIES];
        for (int i = 0; i < PREFIX_QUERIES; i++) {
            String name = contacts[i * contacts.length / PREFIX_QUERIES].getName().toLowerCase();
            prefixes[i] = name.substring(0, 1 + i % 8);
        }
        return prefixes;
    }

    private static void add(double[] sum, double[] times) {
        for (int i = 0; i < sum.length; i++) {
            sum[i] += times[i];
        }
    }

    private static double millis(long start) {
        return (System.nanoTime() - start) / 1_000_000.0;
    }
}
