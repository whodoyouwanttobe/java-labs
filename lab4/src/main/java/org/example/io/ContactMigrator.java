package org.example.io;

import org.example.io.dto.ContactDto;
import org.example.io.dto.ContactFileDto;
import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.model.EmergencyContact;

public class ContactMigrator {

    public static void migrateV1ToV2(ContactDto dto) throws SerializationException {
        if (dto == null) {
            throw new SerializationException("Пустая запись контакта");
        }
        if (dto.updatedAt == null || dto.updatedAt.isBlank()) {
            dto.updatedAt = Contact.DEFAULT_UPDATED_AT;
        }
    }

    public static void migrateV2ToV3(ContactDto dto) throws SerializationException {
        if (dto == null) {
            throw new SerializationException("Пустая запись контакта");
        }
        if (dto.note == null) {
            dto.note = Contact.DEFAULT_NOTE;
        }
    }

    public static void migrate(ContactFileDto data) throws SerializationException {
        checkVersion(data == null ? 0 : data.schemaVersion);
        if (data.contacts == null) {
            throw new SerializationException("В файле нет списка контактов");
        }
        while (data.schemaVersion < Serializer.SCHEMA_VERSION) {
            if (data.schemaVersion == 1) {
                for (ContactDto dto : data.contacts) {
                    migrateV1ToV2(dto);
                }
                data.schemaVersion = 2;
            } else if (data.schemaVersion == 2) {
                for (ContactDto dto : data.contacts) {
                    migrateV2ToV3(dto);
                }
                data.schemaVersion = 3;
            }
        }
    }

    public static void checkVersion(int version) throws SerializationException {
        if (version < 1 || version > Serializer.SCHEMA_VERSION) {
            throw new SerializationException("Версия файла " + version
                    + " не поддерживается, допустимы 1.." + Serializer.SCHEMA_VERSION);
        }
    }

    public static ContactDto toDto(Contact contact, int version) {
        ContactDto dto = new ContactDto();
        dto.type = contact.getType();
        dto.name = contact.getName();
        dto.phone = contact.getPhone();
        dto.email = contact.getEmail();
        dto.organization = contact.getOrganization();
        if (contact instanceof CorporateContact corporate) {
            dto.position = corporate.getPosition();
            dto.internalNumber = corporate.getInternalNumber();
        }
        if (version >= 2) {
            dto.updatedAt = contact.getUpdatedAt();
        }
        if (version >= 3) {
            dto.note = contact.getNote();
        }
        return dto;
    }

    public static Contact fromDto(ContactDto dto, int number) throws SerializationException {
        if (dto == null) {
            throw new SerializationException("Запись " + number + ": пустая запись");
        }
        if (dto.name == null || dto.name.isBlank()) {
            throw new SerializationException("Запись " + number + ": нет имени");
        }
        if (dto.type == null) {
            throw new SerializationException("Запись " + number + ": нет типа");
        }
        String updatedAt = dto.updatedAt == null || dto.updatedAt.isBlank()
                ? Contact.DEFAULT_UPDATED_AT : dto.updatedAt;
        String note = dto.note == null ? Contact.DEFAULT_NOTE : dto.note;

        switch (dto.type) {
            case "Contact":
                return new Contact(dto.name, dto.phone, dto.email, dto.organization, updatedAt, note);
            case "Emergency":
                return new EmergencyContact(dto.name, dto.phone, dto.email, dto.organization, updatedAt, note);
            case "Corporate":
                if (!CorporateContact.isValidInternalNumber(dto.internalNumber)) {
                    throw new SerializationException("Запись " + number + ": во внутреннем номере не только цифры");
                }
                return new CorporateContact(dto.name, dto.phone, dto.email, dto.organization,
                        dto.position, dto.internalNumber, updatedAt, note);
            default:
                throw new SerializationException("Запись " + number + ": неизвестный тип " + dto.type);
        }
    }
}

