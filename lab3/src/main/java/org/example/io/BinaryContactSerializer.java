package org.example.io;

import org.example.io.dto.ContactDto;
import org.example.model.Contact;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class BinaryContactSerializer implements Serializer<Contact> {

    static final int MAGIC = 0x43544B42;
    static final byte TYPE_CONTACT = 1;
    static final byte TYPE_EMERGENCY = 2;
    static final byte TYPE_CORPORATE = 3;
    static final int MAX_STRING_BYTES = 4096;
    static final int MAX_COUNT = 10_000_000;
    private static final int NULL_STRING = -1;

    @Override
    public void write(Contact[] items, OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(new BufferedOutputStream(out));
        data.writeInt(MAGIC);
        data.writeShort(SCHEMA_VERSION);
        data.writeInt(items.length);
        for (Contact contact : items) {
            ContactDto dto = ContactMapper.toDto(contact);
            data.writeByte(typeCode(dto.type()));
            writeString(data, dto.name());
            writeString(data, dto.phone());
            writeString(data, dto.email());
            writeString(data, dto.organization());
            if (ContactMapper.CORPORATE.equals(dto.type())) {
                writeString(data, dto.position());
                writeString(data, dto.internalNumber());
            }
        }
        data.flush();
    }

    @Override
    public Contact[] read(InputStream in) throws IOException, SerializationException {
        CountingDataInput data = new CountingDataInput(new BufferedInputStream(in));
        try {
            int first = data.peekFirstByte();
            if (first < 0) {
                throw new SerializationException(SerializationError.EMPTY_FILE, "Файл пустой", null);
            }
            int magic = data.readInt();
            if (magic != MAGIC) {
                throw new SerializationException(SerializationError.BAD_MAGIC,
                        String.format("Это не файл контактов: сигнатура 0x%08X вместо 0x%08X", magic, MAGIC),
                        "байт 0");
            }
            int version = data.readUnsignedShort();
            if (version != SCHEMA_VERSION) {
                throw new SerializationException(SerializationError.UNSUPPORTED_VERSION,
                        "Версия формата " + version + " не поддерживается, ожидалась " + SCHEMA_VERSION, "байт 4");
            }
            long countAt = data.position();
            int count = data.readInt();
            if (count < 0 || count > MAX_COUNT) {
                throw new SerializationException(SerializationError.CORRUPTED,
                        "Недопустимое число записей: " + count, "байт " + countAt);
            }
            Contact[] result = new Contact[count];
            for (int i = 0; i < count; i++) {
                result[i] = readContact(data, i);
            }
            if (data.read() >= 0) {
                throw new SerializationException(SerializationError.CORRUPTED,
                        "После последней записи есть лишние байты", "байт " + (data.position() - 1));
            }
            return result;
        } catch (EOFException e) {
            throw new SerializationException(SerializationError.TRUNCATED,
                    "Файл обрывается раньше, чем закончились данные", "байт " + data.position(), e);
        }
    }

    private Contact readContact(CountingDataInput data, int index) throws IOException, SerializationException {
        String location = "запись " + index + ", байт " + data.position();
        byte type = data.readByte();
        String typeName = typeName(type, location);
        String name = readString(data);
        String phone = readString(data);
        String email = readString(data);
        String organization = readString(data);
        String position = null;
        String internal = null;
        if (type == TYPE_CORPORATE) {
            position = readString(data);
            internal = readString(data);
        }
        return ContactMapper.fromDto(new ContactDto(typeName, name, phone, email, organization, position, internal),
                location);
    }

    private static byte typeCode(String type) {
        return switch (type) {
            case ContactMapper.EMERGENCY -> TYPE_EMERGENCY;
            case ContactMapper.CORPORATE -> TYPE_CORPORATE;
            default -> TYPE_CONTACT;
        };
    }

    private static String typeName(byte code, String location) throws SerializationException {
        return switch (code) {
            case TYPE_CONTACT -> ContactMapper.CONTACT;
            case TYPE_EMERGENCY -> ContactMapper.EMERGENCY;
            case TYPE_CORPORATE -> ContactMapper.CORPORATE;
            default -> throw new SerializationException(SerializationError.UNKNOWN_TYPE,
                    "Неизвестный код типа " + code, location);
        };
    }

    private static void writeString(DataOutputStream data, String value) throws IOException {
        if (value == null) {
            data.writeShort(NULL_STRING);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_BYTES) {
            throw new IOException("Строка длиннее " + MAX_STRING_BYTES + " байт: " + value.substring(0, 20) + "...");
        }
        data.writeShort(bytes.length);
        data.write(bytes);
    }

    private static String readString(CountingDataInput data) throws IOException, SerializationException {
        long at = data.position();
        short length = data.readShort();
        if (length == NULL_STRING) {
            return null;
        }
        if (length < 0 || length > MAX_STRING_BYTES) {
            throw new SerializationException(SerializationError.CORRUPTED,
                    "Недопустимая длина строки: " + length, "байт " + at);
        }
        byte[] bytes = new byte[length];
        data.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static final class CountingDataInput {
        private final BufferedInputStream in;
        private final DataInputStream data;
        private long position;

        CountingDataInput(BufferedInputStream in) {
            this.in = in;
            this.data = new DataInputStream(in);
        }

        long position() {
            return position;
        }

        int peekFirstByte() throws IOException {
            in.mark(1);
            int b = in.read();
            in.reset();
            return b;
        }

        int read() throws IOException {
            int b = data.read();
            if (b >= 0) {
                position++;
            }
            return b;
        }

        int readInt() throws IOException {
            int v = data.readInt();
            position += Integer.BYTES;
            return v;
        }

        int readUnsignedShort() throws IOException {
            int v = data.readUnsignedShort();
            position += Short.BYTES;
            return v;
        }

        short readShort() throws IOException {
            short v = data.readShort();
            position += Short.BYTES;
            return v;
        }

        byte readByte() throws IOException {
            byte v = data.readByte();
            position++;
            return v;
        }

        void readFully(byte[] buffer) throws IOException {
            data.readFully(buffer);
            position += buffer.length;
        }
    }
}
