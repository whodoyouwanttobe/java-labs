package org.example.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.example.model.Contact;
import org.example.model.CorporateContact;

import java.util.function.Function;

public class ContactTable extends TableView<Contact> {

    public ContactTable(ObservableList<Contact> rows) {
        super(rows);
        addColumn("Тип", 80, Contact::getType);
        addColumn("Имя", 190, Contact::getName);
        addColumn("Телефон", 110, Contact::getPhone);
        addColumn("Email", 140, Contact::getEmail);
        addColumn("Организация", 120, Contact::getOrganization);
        addColumn("Должность", 100, c -> c instanceof CorporateContact corporate ? corporate.getPosition() : "");
        addColumn("Внутр. номер", 80,
                c -> c instanceof CorporateContact corporate ? corporate.getInternalNumber() : "");
        addColumn("Дата изм.", 95, Contact::getUpdatedAt);
        addColumn("Заметка", 130, Contact::getNote);
        setPlaceholder(new Label("Нет контактов"));
    }

    private void addColumn(String title, double width, Function<Contact, String> getter) {
        TableColumn<Contact, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
        getColumns().add(column);
    }
}

