package org.example.ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
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

class ContactDialog extends Dialog<Contact> {

    private static final String TYPE_CONTACT = "Контакт";
    private static final String TYPE_CORPORATE = "Корпоративный контакт";

    private final ComboBox<String> typeBox = new ComboBox<>();
    private final TextField nameField = field("Имя");
    private final TextField phoneField = field("Телефон");
    private final TextField emailField = field("E-mail");
    private final TextField orgField = field("Организация");
    private final TextField positionField = field("Должность");
    private final TextField internalField = field("Внутренний номер");
    private final Label positionLabel = new Label("Должность:");
    private final Label internalLabel = new Label("Внутр. номер:");

    ContactDialog(Contact existing) {
        setTitle(existing == null ? "Новый контакт" : "Изменение контакта");
        setHeaderText(existing == null ? "Заполните поля" : existing.getName());

        ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(saveType, cancelType);

        typeBox.getItems().addAll(TYPE_CONTACT, TYPE_CORPORATE);
        typeBox.setValue(TYPE_CONTACT);
        typeBox.valueProperty().addListener((obs, oldType, newType) -> updateCorporateFields());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(15));
        grid.addRow(0, new Label("Тип:"), typeBox);
        grid.addRow(1, new Label("Имя:"), nameField);
        grid.addRow(2, new Label("Телефон:"), phoneField);
        grid.addRow(3, new Label("E-mail:"), emailField);
        grid.addRow(4, new Label("Организация:"), orgField);
        grid.addRow(5, positionLabel, positionField);
        grid.addRow(6, internalLabel, internalField);
        getDialogPane().setContent(grid);

        if (existing != null) {
            fill(existing);
        }
        updateCorporateFields();

        Button saveButton = (Button) getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            List<String> errors = validate(buildContact());
            if (!errors.isEmpty()) {
                Alerts.warning("Некорректные данные", String.join("\n", errors));
                event.consume();
            }
        });

        setResultConverter(button -> button == saveType ? buildContact() : null);
    }

    private void fill(Contact contact) {
        typeBox.setValue(contact instanceof CorporateContact ? TYPE_CORPORATE : TYPE_CONTACT);
        typeBox.setDisable(true);
        nameField.setText(contact.getName());
        phoneField.setText(contact.getPhone());
        emailField.setText(contact.getEmail());
        orgField.setText(contact.getOrganization());
        if (contact instanceof CorporateContact cc) {
            positionField.setText(cc.getPosition());
            internalField.setText(cc.getInternalNumber());
        }
    }

    private boolean isCorporate() {
        return TYPE_CORPORATE.equals(typeBox.getValue());
    }

    private void updateCorporateFields() {
        boolean visible = isCorporate();
        for (Node node : new Node[]{positionLabel, positionField, internalLabel, internalField}) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
        if (getDialogPane().getScene() != null && getDialogPane().getScene().getWindow() != null) {
            getDialogPane().getScene().getWindow().sizeToScene();
        }
    }

    private Contact buildContact() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String org = orgField.getText().trim();
        if (isCorporate()) {
            return new CorporateContact(name, phone, email, org,
                    positionField.getText().trim(), internalField.getText().trim());
        }
        return new Contact(name, phone, email, org);
    }

    private static List<String> validate(Contact contact) {
        if (contact instanceof Editable editable) {
            return editable.validate();
        }
        return contact.getName().isEmpty() ? List.of("Пустое имя") : List.of();
    }

    private static TextField field(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefColumnCount(22);
        return field;
    }
}
