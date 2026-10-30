package org.example.csv;

import org.example.model.Contact;
import org.example.model.CorporateContact;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CsvSaver {

    public static final String HEADER = "Тип;Имя;Телефон;Email;Организация;Должность;Внутренний номер";

    public void save(Path file, Contact[] contacts) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();
            for (Contact contact : contacts) {
                writer.write(toLine(contact));
                writer.newLine();
            }
        }
    }

    private String toLine(Contact c) {
        StringBuilder line = new StringBuilder(c.getType());
        appendField(line, c.getName());
        appendField(line, c.getPhone());
        appendField(line, c.getEmail());
        appendField(line, c.getOrganization());
        if (c instanceof CorporateContact corporate) {
            appendField(line, corporate.getPosition());
            appendField(line, corporate.getInternalNumber());
        }
        return line.toString();
    }

    private void appendField(StringBuilder line, String value) {
        line.append(CsvFormat.SEPARATOR).append(CsvFormat.escape(value));
    }
}
