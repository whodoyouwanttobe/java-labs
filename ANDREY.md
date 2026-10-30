# Андрею: фронт ЛР3 (импорт и экспорт)

Код обеих лаб переписан проще, `BackgroundRunner` больше нет. Твоя часть ЛР3 теперь целиком в одном
файле — `lab3/src/main/java/org/example/ui/MainApp.java`. Это кнопки «Импорт» и «Экспорт» и окно
ошибки чтения файла с кодом.

## Что сделать

1. Обнови репозиторий: в папке `java-labs` выполни
   ```
   git checkout main
   git pull
   ```
2. Создай ветку:
   ```
   git checkout -b lab3-import-export-v2
   ```
3. Распакуй этот архив в папку `java-labs` с заменой файлов. Заменится только
   `lab3/src/main/java/org/example/ui/MainApp.java`, `ANDREY.md` в репозиторий не клади.
4. Открой папку `java-labs/lab3` в VS Code, Run and Debug, «Окно». Проверь:
   - «Сгенерировать» 1000, потом «Экспорт» в `contacts.json` и в `contacts.ctb`;
   - «Очистить», потом «Импорт» каждого файла: контакты вернулись;
   - «Импорт» любого испорченного файла из `src/test/resources/fixtures` (например `truncated.ctb`):
     окно с кодом ошибки, данные в таблице не изменились.
5. Закоммить и отправь:
   ```
   git add lab3/src/main/java/org/example/ui/MainApp.java
   git commit -m "ЛР3: импорт и экспорт JSON и бинарного формата"
   git push -u origin lab3-import-export-v2
   ```
6. На GitHub создай Pull Request в `main`. Сам не сливай, Саша посмотрит и сольёт.

## Что в файле твоё (разобраться к защите)

| Метод | Что делает |
|---|---|
| `createButtonBar` | добавлены кнопки «Импорт» и «Экспорт» |
| `importFile` | `FileChooser` → `ImportTask` в фоне → новое хранилище подменяет старое, только если весь файл прочитан |
| `exportFile` | проверка, что справочник не пуст → `FileChooser` → `ExportTask` в фоне |
| `formatChooser` | фильтры `*.json` и `*.ctb` |
| `runTask`, `setOnFailed` | если упала `SerializationException`, показывается её код (`BAD_FORMAT`, `TRUNCATED`…) и текст |

Формат выбирается по расширению файла в `Serializer.forFile`. Работа с файлом идёт в `ImportTask` и
`ExportTask`, а не в окне, чтобы окно не зависало и логика не лежала в GUI.

Возможные вопросы:
- **Почему импорт в новое хранилище?** Если файл битый, старые данные не теряются.
- **Где выполняется чтение файла?** В фоновом потоке, `Task.call()`. Окно обновляется в
  `setOnSucceeded`, это уже поток JavaFX.
- **Как окно узнаёт код ошибки?** `task.getException()` — это та самая `SerializationException`,
  у неё есть `getError()`.
