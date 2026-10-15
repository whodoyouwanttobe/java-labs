package org.example.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CorporateContact extends Contact implements Editable {
    private final String position;
    private final String internalNumber;

    public CorporateContact(String name, String phone, String email, String organization,
                            String position, String internalNumber) {
        super(name, phone, email, organization);
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
    public CorporateContact withName(String newName) {
        return new CorporateContact(newName, phone, email, organization, position, internalNumber);
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
        if (internalNumber != null && !internalNumber.isEmpty() && !internalNumber.matches("\\d+")) {
            errors.add("Внутренний номер должен содержать только цифры");
        }
        return errors;
    }

    @Override
    public String getType() {
        return "Corporate";
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o)
                && Objects.equals(position, ((CorporateContact) o).position)
                && Objects.equals(internalNumber, ((CorporateContact) o).internalNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), position, internalNumber);
    }

    @Override
    public String toString() {
        return "CorporateContact{name='" + name + "', position='" + position + "', intNum='" + internalNumber + "'}";
    }
}
