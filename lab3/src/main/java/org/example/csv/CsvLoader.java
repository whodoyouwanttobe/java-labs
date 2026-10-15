package org.example.csv;

import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.model.EmergencyContact;
import org.example.store.EntityStore;
import org.example.tasks.ProgressListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

public class CsvLoader {

    private static final int BASE_FIELDS = 5;
    private static final int CORPORATE_FIELDS = 7;
    private static final int PROGRESS_STEP = 500;

    public LoadResult load(Path file, EntityStore<String, Contact> target) throws IOException {
        return load(file, target, ProgressListener.NONE, () -> false);
    }

    public LoadResult load(Path file, EntityStore<String, Contact> target,
                           ProgressListener progress, BooleanSupplier cancelled) throws IOException {
        long totalBytes = Files.size(file);
        List<CsvParseException> errors = new ArrayList<>();
        int loaded = 0;

        try (CountingInputStream counter = new CountingInputStream(Files.newInputStream(file));
             BufferedReader reader = new BufferedReader(new InputStreamReader(counter, StandardCharsets.UTF_8))) {
            reader.readLine();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (cancelled.getAsBoolean()) {
                    return new LoadResult(loaded, errors, true);
                }
                if (lineNumber % PROGRESS_STEP == 0) {
                    progress.onProgress(counter.getCount(), totalBytes);
                }
                if (line.isBlank()) {
                    continue;
                }
                try {
                    Contact contact = parseLine(line, lineNumber);
                    if (!target.tryAdd(contact)) {
                        throw new CsvParseException("Контакт с именем '" + contact.getName() + "' уже есть",
                                lineNumber, CsvErrorCode.DUPLICATE_NAME);
                    }
                    loaded++;
                } catch (CsvParseException e) {
                    errors.add(e);
                }
            }
        }
        progress.onProgress(totalBytes, totalBytes);
        return new LoadResult(loaded, errors, false);
    }

    public Contact parseLine(String line, int lineNumber) throws CsvParseException {
        String[] parts = CsvFormat.split(line, lineNumber);
        if (parts.length < BASE_FIELDS) {
            throw new CsvParseException("Недостаточно полей", lineNumber, CsvErrorCode.WRONG_FIELD_COUNT);
        }
        String type = parts[0].trim();
        String name = parts[1].trim();
        String phone = parts[2].trim();
        String email = parts[3].trim();
        String organization = parts[4].trim();

        if (name.isEmpty()) {
            throw new CsvParseException("Пустое имя контакта", lineNumber, CsvErrorCode.EMPTY_REQUIRED_FIELD);
        }

        switch (type.toLowerCase(Locale.ROOT)) {
            case "contact":
                return new Contact(name, phone, email, organization);
            case "emergency":
                return new EmergencyContact(name, phone, email, organization);
            case "corporate":
                if (parts.length < CORPORATE_FIELDS) {
                    throw new CsvParseException("Недостаточно полей для Corporate", lineNumber,
                            CsvErrorCode.WRONG_FIELD_COUNT);
                }
                String position = parts[5].trim();
                String internalNumber = parts[6].trim();
                if (!internalNumber.isEmpty() && !internalNumber.matches("\\d+")) {
                    throw new CsvParseException("Внутренний номер содержит не цифры", lineNumber,
                            CsvErrorCode.BAD_NUMBER);
                }
                return new CorporateContact(name, phone, email, organization, position, internalNumber);
            default:
                throw new CsvParseException("Неизвестный тип '" + type + "'", lineNumber, CsvErrorCode.UNKNOWN_TYPE);
        }
    }
}
