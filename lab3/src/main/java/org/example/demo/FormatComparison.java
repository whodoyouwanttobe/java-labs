package org.example.demo;

import org.example.generator.ContactGenerator;
import org.example.io.BinarySerializer;
import org.example.io.JsonSerializer;
import org.example.io.Serializer;
import org.example.model.Contact;
import org.example.store.ContactStore;

import java.nio.file.Files;
import java.nio.file.Path;

public class FormatComparison {

    private static final int COUNT = 10_000;
    private static final int RUNS = 5;

    public static void main(String[] args) throws Exception {
        ContactStore store = new ContactStore();
        new ContactGenerator(42).fill(store, COUNT);
        Contact[] contacts = store.toArray();
        Path dir = Files.createTempDirectory("formats");

        System.out.println("Контактов: " + COUNT);
        System.out.println("Формат | Размер, байт | Запись, мс | Чтение, мс");
        for (Serializer serializer : new Serializer[]{new JsonSerializer(), new BinarySerializer()}) {
            Path file = dir.resolve("contacts." + serializer.getFormat());
            serializer.save(contacts, file);
            serializer.load(file);

            long writeTime = 0;
            long readTime = 0;
            for (int i = 0; i < RUNS; i++) {
                long start = System.nanoTime();
                serializer.save(contacts, file);
                writeTime += System.nanoTime() - start;

                start = System.nanoTime();
                serializer.load(file);
                readTime += System.nanoTime() - start;
            }
            System.out.printf("%s | %d | %.1f | %.1f%n", serializer.getFormat(), Files.size(file),
                    writeTime / 1e6 / RUNS, readTime / 1e6 / RUNS);
        }
        System.out.println("Файлы лежат в " + dir);
    }
}
