package org.example.demo;

import org.example.csv.CsvLoader;
import org.example.csv.CsvSaver;
import org.example.generator.ContactGenerator;
import org.example.io.ContactFormat;
import org.example.io.FileStorage;
import org.example.io.SerializationException;
import org.example.model.Contact;
import org.example.store.ContactStores;
import org.example.store.TrieStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FormatComparison {

    private static final int RUNS = 5;
    private static final int WARMUP = 2;

    private interface Action {
        void run() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 10_000;
        Path dir = Path.of(args.length > 1 ? args[1] : "format-samples");
        Files.createDirectories(dir);

        TrieStore<Contact> store = ContactStores.create();
        new ContactGenerator(42).generateInto(store, count);
        System.out.println("Контактов: " + store.size());
        System.out.println();
        System.out.printf("%-10s %12s %14s %14s%n", "Формат", "Размер, Б", "Запись, мс", "Чтение, мс");

        Path csv = dir.resolve("contacts.csv");
        measure("CSV", csv,
                () -> new CsvSaver().save(csv, store),
                () -> new CsvLoader().load(csv, ContactStores.create()));

        for (ContactFormat format : ContactFormat.values()) {
            Path file = dir.resolve("contacts." + format.extension());
            FileStorage<Contact> storage = new FileStorage<>(format.serializer());
            measure(format.title(), file,
                    () -> storage.save(file, store),
                    () -> storage.loadInto(file, ContactStores.create()));
            checkRoundTrip(store, storage, file);
        }

        System.out.println();
        System.out.println("Начало JSON-файла:");
        System.out.println(head(dir.resolve("contacts.json"), 12));
        System.out.println("Первые 64 байта бинарного файла:");
        System.out.println(hexDump(Files.readAllBytes(dir.resolve("contacts.ctb")), 64));
    }

    private static void measure(String name, Path file, Action write, Action read) throws Exception {
        double writeMs = average(write);
        double readMs = average(read);
        System.out.printf("%-10s %12d %14.1f %14.1f%n", name, Files.size(file), writeMs, readMs);
    }

    private static double average(Action action) throws Exception {
        for (int i = 0; i < WARMUP; i++) {
            action.run();
        }
        long total = 0;
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            action.run();
            total += System.nanoTime() - start;
        }
        return total / 1e6 / RUNS;
    }

    private static void checkRoundTrip(TrieStore<Contact> original, FileStorage<Contact> storage, Path file)
            throws IOException, SerializationException {
        Contact[] expected = original.toArray();
        Contact[] actual = storage.read(file);
        if (expected.length != actual.length) {
            throw new IllegalStateException("Размер не совпал после чтения " + file);
        }
        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equals(actual[i])) {
                throw new IllegalStateException("Запись " + i + " отличается после чтения " + file);
            }
        }
    }

    private static String head(Path file, int lines) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            for (int i = 0; i < lines && (line = reader.readLine()) != null; i++) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private static String hexDump(byte[] bytes, int limit) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(limit, bytes.length); i += 16) {
            sb.append(String.format("%04X  ", i));
            StringBuilder text = new StringBuilder();
            for (int j = i; j < i + 16 && j < bytes.length; j++) {
                sb.append(String.format("%02X ", bytes[j]));
                char c = (char) (bytes[j] & 0xFF);
                text.append(c >= 32 && c < 127 ? c : '.');
            }
            sb.append(" ").append(text).append('\n');
        }
        return sb.toString();
    }
}
