package org.example.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.example.model.Contact;
import org.example.model.CorporateContact;

import java.util.function.Function;

class ContactTable extends TableView<Contact> {

    ContactTable(ObservableList<Contact> rows) {
        super(rows);
        getColumns().add(column("Тип", 90, Contact::getType));
        getColumns().add(column("Имя", 220, Contact::getName));
        getColumns().add(column("Телефон", 120, Contact::getPhone));
        getColumns().add(column("E-mail", 160, Contact::getEmail));
        getColumns().add(column("Организация", 150, Contact::getOrganization));
        getColumns().add(column("Должность", 110,
                c -> c instanceof CorporateContact cc ? cc.getPosition() : ""));
        getColumns().add(column("Внутр. номер", 90,
                c -> c instanceof CorporateContact cc ? cc.getInternalNumber() : ""));
        setPlaceholder(new Label("Нет контактов"));
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    void selectAndShow(Contact contact) {
        int index = getItems().indexOf(contact);
        if (index >= 0) {
            getSelectionModel().select(index);
            scrollTo(index);
        }
    }

    private static TableColumn<Contact, String> column(String title, double width,
                                                       Function<Contact, String> getter) {
        TableColumn<Contact, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(data -> value(getter.apply(data.getValue())));
        return column;
    }

    private static ObservableValue<String> value(String s) {
        return new ReadOnlyStringWrapper(s == null ? "" : s);
    }
}
