package org.example.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CorporateContact extends Contact implements Editable {

    private static final long serialVersionUID = 1L;

    private String position;
    private String internalNumber;

    public CorporateContact(String name, String phone, String email, String organization,
                            String position, String internalNumber) {
        this(name, phone, email, organization, position, internalNumber, DEFAULT_UPDATED_AT, DEFAULT_NOTE);
    }

    public CorporateContact(String name, String phone, String email, String organization,
                            String position, String internalNumber, String updatedAt, String note) {
        super(name, phone, email, organization, updatedAt, note);
        this.position = position;
        this.internalNumber = internalNumber;
    }

    public String getPosition() {
        return position;
    }

    public String getInternalNumber() {
        return internalNumber;
    }

    @Override
    public String getType() {
        return "Corporate";
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (name == null || name.isBlank()) {
            errors.add("Пустое имя");
        }
        if (phone == null || phone.isBlank()) {
            errors.add("Пустой телефон");
        }
        if (!isValidInternalNumber(internalNumber)) {
            errors.add("Внутренний номер должен содержать только цифры");
        }
        return errors;
    }

    public static boolean isValidInternalNumber(String number) {
        return number == null || number.isEmpty() || number.matches("\\d+");
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(position == null ? "" : position);
        out.writeUTF(internalNumber == null ? "" : internalNumber);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        position = in.readUTF();
        internalNumber = in.readUTF();
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        CorporateContact other = (CorporateContact) o;
        return Objects.equals(position, other.position)
                && Objects.equals(internalNumber, other.internalNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), position, internalNumber);
    }
}

