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
        addColumn("Тип", 90, Contact::getType);
        addColumn("Имя", 230, Contact::getName);
        addColumn("Телефон", 120, Contact::getPhone);
        addColumn("Email", 160, Contact::getEmail);
        addColumn("Организация", 140, Contact::getOrganization);
        addColumn("Должность", 110, c -> c instanceof CorporateContact corporate ? corporate.getPosition() : "");
        addColumn("Внутр. номер", 90,
                c -> c instanceof CorporateContact corporate ? corporate.getInternalNumber() : "");
        setPlaceholder(new Label("Нет контактов"));
    }

    private void addColumn(String title, double width, Function<Contact, String> getter) {
        TableColumn<Contact, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
        getColumns().add(column);
    }
}
