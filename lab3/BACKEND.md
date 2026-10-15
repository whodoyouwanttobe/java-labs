# Гайд по бэкенду, лабораторная 3

Здесь только то, что появилось в лабе 3. Trie, `TrieStore`, CSV, генератор и потоки не менялись,
они описаны в `../lab2/BACKEND.md`.

## 1. Новые файлы

`src/main/java/org/example/io`:

| Файл | Что делает |
|---|---|
| `Serializer.java` | интерфейс `Serializer<T>`: `write(T[] items, OutputStream)` и `T[] read(InputStream)`, константа `SCHEMA_VERSION = 1` |
| `JsonContactSerializer.java` | JSON через Jackson |
| `BinaryContactSerializer.java` | свой бинарный формат через `DataOutputStream` / `DataInputStream` |
| `ContactMapper.java` | `Contact` ↔ `ContactDto` и вся проверка данных при чтении |
| `dto/ContactDto.java`, `dto/ContactBookDto.java` | record'ы — то, как контакт выглядит в файле |
| `ContactFormat.java` | enum-реестр форматов: название, расширение, фабрика сериализатора, формат по имени файла |
| `FileStorage.java` | `FileStorage<T>`: файл ↔ хранилище, атомарная запись |
| `SerializationException.java`, `SerializationError.java` | ошибка содержимого файла с кодом и местом |

Плюс `tasks/ExportTask.java`, `tasks/ImportTask.java`, `demo/FormatComparison.java`,
тесты в `src/test/java/org/example/io` и фикстуры в `src/test/resources/fixtures`.

Зависимости между слоями:

```
ui → tasks → io.FileStorage → io.Serializer ← JsonContactSerializer, BinaryContactSerializer
                                                       ↓                    ↓
                                                   ContactMapper ← ContactDto
                                                       ↓
                                                     model
```

`io` не знает про JavaFX и про Trie: `FileStorage` работает с любым `EntityStore`.

## 2. Интерфейс `Serializer<T>` (`Serializer.java`, строки 7–14)

```java
public interface Serializer<T> {
    int SCHEMA_VERSION = 1;
    void write(T[] items, OutputStream out) throws IOException;
    T[] read(InputStream in) throws IOException, SerializationException;
}
```

- Работает с потоками, а не с файлами. Поэтому в тестах можно писать в `ByteArrayOutputStream`
  без диска, а `FileStorage` отвечает за файлы.
- Два вида исключений специально разные. `IOException` — проблема ввода-вывода (нет файла, нет прав).
  `SerializationException` — файл прочитан, но внутри не то. UI показывает их по-разному.
- `write` не закрывает поток: его открыл вызывающий, он и закроет (`try-with-resources` в `FileStorage`).
  Для Jackson для этого выключен `AUTO_CLOSE_TARGET`.

Дженерики: интерфейс общий, реализации — `Serializer<Contact>`. `FileStorage<T>` тоже общий и
принимает `Serializer<T>` и `EntityStore<?, T>`: ключ ему не важен, поэтому `?`.

## 3. DTO и маппер

### 3.1. Зачем DTO

`ContactDto` — record с семью строковыми полями: `type, name, phone, email, organization, position,
internalNumber`. `ContactBookDto` — `schemaVersion` и массив `ContactDto[]`.

Почему не сериализовать `Contact` напрямую:

- у `Contact` три подкласса, Jackson'у пришлось бы объяснять полиморфизм аннотациями на модели;
- имена в файле (`internal_number`, `schema_version`) задаются `@JsonProperty` на DTO, модель про
  формат файла ничего не знает;
- модель неизменяемая, без пустых конструкторов и сеттеров — Jackson'у с ней было бы неудобно;
- бинарный формат тоже собирает `ContactDto` из байтов, и проверка получается общей.

`@JsonInclude(NON_NULL)` на `ContactDto` — `null`-поля не пишутся, поэтому у обычного контакта
в JSON нет `position` и `internal_number`.

### 3.2. `ContactMapper.toDto` (строки 19–26)

`instanceof CorporateContact cc` → тип `corporate` и все 7 полей. Иначе `emergency` или `contact`,
последние два поля `null`.

### 3.3. `ContactMapper.fromDto` (строки 28–56)

Единая проверка для обоих форматов. Получает DTO и строку `location` («где мы в файле»).

1. `type` и `name` обязательны → иначе `MISSING_FIELD` (метод `require`, строки 58–64).
2. Имя после `trim()` пустое → `INVALID_VALUE`.
3. `phone`, `email`, `organization` могут отсутствовать → пустая строка.
4. `switch` по типу без учёта регистра. Для `corporate` внутренний номер должен быть из цифр
   (то же правило, что `CorporateContact.validate()` и CSV) → иначе `INVALID_VALUE`
   с местом `...internal_number`.
5. Неизвестный тип → `UNKNOWN_TYPE`.

## 4. JSON (`JsonContactSerializer.java`)

Настройки `ObjectMapper` (строки 24–27):

- `INDENT_OUTPUT` — файл с отступами, его удобно читать человеку;
- `FAIL_ON_UNKNOWN_PROPERTIES` выключен — лишние поля пропускаются (совместимость вперёд);
- `AUTO_CLOSE_TARGET` выключен — см. выше.

### 4.1. Запись (строки 30–37)

Массив контактов → массив DTO через `ContactMapper.toDto` → `new ContactBookDto(1, dtos)` →
`mapper.writeValue(out, ...)`. Порядок полей в файле задан `@JsonPropertyOrder`: сначала версия.

### 4.2. Чтение (строки 39–64)

Читаем в два шага, сначала в дерево `JsonNode`, потом в объекты:

1. Все байты → если пусто или пробелы → `EMPTY_FILE`.
2. `parse` (строки 66–77): `mapper.readTree`. Сломанный синтаксис → `JsonProcessingException` →
   `MALFORMED` с местом «строка N, столбец M» (из `e.getLocation()`).
3. Корень не объект → `MALFORMED`.
4. `checkVersion` (строки 79–93): нет поля → `MISSING_FIELD`, не целое → `INVALID_VALUE`,
   не 1 → `UNSUPPORTED_VERSION`. Версию проверяем до разбора записей: в файле новой версии
   записи могут выглядеть совсем иначе, и честнее сказать «версия не та», чем «нет поля name».
5. `contacts` нет → `MISSING_FIELD`, не массив → `MALFORMED`.
6. Каждую запись `toDto` (строки 95–105): `treeToValue(node, ContactDto.class)`, потом
   `ContactMapper.fromDto` с местом `$.contacts[i]`.

Почему не сразу `mapper.readValue(bytes, ContactBookDto.class)`: тогда при ошибке в 500-й записи
мы бы знали только строку и столбец, а не номер записи, и версию нельзя было бы проверить раньше
остального.

Почему `readAllBytes`: файл всё равно целиком превращается в дерево, а пустой файл так проще
отличить от сломанного. На 10⁵ контактов это около 23 МБ — приемлемо.

## 5. Бинарный формат (`BinaryContactSerializer.java`)

### 5.1. Раскладка

```
CTKB | версия u16 | N i32 | запись × N
запись = тип u8 | name | phone | email | organization | [position | internal_number]
строка = длина i16 (в байтах, -1 = null) | байты UTF-8
```

Константы (строки 18–24): `MAGIC = 0x43544B42` («CTKB»), коды типов 1/2/3, `MAX_STRING_BYTES = 4096`,
`MAX_COUNT = 10 000 000`.

Пример начала файла с 10 000 контактами:

```
43 54 4B 42   сигнатура CTKB
00 01         версия 1
00 00 27 10   10000 записей
03            corporate
00 38         длина имени 56 байт
D0 90 D0 BB ...  «Ал...» в UTF-8
```

### 5.2. Запись (строки 27–45)

`DataOutputStream` поверх `BufferedOutputStream` (иначе каждый `writeByte` шёл бы в файл по одному
байту). Сигнатура, версия, количество, потом записи. Строки пишет `writeString` (строки 123–134):
длина в байтах UTF-8, потом сами байты. Не `writeUTF`, потому что тот пишет «модифицированный UTF-8»
Java, а наш формат должен читаться и не из Java.

### 5.3. Чтение (строки 48–85)

Обёртка `CountingDataInput` (внутренний класс в конце файла) считает, сколько байт прочитано,
чтобы в ошибке было «байт 118».

1. Первый байт подсматривается через `mark/reset`. Его нет → `EMPTY_FILE`.
2. Сигнатура не `CTKB` → `BAD_MAGIC`. Так ловится, например, JSON, переименованный в `.ctb`.
3. Версия не 1 → `UNSUPPORTED_VERSION`.
4. Количество < 0 или > `MAX_COUNT` → `CORRUPTED`. Без этой проверки испорченный байт превратил бы
   количество в 2 миллиарда, и `new Contact[count]` упал бы с `OutOfMemoryError`.
5. Записи читает `readContact` (строки 87–103): тип → строки → `ContactDto` → `ContactMapper.fromDto`.
   Неизвестный код типа → `UNKNOWN_TYPE` (строки 113–121).
6. `readString` (строки 136–149): длина вне 0…4096 → `CORRUPTED`.
7. После последней записи что-то осталось → `CORRUPTED`.
8. Файл кончился раньше времени → `DataInputStream` бросает `EOFException` → ловим и превращаем
   в `TRUNCATED`.

## 6. `ContactFormat` — реестр форматов

```java
JSON("JSON", "json", JsonContactSerializer::new),
BINARY("Бинарный", "ctb", BinaryContactSerializer::new);
```

Фабрика `Supplier<Serializer<Contact>>` — каждый вызов `serializer()` даёт новый объект
(сериализаторы без общего состояния между потоками, так спокойнее). `ContactFormat.of(path)` ищет
формат по расширению без учёта регистра, неизвестное расширение → `IllegalArgumentException`.

UI работает только с `ContactFormat`, а не с классами сериализаторов. Новый формат (например, XML)
добавляется одной строкой здесь и одним классом.

## 7. `FileStorage<T>`

- `save(file, store)` (строки 21–34): `store.toArray()` → запись во временный файл в той же папке →
  `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`. Если программа упадёт посреди записи, старый файл
  останется целым, а не наполовину перезаписанным. Если файловая система не умеет атомарный
  перенос, переносим обычным. `finally` удаляет временный файл, если он остался.
- `read(file)` (строки 36–40): открыть поток → `serializer.read`.
- `loadInto(file, target)` (строки 42–51): прочитать всё, затем `tryAdd` по одной. Повтор ключа →
  `DUPLICATE_KEY`. Файл сначала читается целиком, поэтому битый файл не добавит ни одной записи.
  При дубликате часть записей уже в `target`, поэтому импорт всегда идёт в новое хранилище.

## 8. Задачи

`ExportTask` и `ImportTask` — обычные `Task<Integer>`, как `SaveCsvTask` / `LoadCsvTask`. Сообщение
для строки состояния через `updateMessage`, прогресс `-1` (неопределённый). Исключения из `call()`
JavaFX кладёт в `task.getException()`, и `BackgroundRunner` показывает их в `setOnFailed`.

## 9. Тесты (29 новых, всего 73)

| Класс | Что проверяет |
|---|---|
| `RoundTripTest` (6) | запись → чтение возвращает те же объекты (сравнение через `equals` из лабы 2): кавычки, `;`, `\`, эмодзи, пустые поля; пустой массив; подклассы сохраняются; 2000 контактов через `FileStorage` в обоих форматах; бинарный меньше JSON |
| `FixtureTest` (20) | `valid.json` и `valid.ctb` дают одинаковые 4 контакта; 18 испорченных случаев — каждый со своим кодом, у части проверено и место ошибки |
| `FileStorageTest` (3) | повторное сохранение перезаписывает и не оставляет временных файлов; формат по расширению; отсутствующий файл — `IOException`, а не ошибка формата |

Фикстуры (`src/test/resources/fixtures`) — настоящие файлы, а не строки в коде теста. Их можно
открыть, показать преподавателю, подсунуть в окно через «Импорт». Бинарные фикстуры получены из
`valid.json` и потом испорчены по байтам: `truncated.ctb` без последних 7 байт, в `bad-magic.ctb`
вместо сигнатуры `{\n  `, в `future-version.ctb` версия 2, в `unknown-type.ctb` код типа 9,
в `bad-count.ctb` отрицательное количество, в `trailing-bytes.ctb` три лишних нуля в конце.

## 10. Как пользоваться из кода

```java
TrieStore<Contact> store = ContactStores.create();
new ContactGenerator(1).generateInto(store, 100);

FileStorage<Contact> storage = new FileStorage<>(ContactFormat.BINARY.serializer());
storage.save(Path.of("book.ctb"), store);

TrieStore<Contact> copy = ContactStores.create();
try {
    storage.loadInto(Path.of("book.ctb"), copy);
} catch (SerializationException e) {
    System.out.println(e.getError() + " в " + e.getLocation() + ": " + e.getMessage());
}
```

## 11. Вопросы на защите

- **Чем бинарный формат лучше и хуже JSON?** Меньше и быстрее (нет имён полей, нет разбора текста).
  Хуже: человек его не прочитает, нужна документация формата, любое изменение — новая версия.
- **Зачем сигнатура?** Чтобы чужой файл сразу отбросить с понятной ошибкой, а не читать мусор.
- **Зачем версия?** Старая программа увидит файл от новой и честно скажет «версия 2 не поддерживается».
  В лабе 4 по версии будут работать миграции.
- **Почему длина строки в байтах, а не в символах?** В UTF-8 русская буква — 2 байта, эмодзи — 4.
  Читать нужно ровно столько байт, сколько записано.
- **Почему `ContactMapper` общий?** Правила корректности контакта не должны зависеть от формата файла.
- **Что если упадёт посреди сохранения?** Пишем во временный файл и атомарно переименовываем.
- **Почему не `Serializable` из Java?** Формат привязан к классам Java и их версиям, файл не прочитать
  другой программой, а десериализация чужих данных — известная дыра в безопасности.
