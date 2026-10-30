# Лабораторная работа 3. Сериализация, десериализация и тестирование

Вариант 4: справочник контактов, префиксное дерево (Trie) по имени.

ЛР3 — это ЛР2 плюс сохранение справочника в JSON и в свой бинарный формат. Дерево, хранилище,
CSV и окно описаны в `../lab2/README.md`.

## Запуск

Папку `lab3` открыть в VS Code с расширением Extension Pack for Java. Нужен JDK 21 или новее.

- окно: Run and Debug, конфигурация «Окно» (или `mvn javafx:run`);
- сравнение форматов для отчёта: конфигурация «FormatComparison»;
- тесты: панель Testing, Run Tests (или `mvn test`).

## Что добавлено

| Где | Что |
|---|---|
| `io/Serializer` | общий интерфейс: `getFormat()`, `save(...)`, `load(...)`, константа `SCHEMA_VERSION` |
| `io/JsonSerializer` | JSON через Jackson |
| `io/BinarySerializer` | свой бинарный формат `.ctb` через `DataOutputStream` |
| `io/ContactMapper` | перевод `Contact` в `ContactDto` и обратно, проверка полей |
| `io/dto` | `ContactDto`, `ContactBookDto` — то, что лежит в файле |
| `io/SerializationException`, `SerializationError` | ошибка чтения файла с кодом |
| `tasks/ExportTask`, `ImportTask` | экспорт и импорт в фоне |
| `ui/MainApp` | кнопки «Импорт» и «Экспорт», `FileChooser` с фильтрами `.json` и `.ctb` |
| `demo/FormatComparison` | размер файла и среднее время записи и чтения за 5 прогонов на 10000 контактах |

Jackson подключён в `pom.xml`. В `module-info.java` добавлены `requires com.fasterxml.jackson.databind`
и `opens org.example.io.dto`, иначе Jackson не сможет создать record'ы из пакета `dto`.

Формат выбирается по расширению файла (`Serializer.forFile`). Импорт идёт в новое хранилище,
и старое заменяется только если весь файл прочитан без ошибок.

## JSON

```json
{
  "schema_version" : 1,
  "contacts" : [ {
    "type" : "corporate",
    "name" : "Игорь Лесной",
    "phone" : "+79130000003",
    "email" : "les@corp.local",
    "organization" : "ООО Альфа",
    "position" : "Инженер",
    "internal_number" : "204"
  } ]
}
```

`type` — `contact`, `emergency` или `corporate`, по нему создаётся нужный класс. `position` и
`internal_number` пишутся только у корпоративных контактов.

## Бинарный формат `.ctb`

| Что | Размер |
|---|---|
| сигнатура `0x43544B42` (буквы CTKB) | 4 байта |
| версия схемы | 4 байта |
| число контактов | 4 байта |
| контакты | по очереди |

Контакт — строки type, name, phone, email, organization, у корпоративного ещё position и
internal_number. Строки пишутся `writeUTF`: 2 байта длины и текст.

## Ошибки

При чтении файла проверяется:

| Код | Когда |
|---|---|
| `EMPTY_FILE` | файл пустой |
| `BAD_FORMAT` | битый JSON, чужая сигнатура, неверное число записей, лишние байты в конце |
| `BAD_VERSION` | версия схемы не 1 |
| `MISSING_FIELD` | нет `schema_version`, `contacts`, типа или имени |
| `BAD_VALUE` | во внутреннем номере не цифры |
| `UNKNOWN_TYPE` | неизвестный тип контакта |
| `DUPLICATE_NAME` | в файле два контакта с одинаковым именем |
| `TRUNCATED` | бинарный файл обрывается раньше времени |

Окно показывает код и текст ошибки.

## Тесты

- `SerializerTest` — сохранение и загрузка дают те же контакты (оба формата), выбор формата по
  расширению;
- `FixtureTest` — готовые файлы из `src/test/resources/fixtures`: правильные и испорченные, на каждый
  испорченный файл свой ожидаемый код ошибки;
- тесты из ЛР2 (дерево, хранилище, CSV, генератор).
