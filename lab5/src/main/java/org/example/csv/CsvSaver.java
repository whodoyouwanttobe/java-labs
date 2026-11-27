package org.example.csv;

import org.example.model.Contact;
import org.example.model.CorporateContact;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CsvSaver {

    public static final String SEPARATOR = ";";
    public static final String HEADER = "Тип;Имя;Телефон;Email;Организация;Должность;Внутренний номер";

    public void save(Path file, Contact[] contacts) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();
            for (Contact c : contacts) {
                String line = c.getType() + field(c.getName()) + field(c.getPhone())
                        + field(c.getEmail()) + field(c.getOrganization());
                if (c instanceof CorporateContact corporate) {
                    line += field(corporate.getPosition()) + field(corporate.getInternalNumber());
                }
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private String field(String value) throws IOException {
        if (value == null) {
            return SEPARATOR;
        }
        if (value.contains(SEPARATOR) || value.contains("\n")) {
            throw new IOException("Поле \"" + value + "\" содержит ; или перенос строки, в CSV его не записать");
        }
        return SEPARATOR + value;
    }
}

