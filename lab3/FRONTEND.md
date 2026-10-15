# Гайд для фронта, лабораторная 3: кнопки «Экспорт» и «Импорт»

Окно из лабы 2 работает без изменений. Для лабы 3 во фронт нужно добавить две кнопки: сохранить
справочник в файл JSON или бинарный и загрузить его обратно. Весь бэкенд для этого готов, нужно
только вызвать его из `MainApp`. Ниже — что вызывать, какие ошибки ловить и готовый код, который
проверен компиляцией против текущего проекта.

Всё остальное про окно (таблица, дерево, `BackgroundRunner`, `lastKey`) описано в `../lab2/FRONTEND.md`.

## 1. Что есть в бэкенде

| Класс | Зачем фронту |
|---|---|
| `org.example.io.ContactFormat` | enum форматов: `JSON` (`*.json`) и `BINARY` (`*.ctb`). Методы `title()`, `extension()`, `pattern()`, `ContactFormat.of(path)` — формат по расширению файла |
| `org.example.tasks.ExportTask` | `Task<Integer>`: сохраняет `store` в файл, результат — сколько записано |
| `org.example.tasks.ImportTask` | `Task<Integer>`: читает файл в переданное хранилище, результат — сколько прочитано |
| `org.example.io.SerializationException` | файл прочитался, но содержимое плохое. `getError()` — код (`SerializationError`), `getLocation()` — где именно |
| `java.io.IOException` | файла нет, нет прав, диск полон |

Обе задачи запускаются так же, как `LoadCsvTask` / `SaveCsvTask` в лабе 2: через `runner.run(...)`.
Прогресс у них неопределённый (`-1`), полоса прогресса просто «бегает». Отмены по ходу у них нет:
файлы на 10⁵ записей пишутся и читаются за доли секунды.

## 2. Правила, которые нужно соблюсти

1. **Импорт — в новое хранилище.** Создаёте `ContactStores.create()`, передаёте его в `ImportTask`,
   и только в `onSuccess` подменяете `store = loaded`. Если файл битый, старые данные остаются.
   Так же сделан `onLoad` для CSV.
2. **Формат определяется по расширению.** При открытии — `ContactFormat.of(file.toPath())`.
   При сохранении — по выбранному в диалоге фильтру; если пользователь не дописал расширение,
   допишите сами, иначе потом файл не откроется по расширению.
3. **Пустой справочник не экспортируем**, как и в «Сохранить CSV».
4. **Ошибку показываем с кодом и местом.** Пользователю (и преподавателю) важно видеть, что
   файл не прочитан именно потому, что `TRUNCATED` на «запись 3, байт 118».

## 3. Готовый код для `MainApp`

Импорты, которые добавятся:

```java
import org.example.io.ContactFormat;
import org.example.tasks.ExportTask;
import org.example.tasks.ImportTask;
```

Кнопки в `buildToolBar()` (рядом с CSV):

```java
Button export = new Button("Экспорт");
export.setOnAction(e -> onExport());
Button importButton = new Button("Импорт");
importButton.setOnAction(e -> onImport());
```

Не забудьте добавить их в массив кнопок, которые блокируются на время фоновой задачи
(`for (Button b : new Button[]{load, save, export, importButton, add, generate, clear})`),
и в `new ToolBar(...)`, например после `save`.

Обработчики:

```java
private void onExport() {
    if (store.size() == 0) {
        Alerts.error("Нечего сохранять — справочник пуст.");
        return;
    }
    FileChooser chooser = formatChooser("Экспорт справочника");
    chooser.setInitialFileName("contacts");
    File chosen = chooser.showSaveDialog(stage);
    if (chosen == null) {
        return;
    }
    ContactFormat format = selectedFormat(chooser);
    File file = withExtension(chosen, format);
    runner.run(new ExportTask(file.toPath(), store, format),
            saved -> statusLabel.setText("Экспортировано " + saved + " в " + file.getName()),
            () -> statusLabel.setText("Экспорт отменён"));
}

private void onImport() {
    FileChooser chooser = formatChooser("Импорт справочника");
    File file = chooser.showOpenDialog(stage);
    if (file == null) {
        return;
    }
    ContactFormat format;
    try {
        format = ContactFormat.of(file.toPath());
    } catch (IllegalArgumentException e) {
        Alerts.error(e.getMessage());
        return;
    }
    TrieStore<Contact> loaded = ContactStores.create();
    runner.run(new ImportTask(file.toPath(), loaded, format), count -> {
        store = loaded;
        lastKey = null;
        refresh("Импортировано " + count + " из " + file.getName());
    }, () -> statusLabel.setText("Импорт отменён, данные не изменились"));
}

private static FileChooser formatChooser(String title) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle(title);
    for (ContactFormat format : ContactFormat.values()) {
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                format.title() + " (" + format.pattern() + ")", format.pattern()));
    }
    return chooser;
}

private static ContactFormat selectedFormat(FileChooser chooser) {
    int index = chooser.getExtensionFilters().indexOf(chooser.getSelectedExtensionFilter());
    return index < 0 ? ContactFormat.JSON : ContactFormat.values()[index];
}

private static File withExtension(File file, ContactFormat format) {
    String suffix = "." + format.extension();
    return file.getName().toLowerCase().endsWith(suffix) ? file : new File(file.getPath() + suffix);
}
```

`selectedFormat` работает, потому что фильтры добавляются в том же порядке, что `ContactFormat.values()`.

## 4. Как показать ошибку чтения красиво

Сейчас при падении задачи `BackgroundRunner` показывает `Alerts.error(error.getMessage())`.
Для `SerializationException` сообщение уже содержит место (`... ($.contacts[1])`), так что работать
будет и без изменений. Чтобы был виден ещё и код, поменяйте ветку `setOnFailed` в
`BackgroundRunner.run`:

```java
task.setOnFailed(e -> {
    finish();
    Throwable error = task.getException();
    statusLabel.setText("Ошибка");
    if (error instanceof SerializationException se) {
        Alerts.warningWithDetails("Файл не прочитан", se.getMessage(),
                "Код: " + se.getError() + "\nГде: " + se.getLocation());
    } else {
        Alerts.error(error == null ? "Неизвестная ошибка" : error.getMessage());
    }
});
```

(импорт `org.example.io.SerializationException`). `getLocation()` может быть `null`, например
для `EMPTY_FILE`, тогда в окне будет «Где: null» — можно заменить на «—».

## 5. Как проверить

1. Сгенерировать 1000 контактов → «Экспорт» → фильтр JSON → `contacts` → появился `contacts.json`.
   Открыть его в блокноте: вверху `"schema_version" : 1`.
2. То же с фильтром «Бинарный» → `contacts.ctb`.
3. «Очистить» → «Импорт» → `contacts.ctb` → в таблице снова 1000 контактов, дерево на месте.
4. Импорт файлов из `src/test/resources/fixtures`:
   - `valid.json`, `valid.ctb` — 4 контакта;
   - `broken.json` — окно ошибки `MALFORMED` со строкой и столбцом, старые данные в таблице остались;
   - `truncated.ctb` — `TRUNCATED`;
   - `future-version.json` — `UNSUPPORTED_VERSION`;
   - `duplicate.json` — `DUPLICATE_KEY`.
5. Переименовать `contacts.json` в `contacts.ctb` и импортировать — `BAD_MAGIC`
   («это не файл контактов»).

## 6. Вопросы по фронту лабы 3 на защите

- **Почему импорт не сразу в текущий `store`?** Если файл окажется битым на середине, часть записей
  уже была бы в таблице. Новое хранилище подменяется целиком только после успеха.
- **Почему задачи в фоне, если файлы маленькие?** Файл может лежать на сетевом диске или быть большим,
  а FX-поток нельзя блокировать: окно перестанет перерисовываться.
- **Откуда UI знает формат?** Из расширения (`ContactFormat.of`). Сам UI ничего не знает о JSON и
  байтах, он передаёт `ContactFormat` в задачу.
