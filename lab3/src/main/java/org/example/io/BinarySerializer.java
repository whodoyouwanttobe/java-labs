package org.example.io;

import org.example.io.dto.ContactDto;
import org.example.model.Contact;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BinarySerializer implements Serializer {

    private static final int SIGNATURE = 0x43544B42;
    private static final int MAX_COUNT = 10_000_000;

    @Override
    public String getFormat() {
        return "ctb";
    }

    @Override
    public void save(Contact[] contacts, Path file) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.writeInt(SIGNATURE);
            out.writeInt(SCHEMA_VERSION);
            out.writeInt(contacts.length);
            for (Contact contact : contacts) {
                ContactDto dto = ContactMapper.toDto(contact);
                out.writeUTF(dto.type());
                out.writeUTF(dto.name());
                out.writeUTF(dto.phone());
                out.writeUTF(dto.email());
                out.writeUTF(dto.organization());
                if (dto.type().equals(ContactMapper.CORPORATE)) {
                    out.writeUTF(dto.position());
                    out.writeUTF(dto.internalNumber());
                }
            }
        }
    }

    @Override
    public Contact[] load(Path file) throws IOException, SerializationException {
        if (Files.size(file) == 0) {
            throw new SerializationException(SerializationError.EMPTY_FILE, "Файл пустой");
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (in.readInt() != SIGNATURE) {
                throw new SerializationException(SerializationError.BAD_FORMAT, "Это не файл справочника");
            }
            int version = in.readInt();
            if (version != SCHEMA_VERSION) {
                throw new SerializationException(SerializationError.BAD_VERSION,
                        "Версия " + version + " не поддерживается, нужна " + SCHEMA_VERSION);
            }
            int count = in.readInt();
            if (count < 0 || count > MAX_COUNT) {
                throw new SerializationException(SerializationError.BAD_FORMAT, "Неверное число записей: " + count);
            }

            Contact[] contacts = new Contact[count];
            for (int i = 0; i < count; i++) {
                contacts[i] = ContactMapper.fromDto(readContact(in), i);
            }
            if (in.read() != -1) {
                throw new SerializationException(SerializationError.BAD_FORMAT, "Лишние данные в конце файла");
            }
            return contacts;
        } catch (EOFException e) {
            throw new SerializationException(SerializationError.TRUNCATED, "Файл обрывается раньше времени");
        } catch (UTFDataFormatException e) {
            throw new SerializationException(SerializationError.BAD_FORMAT, "Повреждена строка в файле");
        }
    }

    private ContactDto readContact(DataInputStream in) throws IOException {
        String type = in.readUTF();
        String name = in.readUTF();
        String phone = in.readUTF();
        String email = in.readUTF();
        String organization = in.readUTF();
        String position = null;
        String internalNumber = null;
        if (type.equals(ContactMapper.CORPORATE)) {
            position = in.readUTF();
            internalNumber = in.readUTF();
        }
        return new ContactDto(type, name, phone, email, organization, position, internalNumber);
    }
}
