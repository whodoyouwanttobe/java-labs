package org.example.ui;


import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

class MigrationDialog {

    private MigrationDialog() {
    }
    static boolean ask(int from, int to) {
        if (from >= to) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Файл версии " + from + ". Мигрировать до " + to + "?",
                ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}
