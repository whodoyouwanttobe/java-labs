package org.example.demo;

import org.example.csv.CsvLoader;
import org.example.csv.CsvParseException;
import org.example.csv.LoadResult;
import org.example.generator.ContactGenerator;
import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.store.ContactStores;
import org.example.store.DuplicateKeyException;
import org.example.store.TrieStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConsoleDemo {

    private static final int DEPTH = 64;

    public static void main(String[] args) throws IOException {
        TrieStore<Contact> store = ContactStores.create();

        Path csv = Path.of(args.length > 0 ? args[0] : "test.csv");
        if (Files.exists(csv)) {
            LoadResult result = new CsvLoader().load(csv, store);
            System.out.println("Загружено из " + csv + ": " + result.getLoaded());
            for (CsvParseException e : result.getErrors()) {
                System.out.println("  строка " + e.getLineNumber() + ": " + e.getMessage() + " [" + e.getErrorCode() + "]");
            }
        }

        store.add(new Contact("Иван Сидоров", "+79130000001", "sidorov@mail.ru", "НГТУ"));
        store.add(new Contact("Ирина Котова", "+79130000002", "kotova@mail.ru", "НГТУ"));
        store.add(new CorporateContact("Игорь Лесной", "+79130000003", "les@corp.local", "ООО Альфа", "Инженер", "204"));
        store.add(new Contact("Анна Белова", "+79130000004", "belova@mail.ru", "ИП Белова"));

        section("Всего контактов: " + store.size() + ", узлов в дереве: " + store.nodeCount());
        print(store, "", null);

        section("Вставка 'Иван Петров' (подсвечен путь вставки)");
        store.add(new Contact("Иван Петров", "+79130000005", "petrov@mail.ru", "ООО Вектор"));
        print(store, "", "Иван Петров");

        section("Повторная вставка через tryAdd -> " + store.tryAdd(new Contact("иван петров", "", "", "")));
        try {
            store.add(new Contact("иван петров", "", "", ""));
        } catch (DuplicateKeyException e) {
            System.out.println("add бросил исключение: " + e.getMessage());
        }

        section("Поиск 'Ирина Котова' -> " + store.findByKey("Ирина Котова"));
        print(store, "", "Ирина Котова");

        section("Поиск 'Ирина Иванова' -> " + store.findByKey("Ирина Иванова"));

        section("Автодополнение 'и' (limit 10)");
        for (Contact c : store.findByPrefix("и", 10)) {
            System.out.println("  " + c.getName() + " [" + c.getType() + "]");
        }
        System.out.println("Контактов в поддереве 'и': " + store.countByPrefix("и"));
        System.out.println("Контактов в поддереве 'ива': " + store.countByPrefix("ива"));
        System.out.println("Контактов в поддереве 'я': " + store.countByPrefix("я"));
        print(store, "и", "и");

        section("Удаление 'Иван Сидоров' -> " + store.removeByKey("Иван Сидоров"));
        print(store, "", "Иван Сидоров");

        section("Генерация 10000 контактов");
        long start = System.nanoTime();
        int added = new ContactGenerator(42).generateInto(store, 10_000);
        System.out.printf("Добавлено %d за %.1f мс, всего %d, узлов %d%n",
                added, (System.nanoTime() - start) / 1e6, store.size(), store.nodeCount());
        System.out.println("Автодополнение 'иванов ив' (первые 5):");
        for (Contact c : store.findByPrefix("иванов ив", 5)) {
            System.out.println("  " + c.getName());
        }
        System.out.println("Всего на 'иванов': " + store.countByPrefix("иванов"));
    }

    private static void print(TrieStore<Contact> store, String prefix, String highlight) {
        System.out.print(TriePrinter.print(store.snapshot(prefix, DEPTH, Integer.MAX_VALUE, highlight)));
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("== " + title);
    }
}
