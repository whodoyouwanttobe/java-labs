# Лабораторная работа 4. Версионирование формата и миграции данных

Вариант 4: справочник контактов, префиксное дерево (Trie) по имени.

ЛР4 продолжает ЛР3: в сущность `Contact` добавлены поля `updatedAt` (дата изменения, появилась в версии 2)
и `note` (заметка, появилась в версии 3). Поддерживается поэтапная миграция `v1 -> v2 -> v3` при чтении
и обратная совместимость (экспорт в старые версии `v1` и `v2`) в обоих форматах — JSON и `.ctb`.

## Запуск

Папку `lab4` открыть в VS Code с расширением Extension Pack for Java. Нужен JDK 21 или новее.

- окно: Run and Debug, конфигурация «Окно» (или `mvn javafx:run`);
- сравнение форматов: конфигурация «FormatComparison»;
- тесты: панель Testing, Run Tests (или `mvn test`).

## Версии схемы (`SCHEMA_VERSION = 3`)

| Версия | Поля контакта | Что нового |
|---|---|---|
| 1 | `type`, `name`, `phone`, `email`, `organization` (+ `position`, `internalNumber` у `Corporate`) | исходный формат из ЛР3 |
| 2 | поля v1 + `updatedAt` | дата изменения (по умолчанию `"1970-01-01"`, не `null`) |
| 3 | поля v2 + `note` | заметка (по умолчанию `""`, не `null`) |

## Что добавлено

| Где | Что |
|---|---|
| `model/Contact` | поля `updatedAt`, `note`, значения по умолчанию, кастомные `writeObject`/`readObject` с версией |
| `io/ContactMigrator` | шаги `migrateV1ToV2`, `migrateV2ToV3`, цепочка `migrate(ContactFileDto)` и подготовка DTO под нужную версию |
| `io/Serializer` | `SCHEMA_VERSION = 3`, `readVersion(file)`, `save(contacts, file, version)` |
| `io/JsonSerializer`, `BinarySerializer` | чтение файлов версий 1..3 с миграцией и сохранение в выбранную версию 1..3 |
| `ui/MigrationDialog` | диалог «Файл версии X. Мигрировать до 3?» перед импортом старого файла |
| `ui/ExportVersionDialog` | выбор версии (3, 2, 1) при экспорте с предупреждением о потере новых полей |
| `ui/StatusBarView`, `ui/TaskRunner` | нижняя панель состояния и запуск фоновых задач |
| `io/MigrationTest` | тесты шагов миграции `v1 -> v2 -> v3`, дефолтных значений, downgrade в `v1`/`v2` и ошибок |

