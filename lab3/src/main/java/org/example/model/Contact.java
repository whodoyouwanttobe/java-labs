package org.example.model;

import java.util.Objects;

public class Contact {
    protected final String name;
    protected final String phone;
    protected final String email;
    protected final String organization;

    public Contact(String name, String phone, String email, String organization) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.organization = organization;
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

    public Contact withName(String newName) {
        return new Contact(newName, phone, email, organization);
    }

    public String getType() {
        return "Contact";
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
                && Objects.equals(organization, other.organization);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), name, phone, email, organization);
    }

    @Override
    public String toString() {
        return String.format("Contact{name='%s', phone='%s', email='%s', org='%s'}",
                name, phone, email, organization);
    }
}
