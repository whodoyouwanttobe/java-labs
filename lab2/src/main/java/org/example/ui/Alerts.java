package org.example.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.util.Optional;

final class Alerts {

    static final ButtonType YES = new ButtonType("Да", ButtonBar.ButtonData.YES);
    static final ButtonType NO = new ButtonType("Нет", ButtonBar.ButtonData.NO);

    private Alerts() {
    }

    static void error(String message) {
        show(Alert.AlertType.ERROR, "Ошибка", null, message);
    }

    static void info(String message) {
        show(Alert.AlertType.INFORMATION, "Информация", null, message);
    }

    static void warning(String header, String message) {
        show(Alert.AlertType.WARNING, "Внимание", header, message);
    }

    static void warningWithDetails(String header, String message, String details) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Внимание");
        alert.setHeaderText(header);
        TextArea area = new TextArea(details);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(8);
        alert.getDialogPane().setContent(new VBox(8, new Label(message), area));
        alert.showAndWait();
    }

    static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, YES, NO);
        alert.setTitle("Подтверждение");
        alert.setHeaderText(null);
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == YES;
    }

    private static void show(Alert.AlertType type, String title, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
