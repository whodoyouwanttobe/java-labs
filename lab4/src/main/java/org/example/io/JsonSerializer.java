package org.example.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.io.dto.ContactDto;
import org.example.io.dto.ContactFileDto;
import org.example.model.Contact;

import java.io.IOException;
import java.nio.file.Path;

public class JsonSerializer implements Serializer {

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public String getFormat() {
        return "json";
    }

    @Override
    public void save(Contact[] contacts, Path file, int version) throws IOException {
        if (version < MIN_SCHEMA_VERSION || version > SCHEMA_VERSION) {
            throw new IllegalArgumentException("Неверная версия: " + version);
        }
        ContactFileDto data = new ContactFileDto();
        data.schemaVersion = version;
        data.contacts = new ContactDto[contacts.length];
        for (int i = 0; i < contacts.length; i++) {
            data.contacts[i] = ContactMigrator.toDto(contacts[i], version);
        }
        mapper.writeValue(file.toFile(), data);
    }

    @Override
    public int readVersion(Path file) throws IOException, SerializationException {
        ContactFileDto data = readDto(file);
        ContactMigrator.checkVersion(data.schemaVersion);
        return data.schemaVersion;
    }

    @Override
    public Contact[] load(Path file) throws IOException, SerializationException {
        ContactFileDto data = readDto(file);
        ContactMigrator.migrate(data);
        Contact[] contacts = new Contact[data.contacts.length];
        for (int i = 0; i < contacts.length; i++) {
            contacts[i] = ContactMigrator.fromDto(data.contacts[i], i + 1);
        }
        return contacts;
    }

    private ContactFileDto readDto(Path file) throws IOException, SerializationException {
        try {
            return mapper.readValue(file.toFile(), ContactFileDto.class);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Файл повреждён или это не файл справочника");
        }
    }
}

