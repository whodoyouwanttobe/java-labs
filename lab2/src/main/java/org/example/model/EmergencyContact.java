package org.example.model;

public class EmergencyContact extends Contact {

    public EmergencyContact(String name, String phone, String email, String organization) {
        super(name, phone, email, organization);
    }

    @Override
    public String getType() {
        return "Emergency";
    }
}
