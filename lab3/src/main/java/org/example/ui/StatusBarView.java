package org.example.ui;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class StatusBarView {
    private final Label statusLabel = new Label("Готово");  // что сейчас пишет?
    private final Label sizeLabel = new Label();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Button cancelButton = new Button("Отмена");
    private final HBox root = new HBox(10);
    StatusBarView() {
        cancelButton.setDisable(true);
        progressBar.setPrefWidth(200);
        root.getChildren().addAll(statusLabel, progressBar, cancelButton, sizeLabel);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(5, 8, 5, 8));
    }
    public HBox getRoot() {
        return root;
    }

    public void setOnCancel(Runnable action) {
        cancelButton.setOnAction(e -> action.run());
    }
    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    public void setSize(String text) {
        sizeLabel.setText(text);
    }

    public void setCancelEnabled(boolean enabled) {
        cancelButton.setDisable(!enabled);
    }
    public void bindProgress(ReadOnlyDoubleProperty p) {
        progressBar.progressProperty().bind(p);
    }

    public void unbindProgress() {
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);   // сброс на начальное значение
    }

    public void bindStatus(ReadOnlyStringProperty s) {
        statusLabel.textProperty().bind(s);
    }

    public void unbindStatus() {
        statusLabel.textProperty().unbind();
    }
}