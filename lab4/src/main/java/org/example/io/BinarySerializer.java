package org.example.io;

import org.example.io.dto.ContactDto;
import org.example.io.dto.ContactFileDto;
import org.example.model.Contact;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BinarySerializer implements Serializer {

    private static final int LINE_STEP = 40;

    @Override
    public String getFormat() {
        return "ctb";
    }

    @Override
    public void save(Contact[] contacts, Path file, int version) throws IOException {
        if (version < MIN_SCHEMA_VERSION || version > SCHEMA_VERSION) {
            throw new IllegalArgumentException("Неверная версия: " + version);
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
            Contact.setWriteVersion(version);
            out.writeInt(version);
            out.writeObject(contacts);
        } finally {
            Contact.setWriteVersion(SCHEMA_VERSION);
        }
        Files.write(file, pack(buffer.toByteArray()));
    }

    @Override
    public int readVersion(Path file) throws IOException, SerializationException {
        try (ObjectInputStream in = openStream(file)) {
            int version = in.readInt();
            ContactMigrator.checkVersion(version);
            return version;
        } catch (EOFException e) {
            throw new SerializationException("Файл пустой или обрывается раньше времени");
        } catch (ObjectStreamException e) {
            throw new SerializationException("Файл повреждён или это не файл справочника");
        }
    }

    @Override
    public Contact[] load(Path file) throws IOException, SerializationException {
        try (ObjectInputStream in = openStream(file)) {
            int version = in.readInt();
            ContactMigrator.checkVersion(version);
            Contact[] raw = (Contact[]) in.readObject();
            ContactFileDto data = new ContactFileDto();
            data.schemaVersion = version;
            data.contacts = new ContactDto[raw.length];
            for (int i = 0; i < raw.length; i++) {
                data.contacts[i] = ContactMigrator.toDto(raw[i], version);
            }
            ContactMigrator.migrate(data);
            Contact[] contacts = new Contact[data.contacts.length];
            for (int i = 0; i < contacts.length; i++) {
                contacts[i] = ContactMigrator.fromDto(data.contacts[i], i + 1);
            }
            return contacts;
        } catch (EOFException e) {
            throw new SerializationException("Файл пустой или обрывается раньше времени");
        } catch (ObjectStreamException | ClassNotFoundException | ClassCastException e) {
            throw new SerializationException("Файл повреждён или это не файл справочника");
        }
    }

    private ObjectInputStream openStream(Path file) throws IOException {
        return new ObjectInputStream(new ByteArrayInputStream(unpack(Files.readAllBytes(file))));
    }

    private byte[] pack(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < data.length; i++) {
            out.write(data[i] ^ (i * 73 + 0x5A));
            if ((i + 1) % LINE_STEP == 0) {
                out.write('\n');
            }
        }
        return out.toByteArray();
    }

    private byte[] unpack(byte[] bytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < bytes.length; i++) {
            if (i % (LINE_STEP + 1) == LINE_STEP) {
                continue;
            }
            int k = out.size();
            out.write(bytes[i] ^ (k * 73 + 0x5A));
        }
        return out.toByteArray();
    }
}
