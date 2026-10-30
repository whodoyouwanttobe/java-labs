package org.example.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.io.dto.ContactBookDto;
import org.example.io.dto.ContactDto;
import org.example.model.Contact;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonSerializer implements Serializer {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Override
    public String getFormat() {
        return "json";
    }

    @Override
    public void save(Contact[] contacts, Path file) throws IOException {
        ContactDto[] dtos = new ContactDto[contacts.length];
        for (int i = 0; i < contacts.length; i++) {
            dtos[i] = ContactMapper.toDto(contacts[i]);
        }
        mapper.writeValue(file.toFile(), new ContactBookDto(SCHEMA_VERSION, dtos));
    }

    @Override
    public Contact[] load(Path file) throws IOException, SerializationException {
        if (Files.size(file) == 0) {
            throw new SerializationException(SerializationError.EMPTY_FILE, "Файл пустой");
        }
        ContactBookDto book;
        try {
            book = mapper.readValue(file.toFile(), ContactBookDto.class);
        } catch (JsonProcessingException e) {
            String line = e.getLocation() == null ? "" : " (строка " + e.getLocation().getLineNr() + ")";
            throw new SerializationException(SerializationError.BAD_FORMAT,
                    "Ошибка в JSON" + line + ": " + e.getOriginalMessage());
        }

        if (book.schemaVersion() == null) {
            throw new SerializationException(SerializationError.MISSING_FIELD, "Нет поля schema_version");
        }
        if (book.schemaVersion() != SCHEMA_VERSION) {
            throw new SerializationException(SerializationError.BAD_VERSION,
                    "Версия " + book.schemaVersion() + " не поддерживается, нужна " + SCHEMA_VERSION);
        }
        if (book.contacts() == null) {
            throw new SerializationException(SerializationError.MISSING_FIELD, "Нет поля contacts");
        }

        Contact[] contacts = new Contact[book.contacts().length];
        for (int i = 0; i < contacts.length; i++) {
            contacts[i] = ContactMapper.fromDto(book.contacts()[i], i);
        }
        return contacts;
    }
}
