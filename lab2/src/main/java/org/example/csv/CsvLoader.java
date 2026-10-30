package org.example.csv;

import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.model.EmergencyContact;
import org.example.store.DuplicateKeyException;
import org.example.store.Store;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CsvLoader {

    private static final int BASE_FIELDS = 5;
    private static final int CORPORATE_FIELDS = 7;
    private static final int PROGRESS_STEP = 500;

    public LoadResult load(Path file, Store<Contact> store) throws IOException {
        return load(file, store, (done, total) -> { });
    }

    public LoadResult load(Path file, Store<Contact> store, ProgressListener listener) throws IOException {
        long fileSize = Files.size(file);
        long bytesRead = 0;
        int loaded = 0;
        List<CsvParseException> errors = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            reader.readLine();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                lineNumber++;
                bytesRead += line.getBytes(StandardCharsets.UTF_8).length + 1;
                if (lineNumber % PROGRESS_STEP == 0) {
                    listener.onProgress(bytesRead, fileSize);
                }
                if (line.isBlank()) {
                    continue;
                }
                try {
                    store.add(parseLine(line, lineNumber));
                    loaded++;
                } catch (CsvParseException e) {
                    errors.add(e);
                } catch (DuplicateKeyException e) {
                    errors.add(new CsvParseException(e.getMessage(), lineNumber, CsvErrorCode.DUPLICATE_NAME));
                }
            }
        }
        listener.onProgress(fileSize, fileSize);
        return new LoadResult(loaded, errors);
    }

    Contact parseLine(String line, int lineNumber) throws CsvParseException {
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
                if (!CorporateContact.isValidInternalNumber(internalNumber)) {
                    throw new CsvParseException("Внутренний номер содержит не цифры", lineNumber,
                            CsvErrorCode.BAD_NUMBER);
                }
                return new CorporateContact(name, phone, email, organization, position, internalNumber);
            default:
                throw new CsvParseException("Неизвестный тип " + type, lineNumber, CsvErrorCode.UNKNOWN_TYPE);
        }
    }
}
