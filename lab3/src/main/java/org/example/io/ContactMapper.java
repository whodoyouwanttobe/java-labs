package org.example.io;

import org.example.io.dto.ContactDto;
import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.model.EmergencyContact;

public class ContactMapper {

    public static final String CONTACT = "contact";
    public static final String EMERGENCY = "emergency";
    public static final String CORPORATE = "corporate";

    private ContactMapper() {
    }

    public static ContactDto toDto(Contact contact) {
        if (contact instanceof CorporateContact corporate) {
            return new ContactDto(CORPORATE, corporate.getName(), corporate.getPhone(), corporate.getEmail(),
                    corporate.getOrganization(), corporate.getPosition(), corporate.getInternalNumber());
        }
        String type = contact instanceof EmergencyContact ? EMERGENCY : CONTACT;
        return new ContactDto(type, contact.getName(), contact.getPhone(), contact.getEmail(),
                contact.getOrganization(), null, null);
    }

    public static Contact fromDto(ContactDto dto, int index) throws SerializationException {
        String where = "запись " + (index + 1) + ": ";
        if (dto == null || dto.type() == null) {
            throw new SerializationException(SerializationError.MISSING_FIELD, where + "нет поля type");
        }
        if (dto.name() == null || dto.name().isBlank()) {
            throw new SerializationException(SerializationError.MISSING_FIELD, where + "нет имени");
        }
        String name = dto.name().trim();
        String phone = valueOrEmpty(dto.phone());
        String email = valueOrEmpty(dto.email());
        String organization = valueOrEmpty(dto.organization());

        switch (dto.type()) {
            case CONTACT:
                return new Contact(name, phone, email, organization);
            case EMERGENCY:
                return new EmergencyContact(name, phone, email, organization);
            case CORPORATE:
                String internalNumber = valueOrEmpty(dto.internalNumber());
                if (!CorporateContact.isValidInternalNumber(internalNumber)) {
                    throw new SerializationException(SerializationError.BAD_VALUE,
                            where + "внутренний номер содержит не цифры: " + internalNumber);
                }
                return new CorporateContact(name, phone, email, organization,
                        valueOrEmpty(dto.position()), internalNumber);
            default:
                throw new SerializationException(SerializationError.UNKNOWN_TYPE,
                        where + "неизвестный тип " + dto.type());
        }
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
