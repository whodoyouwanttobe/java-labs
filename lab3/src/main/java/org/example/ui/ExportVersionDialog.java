package org.example.ui;

import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.Optional;

class ExportVersionDialog {
    private ExportVersionDialog() {
    }
    static Optional<Integer> chooseVersion(List<Integer> versions) {
        if (versions == null || versions.isEmpty()) {
            return Optional.empty();
        }
        ChoiceBox<Integer> box = new ChoiceBox<>(FXCollections.observableArrayList(versions));
        int current = versions.get(0);
        box.setValue(current);
        Label warning = new Label();
        box.valueProperty().addListener((obs, oldV, newV) -> warning.setText(lossesText(newV, current)));
        warning.setText(lossesText(box.getValue(), current));
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Экспорт");
        dialog.getDialogPane().setContent(new VBox(8, new Label("Версия:"), box, warning));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == ButtonType.OK ? box.getValue() : null);
        return dialog.showAndWait();
    }
    private static String lossesText(int version, int current) {
        if (version == current) {
            return "";
        }
        if (version == 2) {
            return "Версия 2 не хранит: заметки.";
        }
        if (version == 1) {
            return "Версия 1 не хранит: заметки и дату изменения.";
        }
        return "";
    }
}
