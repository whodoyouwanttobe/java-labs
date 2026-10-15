package org.example.bench;

import org.example.generator.ContactGenerator;
import org.example.model.Contact;
import org.example.store.ContactStores;
import org.example.store.Trie;
import org.example.store.TrieStore;

import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

public class Benchmark {

    private static final int[] SIZES = {10_000, 100_000};
    private static final int RUNS = 5;
    private static final int PREFIX_QUERIES = 1_000;
    private static final int PREFIX_LIMIT = 10;

    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            measure(10_000);
        }

        System.out.println("| N | Операция | Trie, мс | TreeMap, мс |");
        System.out.println("|---|---|---|---|");
        for (int n : SIZES) {
            double[][] sum = new double[2][5];
            for (int r = 0; r < RUNS; r++) {
                double[][] res = measure(n);
                for (int s = 0; s < 2; s++) {
                    for (int op = 0; op < 5; op++) {
                        sum[s][op] += res[s][op];
                    }
                }
            }
            String[] ops = {
                    "вставка " + n,
                    "поиск " + n,
                    "автодополнение x" + PREFIX_QUERIES,
                    "подсчёт в поддереве x" + PREFIX_QUERIES,
                    "удаление " + n
            };
            for (int op = 0; op < ops.length; op++) {
                System.out.printf(Locale.ROOT, "| %d | %s | %.2f | %.2f |%n",
                        n, ops[op], sum[0][op] / RUNS, sum[1][op] / RUNS);
            }
        }
    }

    private static double[][] measure(int n) {
        TrieStore<Contact> tmp = ContactStores.create();
        new ContactGenerator(7).generateInto(tmp, n);
        Contact[] data = tmp.toArray();
        shuffle(data, new Random(3));

        String[] prefixes = new String[PREFIX_QUERIES];
        Random rnd = new Random(5);
        for (int i = 0; i < prefixes.length; i++) {
            String name = Trie.normalize(data[rnd.nextInt(n)].getName());
            prefixes[i] = name.substring(0, Math.min(name.length(), 1 + rnd.nextInt(8)));
        }

        return new double[][]{measureTrie(data, prefixes), measureTreeMap(data, prefixes)};
    }

    private static double[] measureTrie(Contact[] data, String[] prefixes) {
        TrieStore<Contact> store = ContactStores.create();
        double[] t = new double[5];
        long sink = 0;

        long start = System.nanoTime();
        for (Contact c : data) {
            store.add(c);
        }
        t[0] = ms(start);

        start = System.nanoTime();
        for (Contact c : data) {
            sink += store.findByKey(c.getName()).getPhone().length();
        }
        t[1] = ms(start);

        start = System.nanoTime();
        for (String p : prefixes) {
            sink += store.findByPrefix(p, PREFIX_LIMIT).length;
        }
        t[2] = ms(start);

        start = System.nanoTime();
        for (String p : prefixes) {
            sink += store.countByPrefix(p);
        }
        t[3] = ms(start);

        start = System.nanoTime();
        for (Contact c : data) {
            store.removeByKey(c.getName());
        }
        t[4] = ms(start);

        blackhole(sink);
        return t;
    }

    private static double[] measureTreeMap(Contact[] data, String[] prefixes) {
        TreeMap<String, Contact> map = new TreeMap<>();
        double[] t = new double[5];
        long sink = 0;

        long start = System.nanoTime();
        for (Contact c : data) {
            map.putIfAbsent(Trie.normalize(c.getName()), c);
        }
        t[0] = ms(start);

        start = System.nanoTime();
        for (Contact c : data) {
            sink += map.get(Trie.normalize(c.getName())).getPhone().length();
        }
        t[1] = ms(start);

        start = System.nanoTime();
        for (String p : prefixes) {
            int taken = 0;
            for (Map.Entry<String, Contact> e : prefixRange(map, p).entrySet()) {
                if (taken++ == PREFIX_LIMIT) {
                    break;
                }
                sink += e.getValue().getName().length();
            }
        }
        t[2] = ms(start);

        start = System.nanoTime();
        for (String p : prefixes) {
            sink += prefixRange(map, p).size();
        }
        t[3] = ms(start);

        start = System.nanoTime();
        for (Contact c : data) {
            map.remove(Trie.normalize(c.getName()));
        }
        t[4] = ms(start);

        blackhole(sink);
        return t;
    }

    private static SortedMap<String, Contact> prefixRange(TreeMap<String, Contact> map, String prefix) {
        return map.subMap(prefix, prefix + Character.MAX_VALUE);
    }

    private static void shuffle(Contact[] a, Random rnd) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            Contact tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    private static double ms(long start) {
        return (System.nanoTime() - start) / 1_000_000.0;
    }

    private static void blackhole(long value) {
        if (value == 42) {
            System.out.print("");
        }
    }
}
