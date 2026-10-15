package org.example.ui;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.csv.CsvParseException;
import org.example.csv.LoadResult;
import org.example.model.Contact;
import org.example.model.Editable;
import org.example.store.ContactStores;
import org.example.store.DuplicateKeyException;
import org.example.store.KeyNotFoundException;
import org.example.store.TrieStore;
import org.example.tasks.GenerateContactsTask;
import org.example.tasks.LoadCsvTask;
import org.example.tasks.SaveCsvTask;

import java.io.File;
import java.util.List;

public class MainApp extends Application {

    private static final int SUGGESTION_LIMIT = 50;
    private static final int MAX_VISIBLE_NODES = 200;
    private static final int DEFAULT_DEPTH = 25;
    private static final int MAX_DEPTH = 60;
    private static final int MAX_GENERATE = 1_000_000;
    private static final int MAX_ERRORS_SHOWN = 30;
    private static final String DEFAULT_GENERATE = "1000";

    private TrieStore<Contact> store = ContactStores.create();
    private final ObservableList<Contact> rows = FXCollections.observableArrayList();
    private final ObservableList<Contact> suggestions = FXCollections.observableArrayList();

    private Stage stage;
    private ContactTable table;
    private TriePane triePane;
    private BackgroundRunner runner;
    private String lastKey;

    private final TextField findField = new TextField();
    private final TextField prefixField = new TextField();
    private final TextField generateField = new TextField(DEFAULT_GENERATE);
    private final Spinner<Integer> depthSpinner = new Spinner<>(1, MAX_DEPTH, DEFAULT_DEPTH);
    private final Label countLabel = new Label();
    private final Label sizeLabel = new Label();
    private final Label statusLabel = new Label("Готово");
    private final ProgressBar progressBar = new ProgressBar(0);

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        runner = new BackgroundRunner(progressBar, statusLabel);
        table = new ContactTable(rows);
        triePane = new TriePane();

        SplitPane center = new SplitPane(buildLeft(), buildRight());
        center.setDividerPositions(0.55);

        BorderPane root = new BorderPane();
        root.setTop(buildToolBar());
        root.setCenter(center);
        root.setBottom(buildStatusBar());

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                lastKey = selected.getName();
                redrawTrie();
            }
        });
        prefixField.textProperty().addListener((obs, oldValue, newValue) -> {
            lastKey = newValue;
            updateSuggestions();
            redrawTrie();
        });
        depthSpinner.valueProperty().addListener((obs, oldValue, newValue) -> redrawTrie());

        stage.setTitle("Справочник контактов — Trie");
        stage.setScene(new Scene(root, 1300, 750));
        stage.show();
        refresh("Готово");
    }

    @Override
    public void stop() {
        runner.shutdown();
    }

    private ToolBar buildToolBar() {
        Button load = new Button("Загрузить CSV");
        load.setOnAction(e -> onLoad());
        Button save = new Button("Сохранить CSV");
        save.setOnAction(e -> onSave());
        Button add = new Button("Добавить");
        add.setOnAction(e -> onAdd());
        Button edit = new Button("Изменить");
        edit.setOnAction(e -> onEdit());
        Button delete = new Button("Удалить");
        delete.setOnAction(e -> onDelete());
        Button generate = new Button("Сгенерировать");
        generate.setOnAction(e -> onGenerate());
        Button clear = new Button("Очистить");
        clear.setOnAction(e -> onClear());

        generateField.setPrefColumnCount(7);

        var busy = runner.busyProperty();
        var selected = table.getSelectionModel().selectedItemProperty();
        for (Button b : new Button[]{load, save, add, generate, clear}) {
            b.disableProperty().bind(busy);
        }
        edit.disableProperty().bind(busy.or(Bindings.createBooleanBinding(
                () -> !(selected.get() instanceof Editable), selected)));
        delete.disableProperty().bind(busy.or(selected.isNull()));

        return new ToolBar(load, save, new Separator(Orientation.VERTICAL),
                add, edit, delete, new Separator(Orientation.VERTICAL),
                new Label("Кол-во:"), generateField, generate, clear);
    }

    private VBox buildLeft() {
        findField.setPromptText("Полное имя");
        findField.setOnAction(e -> onFind());
        Button find = new Button("Найти");
        find.setOnAction(e -> onFind());
        HBox.setHgrow(findField, Priority.ALWAYS);

        HBox findRow = new HBox(8, new Label("Поиск:"), findField, find);
        findRow.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, findRow, table);
        box.setPadding(new Insets(8));
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private VBox buildRight() {
        prefixField.setPromptText("например: ив");
        HBox.setHgrow(prefixField, Priority.ALWAYS);
        depthSpinner.setPrefWidth(75);
        depthSpinner.setEditable(true);

        HBox prefixRow = new HBox(8, new Label("Префикс:"), prefixField,
                new Label("Глубина:"), depthSpinner);
        prefixRow.setAlignment(Pos.CENTER_LEFT);

        ListView<Contact> suggestionList = new ListView<>(suggestions);
        suggestionList.setPrefHeight(140);
        suggestionList.setMinHeight(110);
        suggestionList.setPlaceholder(new Label("Нет совпадений"));
        suggestionList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Contact item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + "  [" + item.getType() + "]");
            }
        });
        suggestionList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, picked) -> {
            if (picked != null) {
                table.selectAndShow(picked);
            }
        });

        VBox box = new VBox(8, prefixRow, countLabel, suggestionList, triePane, buildLegend());
        box.setPadding(new Insets(8));
        VBox.setVgrow(triePane, Priority.ALWAYS);
        return box;
    }

    private FlowPane buildLegend() {
        FlowPane legend = new FlowPane(14, 4,
                legendItem(Color.WHITE, Color.web("#546e7a"), "узел"),
                legendItem(Color.web("#a5d6a7"), Color.web("#546e7a"), "есть контакт"),
                legendItem(Color.web("#ffe0b2"), Color.web("#ef6c00"), "путь последней операции"),
                new Label("число — контактов в поддереве"),
                new Label("… — ветка не показана"));
        legend.setAlignment(Pos.CENTER_LEFT);
        return legend;
    }

    private HBox legendItem(Color fill, Color stroke, String text) {
        Circle circle = new Circle(6, fill);
        circle.setStroke(stroke);
        circle.setStrokeWidth(2);
        HBox item = new HBox(4, circle, new Label(text));
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private HBox buildStatusBar() {
        Button cancel = new Button("Отмена");
        cancel.setOnAction(e -> runner.cancel());
        cancel.disableProperty().bind(runner.busyProperty().not());
        progressBar.setPrefWidth(220);
        progressBar.visibleProperty().bind(runner.busyProperty());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, statusLabel, spacer, sizeLabel, progressBar, cancel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 8, 4, 8));
        return bar;
    }

    private void onLoad() {
        FileChooser chooser = csvChooser("Открыть CSV");
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        TrieStore<Contact> loaded = ContactStores.create();
        runner.run(new LoadCsvTask(file.toPath(), loaded), result -> {
            store = loaded;
            lastKey = null;
            refresh("Загружено " + result.getLoaded() + " из " + file.getName());
            if (result.hasErrors()) {
                showLoadErrors(result);
            } else if (result.getLoaded() == 0) {
                Alerts.info("В файле нет контактов.");
            }
        }, () -> statusLabel.setText("Загрузка отменена, данные не изменились"));
    }

    private void showLoadErrors(LoadResult result) {
        List<CsvParseException> errors = result.getErrors();
        StringBuilder details = new StringBuilder();
        for (int i = 0; i < errors.size() && i < MAX_ERRORS_SHOWN; i++) {
            CsvParseException e = errors.get(i);
            details.append("Строка ").append(e.getLineNumber()).append(": ")
                   .append(e.getMessage()).append(" [").append(e.getErrorCode()).append("]\n");
        }
        if (errors.size() > MAX_ERRORS_SHOWN) {
            details.append("... и ещё ").append(errors.size() - MAX_ERRORS_SHOWN);
        }
        Alerts.warningWithDetails("Часть строк пропущена",
                "Загружено: " + result.getLoaded() + ", пропущено: " + errors.size(),
                details.toString());
    }

    private void onSave() {
        if (store.size() == 0) {
            Alerts.error("Нечего сохранять — справочник пуст.");
            return;
        }
        FileChooser chooser = csvChooser("Сохранить CSV");
        chooser.setInitialFileName("contacts.csv");
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        runner.run(new SaveCsvTask(file.toPath(), store),
                saved -> statusLabel.setText("Сохранено " + saved + " в " + file.getName()),
                () -> statusLabel.setText("Сохранение отменено"));
    }

    private void onAdd() {
        new ContactDialog(null).showAndWait().ifPresent(contact -> {
            try {
                store.add(contact);
            } catch (DuplicateKeyException e) {
                Alerts.error("Контакт с именем «" + e.getKey() + "» уже есть.");
                return;
            }
            lastKey = contact.getName();
            refresh("Добавлен: " + contact.getName());
            table.selectAndShow(contact);
        });
    }

    private void onEdit() {
        Contact selected = table.getSelectionModel().getSelectedItem();
        if (!(selected instanceof Editable)) {
            return;
        }
        new ContactDialog(selected).showAndWait().ifPresent(updated -> {
            try {
                store.update(selected.getName(), updated);
            } catch (DuplicateKeyException e) {
                Alerts.error("Контакт с именем «" + e.getKey() + "» уже есть.");
                return;
            } catch (KeyNotFoundException e) {
                Alerts.error("Контакт «" + e.getKey() + "» уже удалён.");
                refresh("Таблица обновлена");
                return;
            }
            lastKey = updated.getName();
            refresh("Изменён: " + updated.getName());
            table.selectAndShow(updated);
        });
    }

    private void onDelete() {
        Contact selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || !Alerts.confirm("Удалить «" + selected.getName() + "»?")) {
            return;
        }
        store.removeByKey(selected.getName());
        lastKey = selected.getName();
        refresh("Удалён: " + selected.getName());
    }

    private void onFind() {
        String name = findField.getText().trim();
        if (name.isEmpty()) {
            return;
        }
        lastKey = name;
        Contact found = store.findByKey(name);
        if (found == null) {
            redrawTrie();
            statusLabel.setText("Не найден: " + name);
            return;
        }
        statusLabel.setText("Найден: " + found.getName());
        table.selectAndShow(found);
        redrawTrie();
    }

    private void onGenerate() {
        int count;
        try {
            count = Integer.parseInt(generateField.getText().trim());
        } catch (NumberFormatException e) {
            Alerts.error("Количество должно быть целым числом.");
            return;
        }
        if (count < 1 || count > MAX_GENERATE) {
            Alerts.error("Количество должно быть от 1 до " + MAX_GENERATE + ".");
            return;
        }
        runner.run(new GenerateContactsTask(store, count),
                result -> refresh(result.stopped()
                        ? "Генерация остановлена, добавлено " + result.added() + " из " + result.requested()
                        : "Сгенерировано " + result.added()),
                () -> refresh("Генерация отменена"));
    }

    private void onClear() {
        if (store.size() > 0 && Alerts.confirm("Удалить все контакты?")) {
            store.clear();
            lastKey = null;
            refresh("Справочник очищен");
        }
    }

    private void refresh(String status) {
        redrawTrie();
        rows.setAll(store.toArray());
        updateSuggestions();
        sizeLabel.setText("Контактов: " + store.size() + ", узлов: " + store.nodeCount());
        statusLabel.setText(status);
    }

    private void updateSuggestions() {
        String prefix = prefixField.getText();
        suggestions.setAll(store.findByPrefix(prefix, SUGGESTION_LIMIT));
        countLabel.setText("Контактов в поддереве: " + store.countByPrefix(prefix));
    }

    private void redrawTrie() {
        String prefix = prefixField.getText().trim();
        triePane.show(store.snapshot(prefix, depthSpinner.getValue(), MAX_VISIBLE_NODES, lastKey), prefix);
    }

    private static FileChooser csvChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        return chooser;
    }
}
