package org.example.csv;

import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.store.EntityStore;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CsvSaver {

    public static final String HEADER = "Тип;Имя;Телефон;Email;Организация;Должность;Внутренний номер";

    public int save(Path file, EntityStore<String, Contact> store) throws IOException {
        Contact[] contacts = store.toArray();
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();
            for (Contact c : contacts) {
                writer.write(toLine(c));
                writer.newLine();
            }
        }
        return contacts.length;
    }

    String toLine(Contact c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.getType());
        append(sb, c.getName());
        append(sb, c.getPhone());
        append(sb, c.getEmail());
        append(sb, c.getOrganization());
        if (c instanceof CorporateContact cc) {
            append(sb, cc.getPosition());
            append(sb, cc.getInternalNumber());
        }
        return sb.toString();
    }

    private static void append(StringBuilder sb, String value) {
        sb.append(CsvFormat.SEPARATOR).append(CsvFormat.escape(value));
    }
}
