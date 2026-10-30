package org.example.ui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.csv.CsvParseException;
import org.example.csv.LoadResult;
import org.example.model.Contact;
import org.example.model.Editable;
import org.example.store.ContactStore;
import org.example.store.DuplicateKeyException;
import org.example.store.KeyNotFoundException;
import org.example.tasks.GenerateTask;
import org.example.tasks.LoadCsvTask;
import org.example.tasks.SaveCsvTask;

import java.io.File;

public class MainApp extends Application {

    private static final int MAX_TRIE_NODES = 300;
    private static final int MAX_GENERATE = 1_000_000;
    private static final int MAX_ERRORS_SHOWN = 50;

    private ContactStore store = new ContactStore();
    private final ObservableList<Contact> rows = FXCollections.observableArrayList();
    private String lastKey;
    private Task<?> currentTask;

    private Stage stage;
    private final ContactTable table = new ContactTable(rows);
    private final TriePane triePane = new TriePane();
    private final TextField searchField = new TextField();
    private final TextField prefixField = new TextField();
    private final TextField countField = new TextField("1000");
    private final Label foundLabel = new Label();
    private final Label sizeLabel = new Label();
    private final Label statusLabel = new Label("Готово");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Button cancelButton = new Button("Отмена");
    private final Button editButton = new Button("Изменить");
    private final HBox buttonBar = new HBox(8);
    private final HBox searchBar = new HBox(8);

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        watchSelection();
        createButtonBar();
        createSearchBar();

        SplitPane center = new SplitPane(table, triePane);
        center.setDividerPositions(0.55);

        VBox top = new VBox(8, buttonBar, searchBar);
        top.setPadding(new Insets(8));

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(center);
        root.setBottom(createStatusBar());

        stage.setTitle("Справочник контактов");
        stage.setScene(new Scene(root, 1200, 700));
        stage.show();
        refresh();
    }

    private void watchSelection() {
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            editButton.setDisable(!(selected instanceof Editable));
            if (selected != null) {
                lastKey = selected.getName();
                drawTrie();
            }
        });
    }

    private void createButtonBar() {
        Button loadButton = new Button("Загрузить CSV");
        loadButton.setOnAction(e -> loadCsv());
        Button saveButton = new Button("Сохранить CSV");
        saveButton.setOnAction(e -> saveCsv());
        Button addButton = new Button("Добавить");
        addButton.setOnAction(e -> addContact());
        editButton.setOnAction(e -> editContact());
        editButton.setDisable(true);
        Button deleteButton = new Button("Удалить");
        deleteButton.setOnAction(e -> deleteContact());
        Button clearButton = new Button("Очистить");
        clearButton.setOnAction(e -> clearAll());

        buttonBar.getChildren().addAll(loadButton, saveButton, addButton, editButton, deleteButton, clearButton);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
    }

    private void createSearchBar() {
        searchField.setPromptText("Полное имя");
        searchField.setOnAction(e -> findContact());
        Button findButton = new Button("Найти");
        findButton.setOnAction(e -> findContact());

        prefixField.setPromptText("Начало имени");
        prefixField.textProperty().addListener((obs, oldValue, newValue) -> refresh());

        countField.setPrefColumnCount(7);
        Button generateButton = new Button("Сгенерировать");
        generateButton.setOnAction(e -> generate());

        searchBar.getChildren().addAll(new Label("Поиск:"), searchField, findButton,
                new Label("Префикс:"), prefixField, foundLabel,
                new Label("Количество:"), countField, generateButton);
        searchBar.setAlignment(Pos.CENTER_LEFT);
    }

    private HBox createStatusBar() {
        cancelButton.setDisable(true);
        cancelButton.setOnAction(e -> currentTask.cancel());
        progressBar.setPrefWidth(200);

        HBox statusBar = new HBox(10, statusLabel, progressBar, cancelButton, sizeLabel);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(5, 8, 5, 8));
        return statusBar;
    }

    private void loadCsv() {
        File file = csvChooser().showOpenDialog(stage);
        if (file == null) {
            return;
        }
        ContactStore newStore = new ContactStore();
        LoadCsvTask task = new LoadCsvTask(file.toPath(), newStore);
        runTask(task, () -> {
            store = newStore;
            lastKey = null;
            refresh();
            LoadResult result = task.getValue();
            statusLabel.setText("Загружено контактов: " + result.getLoaded());
            if (result.hasErrors()) {
                showLoadErrors(result);
            }
        });
    }

    private void showLoadErrors(LoadResult result) {
        StringBuilder text = new StringBuilder();
        int shown = 0;
        for (CsvParseException error : result.getErrors()) {
            if (shown == MAX_ERRORS_SHOWN) {
                text.append("...");
                break;
            }
            text.append("Строка ").append(error.getLineNumber()).append(": ")
                    .append(error.getMessage()).append(" [").append(error.getErrorCode()).append("]\n");
            shown++;
        }
        Alerts.warning("Пропущено строк: " + result.getErrors().size(), text.toString());
    }

    private void saveCsv() {
        if (store.size() == 0) {
            Alerts.error("Справочник пуст, нечего сохранять");
            return;
        }
        FileChooser chooser = csvChooser();
        chooser.setInitialFileName("contacts.csv");
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        runTask(new SaveCsvTask(file.toPath(), store.toArray()),
                () -> statusLabel.setText("Сохранено в " + file.getName()));
    }

    private FileChooser csvChooser() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        return chooser;
    }

    private void addContact() {
        new ContactDialog(null).showAndWait().ifPresent(contact -> {
            try {
                store.add(contact);
                lastKey = contact.getName();
                refresh();
                table.getSelectionModel().select(contact);
                statusLabel.setText("Добавлен " + contact.getName());
            } catch (DuplicateKeyException e) {
                Alerts.error(e.getMessage());
            }
        });
    }

    private void editContact() {
        Contact selected = table.getSelectionModel().getSelectedItem();
        if (!(selected instanceof Editable)) {
            return;
        }
        new ContactDialog(selected).showAndWait().ifPresent(changed -> {
            try {
                store.update(selected.getName(), changed);
                lastKey = changed.getName();
                refresh();
                table.getSelectionModel().select(changed);
                statusLabel.setText("Изменён " + changed.getName());
            } catch (DuplicateKeyException | KeyNotFoundException e) {
                Alerts.error(e.getMessage());
            }
        });
    }

    private void deleteContact() {
        Contact selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || !Alerts.confirm("Удалить " + selected.getName() + "?")) {
            return;
        }
        store.remove(selected.getName());
        lastKey = selected.getName();
        refresh();
        statusLabel.setText("Удалён " + selected.getName());
    }

    private void clearAll() {
        if (store.size() > 0 && Alerts.confirm("Удалить все контакты?")) {
            store.clear();
            lastKey = null;
            refresh();
            statusLabel.setText("Справочник очищен");
        }
    }

    private void findContact() {
        String name = searchField.getText().trim();
        if (name.isEmpty()) {
            return;
        }
        lastKey = name;
        Contact found = store.find(name);
        if (found == null) {
            statusLabel.setText("Не найден: " + name);
            drawTrie();
            return;
        }
        prefixField.clear();
        table.getSelectionModel().select(found);
        table.scrollTo(found);
        statusLabel.setText("Найден: " + found.getName());
        drawTrie();
    }

    private void generate() {
        int count;
        try {
            count = Integer.parseInt(countField.getText().trim());
        } catch (NumberFormatException e) {
            Alerts.error("Количество должно быть целым числом");
            return;
        }
        if (count < 1 || count > MAX_GENERATE) {
            Alerts.error("Количество должно быть от 1 до " + MAX_GENERATE);
            return;
        }
        GenerateTask task = new GenerateTask(store, count);
        runTask(task, () -> {
            refresh();
            statusLabel.setText("Сгенерировано контактов: " + task.getValue());
        });
    }

    private void runTask(Task<?> task, Runnable onSuccess) {
        currentTask = task;
        setBusy(true);
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            finishTask();
            onSuccess.run();
        });
        task.setOnCancelled(e -> {
            finishTask();
            refresh();
            statusLabel.setText("Операция отменена");
        });
        task.setOnFailed(e -> {
            finishTask();
            statusLabel.setText("Ошибка");
            Alerts.error(task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void finishTask() {
        progressBar.progressProperty().unbind();
        statusLabel.textProperty().unbind();
        progressBar.setProgress(0);
        setBusy(false);
    }

    private void setBusy(boolean busy) {
        buttonBar.setDisable(busy);
        searchBar.setDisable(busy);
        cancelButton.setDisable(!busy);
    }

    private void refresh() {
        String prefix = prefixField.getText();
        rows.setAll(store.findByPrefix(prefix, store.size()));
        foundLabel.setText("Найдено: " + store.countByPrefix(prefix));
        sizeLabel.setText("Всего контактов: " + store.size() + ", узлов в дереве: " + store.nodeCount());
        drawTrie();
    }

    private void drawTrie() {
        String prefix = prefixField.getText();
        triePane.draw(store.snapshot(prefix, MAX_TRIE_NODES), prefix, lastKey);
    }
}
