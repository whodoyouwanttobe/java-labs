package org.example.ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.example.model.CorporateContact;

import java.time.LocalDate;
import java.util.List;

public class ContactDialog extends Dialog<CorporateContact> {

    private final TextField nameField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField emailField = new TextField();
    private final TextField organizationField = new TextField();
    private final TextField positionField = new TextField();
    private final TextField internalField = new TextField();
    private final TextField updatedAtField = new TextField(LocalDate.now().toString());
    private final TextField noteField = new TextField();

    public ContactDialog(CorporateContact contact) {
        setTitle(contact == null ? "Новый контакт" : "Изменение контакта");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Имя:"), nameField);
        grid.addRow(1, new Label("Телефон:"), phoneField);
        grid.addRow(2, new Label("Email:"), emailField);
        grid.addRow(3, new Label("Организация:"), organizationField);
        grid.addRow(4, new Label("Должность:"), positionField);
        grid.addRow(5, new Label("Внутр. номер:"), internalField);
        grid.addRow(6, new Label("Дата изм.:"), updatedAtField);
        grid.addRow(7, new Label("Заметка:"), noteField);
        getDialogPane().setContent(grid);

        if (contact != null) {
            nameField.setText(contact.getName());
            phoneField.setText(contact.getPhone());
            emailField.setText(contact.getEmail());
            organizationField.setText(contact.getOrganization());
            positionField.setText(contact.getPosition());
            internalField.setText(contact.getInternalNumber());
            updatedAtField.setText(contact.getUpdatedAt());
            noteField.setText(contact.getNote());
        }

        getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            List<String> errors = createContact().validate();
            if (!errors.isEmpty()) {
                Alerts.error(String.join("\n", errors));
                event.consume();
            }
        });

        setResultConverter(button -> button == ButtonType.OK ? createContact() : null);
    }

    private CorporateContact createContact() {
        return new CorporateContact(nameField.getText().trim(), phoneField.getText().trim(),
                emailField.getText().trim(), organizationField.getText().trim(),
                positionField.getText().trim(), internalField.getText().trim(),
                updatedAtField.getText().trim(), noteField.getText().trim());
    }
}

