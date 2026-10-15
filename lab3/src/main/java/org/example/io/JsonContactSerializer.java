package org.example.io;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.io.dto.ContactBookDto;
import org.example.io.dto.ContactDto;
import org.example.model.Contact;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class JsonContactSerializer implements Serializer<Contact> {

    private static final String VERSION_FIELD = "schema_version";
    private static final String CONTACTS_FIELD = "contacts";

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

    @Override
    public void write(Contact[] items, OutputStream out) throws IOException {
        ContactDto[] dtos = new ContactDto[items.length];
        for (int i = 0; i < items.length; i++) {
            dtos[i] = ContactMapper.toDto(items[i]);
        }
        mapper.writeValue(out, new ContactBookDto(SCHEMA_VERSION, dtos));
    }

    @Override
    public Contact[] read(InputStream in) throws IOException, SerializationException {
        byte[] bytes = in.readAllBytes();
        if (new String(bytes, StandardCharsets.UTF_8).isBlank()) {
            throw new SerializationException(SerializationError.EMPTY_FILE, "Файл пустой", null);
        }
        JsonNode root = parse(bytes);
        if (!root.isObject()) {
            throw new SerializationException(SerializationError.MALFORMED, "Ожидался JSON-объект", "$");
        }
        checkVersion(root.get(VERSION_FIELD));
        JsonNode list = root.get(CONTACTS_FIELD);
        if (list == null || list.isNull()) {
            throw new SerializationException(SerializationError.MISSING_FIELD,
                    "Нет поля '" + CONTACTS_FIELD + "'", "$");
        }
        if (!list.isArray()) {
            throw new SerializationException(SerializationError.MALFORMED,
                    "Поле '" + CONTACTS_FIELD + "' должно быть массивом", "$." + CONTACTS_FIELD);
        }
        Contact[] result = new Contact[list.size()];
        for (int i = 0; i < result.length; i++) {
            String location = "$." + CONTACTS_FIELD + "[" + i + "]";
            result[i] = ContactMapper.fromDto(toDto(list.get(i), location), location);
        }
        return result;
    }

    private JsonNode parse(byte[] bytes) throws SerializationException {
        try {
            return mapper.readTree(bytes);
        } catch (JsonProcessingException e) {
            JsonLocation where = e.getLocation();
            String location = where == null ? null : "строка " + where.getLineNr() + ", столбец " + where.getColumnNr();
            throw new SerializationException(SerializationError.MALFORMED,
                    "Некорректный JSON: " + e.getOriginalMessage(), location, e);
        } catch (IOException e) {
            throw new SerializationException(SerializationError.MALFORMED, "Некорректный JSON", null, e);
        }
    }

    private void checkVersion(JsonNode version) throws SerializationException {
        if (version == null || version.isNull()) {
            throw new SerializationException(SerializationError.MISSING_FIELD,
                    "Нет поля '" + VERSION_FIELD + "'", "$");
        }
        if (!version.isInt()) {
            throw new SerializationException(SerializationError.INVALID_VALUE,
                    "'" + VERSION_FIELD + "' должно быть целым числом", "$." + VERSION_FIELD);
        }
        if (version.intValue() != SCHEMA_VERSION) {
            throw new SerializationException(SerializationError.UNSUPPORTED_VERSION,
                    "Версия схемы " + version.intValue() + " не поддерживается, ожидалась " + SCHEMA_VERSION,
                    "$." + VERSION_FIELD);
        }
    }

    private ContactDto toDto(JsonNode node, String location) throws SerializationException {
        if (!node.isObject()) {
            throw new SerializationException(SerializationError.MALFORMED, "Запись должна быть объектом", location);
        }
        try {
            return mapper.treeToValue(node, ContactDto.class);
        } catch (JsonProcessingException e) {
            throw new SerializationException(SerializationError.INVALID_VALUE,
                    "Поле неверного типа: " + e.getOriginalMessage(), location, e);
        }
    }
}
