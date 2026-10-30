package org.example.ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.model.Editable;

import java.util.List;

public class ContactDialog extends Dialog<Contact> {

    private static final String SIMPLE = "Контакт";
    private static final String CORPORATE = "Корпоративный";

    private final ComboBox<String> typeBox = new ComboBox<>();
    private final TextField nameField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField emailField = new TextField();
    private final TextField organizationField = new TextField();
    private final TextField positionField = new TextField();
    private final TextField internalField = new TextField();

    public ContactDialog(Contact contact) {
        setTitle(contact == null ? "Новый контакт" : "Изменение контакта");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        typeBox.getItems().addAll(SIMPLE, CORPORATE);
        typeBox.setValue(SIMPLE);
        typeBox.setOnAction(e -> updateFields());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Тип:"), typeBox);
        grid.addRow(1, new Label("Имя:"), nameField);
        grid.addRow(2, new Label("Телефон:"), phoneField);
        grid.addRow(3, new Label("Email:"), emailField);
        grid.addRow(4, new Label("Организация:"), organizationField);
        grid.addRow(5, new Label("Должность:"), positionField);
        grid.addRow(6, new Label("Внутр. номер:"), internalField);
        getDialogPane().setContent(grid);

        if (contact != null) {
            fill(contact);
        }
        updateFields();

        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            List<String> errors = validate(createContact());
            if (!errors.isEmpty()) {
                Alerts.error(String.join("\n", errors));
                event.consume();
            }
        });

        setResultConverter(button -> button == ButtonType.OK ? createContact() : null);
    }

    private void fill(Contact contact) {
        typeBox.setValue(contact instanceof CorporateContact ? CORPORATE : SIMPLE);
        typeBox.setDisable(true);
        nameField.setText(contact.getName());
        phoneField.setText(contact.getPhone());
        emailField.setText(contact.getEmail());
        organizationField.setText(contact.getOrganization());
        if (contact instanceof CorporateContact corporate) {
            positionField.setText(corporate.getPosition());
            internalField.setText(corporate.getInternalNumber());
        }
    }

    private void updateFields() {
        boolean corporate = CORPORATE.equals(typeBox.getValue());
        positionField.setDisable(!corporate);
        internalField.setDisable(!corporate);
    }

    private Contact createContact() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String organization = organizationField.getText().trim();
        if (CORPORATE.equals(typeBox.getValue())) {
            return new CorporateContact(name, phone, email, organization,
                    positionField.getText().trim(), internalField.getText().trim());
        }
        return new Contact(name, phone, email, organization);
    }

    private List<String> validate(Contact contact) {
        if (contact instanceof Editable editable) {
            return editable.validate();
        }
        if (contact.getName().isEmpty()) {
            return List.of("Пустое имя");
        }
        return List.of();
    }
}
