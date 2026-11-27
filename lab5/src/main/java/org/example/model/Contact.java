package org.example.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;

public class Contact implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_UPDATED_AT = "1970-01-01";
    public static final String DEFAULT_NOTE = "";

    private static int writeVersion = 3;

    protected String name;
    protected String phone;
    protected String email;
    protected String organization;
    protected String updatedAt;
    protected String note;

    public Contact(String name, String phone, String email, String organization) {
        this(name, phone, email, organization, DEFAULT_UPDATED_AT, DEFAULT_NOTE);
    }

    public Contact(String name, String phone, String email, String organization,
                   String updatedAt, String note) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.organization = organization;
        this.updatedAt = updatedAt == null || updatedAt.isBlank() ? DEFAULT_UPDATED_AT : updatedAt;
        this.note = note == null ? DEFAULT_NOTE : note;
    }

    public static void setWriteVersion(int version) {
        writeVersion = version;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getOrganization() {
        return organization;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getNote() {
        return note;
    }

    public String getType() {
        return "Contact";
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(writeVersion);
        out.writeUTF(name == null ? "" : name);
        out.writeUTF(phone == null ? "" : phone);
        out.writeUTF(email == null ? "" : email);
        out.writeUTF(organization == null ? "" : organization);
        if (writeVersion >= 2) {
            out.writeUTF(updatedAt == null ? DEFAULT_UPDATED_AT : updatedAt);
        }
        if (writeVersion >= 3) {
            out.writeUTF(note == null ? DEFAULT_NOTE : note);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int version = in.readInt();
        name = in.readUTF();
        phone = in.readUTF();
        email = in.readUTF();
        organization = in.readUTF();
        updatedAt = version >= 2 ? in.readUTF() : null;
        note = version >= 3 ? in.readUTF() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Contact other = (Contact) o;
        return Objects.equals(name, other.name)
                && Objects.equals(phone, other.phone)
                && Objects.equals(email, other.email)
                && Objects.equals(organization, other.organization)
                && Objects.equals(updatedAt, other.updatedAt)
                && Objects.equals(note, other.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, phone, email, organization, updatedAt, note);
    }
}

