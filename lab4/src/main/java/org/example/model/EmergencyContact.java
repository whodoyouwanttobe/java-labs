package org.example.model;

public class EmergencyContact extends Contact {

    private static final long serialVersionUID = 1L;

    public EmergencyContact(String name, String phone, String email, String organization) {
        super(name, phone, email, organization);
    }

    public EmergencyContact(String name, String phone, String email, String organization,
                            String updatedAt, String note) {
        super(name, phone, email, organization, updatedAt, note);
    }

    @Override
    public String getType() {
        return "Emergency";
    }
}

