package org.example.ui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.csv.CsvParseException;
import org.example.io.Serializer;
import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.model.Editable;
import org.example.store.ContactStore;
import org.example.store.DuplicateKeyException;
import org.example.tasks.ExportTask;
import org.example.tasks.GenerateTask;
import org.example.tasks.ImportTask;
import org.example.tasks.LoadCsvTask;
import org.example.tasks.SaveCsvTask;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MainApp extends Application {

    private static final int MAX_TRIE_NODES = 300;
    private static final int MAX_GENERATE = 1_000_000;
    private static final int MAX_ERRORS_SHOWN = 20;

    private ContactStore store = new ContactStore();
    private final ObservableList<Contact> rows = FXCollections.observableArrayList();
    private String lastKey;

    private Stage stage;
    private final ContactTable table = new ContactTable(rows);
    private final TriePane triePane = new TriePane();
    private final TextField searchField = new TextField();
    private final TextField prefixField = new TextField();
    private final TextField countField = new TextField("1000");
    private final Label foundLabel = new Label();
    private final Button editButton = new Button("Изменить");
    private final HBox buttonBar = new HBox(8);
    private final HBox searchBar = new HBox(8);
    private final StatusBarView statusBar = new StatusBarView();
    private final TaskRunner runner = new TaskRunner(statusBar, busy -> {
        buttonBar.setDisable(busy);
        searchBar.setDisable(busy);
        statusBar.setCancelEnabled(busy);
    });

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        runner.setOnCancelled(() -> {
            refresh();
            statusBar.setStatus("Операция отменена");
        });
        runner.setOnError(error -> {
            statusBar.setStatus("Ошибка");
            Alerts.error(error.getMessage());
        });
        watchSelection();
        createButtonBar();
        createSearchBar();

        SplitPane center = new SplitPane(table, triePane);
        center.setDividerPositions(0.6);

        VBox top = new VBox(8, buttonBar, searchBar);
        top.setPadding(new Insets(8));

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(center);
        root.setBottom(statusBar.getRoot());

        stage.setTitle("Справочник контактов");
        stage.setScene(new Scene(root, 1280, 720));
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
        Button importButton = new Button("Импорт");
        importButton.setOnAction(e -> importFile());
        Button exportButton = new Button("Экспорт");
        exportButton.setOnAction(e -> exportFile());
        Button addButton = new Button("Добавить");
        addButton.setOnAction(e -> addContact());
        editButton.setOnAction(e -> editContact());
        editButton.setDisable(true);
        Button deleteButton = new Button("Удалить");
        deleteButton.setOnAction(e -> deleteContact());

        buttonBar.getChildren().addAll(loadButton, saveButton, importButton, exportButton,
                addButton, editButton, deleteButton);
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

    private void loadCsv() {
        File file = csvChooser().showOpenDialog(stage);
        if (file == null) {
            return;
        }
        ContactStore newStore = new ContactStore();
        LoadCsvTask task = new LoadCsvTask(file.toPath(), newStore);
        runner.setOnSuccess(() -> {
            store = newStore;
            lastKey = null;
            refresh();
            statusBar.setStatus("Загружено контактов: " + store.size());
            if (!task.getValue().isEmpty()) {
                showLoadErrors(task.getValue());
            }
        });
        runner.run(task);
    }

    private void showLoadErrors(List<CsvParseException> errors) {
        String text = "Пропущено строк: " + errors.size() + "\n";
        for (int i = 0; i < errors.size() && i < MAX_ERRORS_SHOWN; i++) {
            CsvParseException error = errors.get(i);
            text += "Строка " + error.getLineNumber() + ": " + error.getMessage() + " [" + error.getErrorCode() + "]\n";
        }
        Alerts.warning(text);
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
        runner.setOnSuccess(() -> statusBar.setStatus("Сохранено в " + file.getName()));
        runner.run(new SaveCsvTask(file.toPath(), store.toArray()));
    }

    private FileChooser csvChooser() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        return chooser;
    }

    private void importFile() {
        File file = formatChooser().showOpenDialog(stage);
        if (file == null) {
            return;
        }
        int version;
        try {
            version = Serializer.forFile(file.toPath()).readVersion(file.toPath());
        } catch (Exception e) {
            Alerts.error(e.getMessage());
            return;
        }
        if (!MigrationDialog.ask(version, Serializer.SCHEMA_VERSION)) {
            statusBar.setStatus("Миграция отменена");
            return;
        }
        ContactStore newStore = new ContactStore();
        ImportTask task = new ImportTask(file.toPath(), newStore);
        runner.setOnSuccess(() -> {
            store = newStore;
            lastKey = null;
            refresh();
            statusBar.setStatus("Импортировано контактов: " + task.getValue());
        });
        runner.run(task);
    }

    private void exportFile() {
        if (store.size() == 0) {
            Alerts.error("Справочник пуст, нечего сохранять");
            return;
        }
        Optional<Integer> version = ExportVersionDialog.chooseVersion(Serializer.SUPPORTED_VERSIONS);
        if (version.isEmpty()) {
            return;
        }
        FileChooser chooser = formatChooser();
        chooser.setInitialFileName("contacts");
        File chosen = chooser.showSaveDialog(stage);
        if (chosen == null) {
            return;
        }
        File file = withExtension(chosen, chooser.getSelectedExtensionFilter());
        runner.setOnSuccess(() -> statusBar.setStatus("Экспортировано в " + file.getName() + " (v" + version.get() + ")"));
        runner.run(new ExportTask(file.toPath(), store.toArray(), version.get()));
    }

    private File withExtension(File file, FileChooser.ExtensionFilter filter) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".json") || name.endsWith(".ctb") || filter == null) {
            return file;
        }
        return new File(file.getPath() + filter.getExtensions().get(0).substring(1));
    }

    private FileChooser formatChooser() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JSON", "*.json"),
                new FileChooser.ExtensionFilter("Бинарный файл", "*.ctb"));
        return chooser;
    }

    private void addContact() {
        new ContactDialog(null).showAndWait().ifPresent(contact -> {
            try {
                store.add(contact);
                lastKey = contact.getName();
                refresh();
                table.getSelectionModel().select(contact);
                statusBar.setStatus("Добавлен " + contact.getName());
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
        new ContactDialog((CorporateContact) selected).showAndWait().ifPresent(changed -> {
            try {
                store.update(selected.getName(), changed);
                lastKey = changed.getName();
                refresh();
                table.getSelectionModel().select(changed);
                statusBar.setStatus("Изменён " + changed.getName());
            } catch (DuplicateKeyException e) {
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
        statusBar.setStatus("Удалён " + selected.getName());
    }

    private void findContact() {
        String name = searchField.getText().trim();
        if (name.isEmpty()) {
            return;
        }
        lastKey = name;
        Contact found = store.find(name);
        if (found == null) {
            statusBar.setStatus("Не найден: " + name);
            drawTrie();
            return;
        }
        prefixField.clear();
        table.getSelectionModel().select(found);
        table.scrollTo(found);
        statusBar.setStatus("Найден: " + found.getName());
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
        runner.setOnSuccess(() -> {
            refresh();
            statusBar.setStatus("Сгенерировано контактов: " + task.getValue());
        });
        runner.run(task);
    }

    private void refresh() {
        String prefix = prefixField.getText();
        rows.setAll(store.findByPrefix(prefix, store.size()));
        foundLabel.setText("Найдено: " + store.countByPrefix(prefix));
        statusBar.setSize("Всего контактов: " + store.size() + ", узлов в дереве: " + store.nodeCount());
        drawTrie();
    }

    private void drawTrie() {
        String prefix = prefixField.getText();
        triePane.draw(store.snapshot(prefix, MAX_TRIE_NODES), prefix, lastKey);
    }
}

