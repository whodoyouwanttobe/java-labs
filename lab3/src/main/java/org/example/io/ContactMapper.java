package org.example.io;

import org.example.io.dto.ContactDto;
import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.model.EmergencyContact;

import java.util.Locale;

public final class ContactMapper {

    static final String CONTACT = "contact";
    static final String EMERGENCY = "emergency";
    static final String CORPORATE = "corporate";

    private ContactMapper() {
    }

    public static ContactDto toDto(Contact c) {
        if (c instanceof CorporateContact cc) {
            return new ContactDto(CORPORATE, cc.getName(), cc.getPhone(), cc.getEmail(), cc.getOrganization(),
                    cc.getPosition(), cc.getInternalNumber());
        }
        String type = c instanceof EmergencyContact ? EMERGENCY : CONTACT;
        return new ContactDto(type, c.getName(), c.getPhone(), c.getEmail(), c.getOrganization(), null, null);
    }

    public static Contact fromDto(ContactDto dto, String location) throws SerializationException {
        if (dto == null) {
            throw new SerializationException(SerializationError.MISSING_FIELD, "Пустая запись", location);
        }
        String type = require(dto.type(), "type", location).toLowerCase(Locale.ROOT);
        String name = require(dto.name(), "name", location).trim();
        if (name.isEmpty()) {
            throw new SerializationException(SerializationError.INVALID_VALUE, "Пустое имя", location + ".name");
        }
        String phone = orEmpty(dto.phone());
        String email = orEmpty(dto.email());
        String organization = orEmpty(dto.organization());
        switch (type) {
            case CONTACT:
                return new Contact(name, phone, email, organization);
            case EMERGENCY:
                return new EmergencyContact(name, phone, email, organization);
            case CORPORATE:
                String internal = orEmpty(dto.internalNumber());
                if (!internal.isEmpty() && !internal.matches("\\d+")) {
                    throw new SerializationException(SerializationError.INVALID_VALUE,
                            "Внутренний номер содержит не цифры: '" + internal + "'", location + ".internal_number");
                }
                return new CorporateContact(name, phone, email, organization, orEmpty(dto.position()), internal);
            default:
                throw new SerializationException(SerializationError.UNKNOWN_TYPE,
                        "Неизвестный тип '" + dto.type() + "'", location + ".type");
        }
    }

    private static String require(String value, String field, String location) throws SerializationException {
        if (value == null) {
            throw new SerializationException(SerializationError.MISSING_FIELD,
                    "Нет обязательного поля '" + field + "'", location);
        }
        return value;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
