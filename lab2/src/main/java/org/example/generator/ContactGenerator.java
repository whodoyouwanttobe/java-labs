package org.example.generator;

import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.store.EntityStore;
import org.example.tasks.ProgressListener;

import java.util.Random;
import java.util.function.BooleanSupplier;

public class ContactGenerator {

    private static final String[] LAST_NAMES = {
            "Иванов", "Петров", "Сидоров", "Смирнов", "Кузнецов", "Попов", "Васильев", "Соколов",
            "Михайлов", "Новиков", "Фёдоров", "Морозов", "Волков", "Алексеев", "Лебедев", "Семёнов",
            "Егоров", "Павлов", "Козлов", "Степанов", "Николаев", "Орлов", "Андреев", "Макаров",
            "Никитин", "Захаров", "Зайцев", "Соловьёв", "Борисов", "Яковлев"
    };
    private static final String[] FIRST_NAMES = {
            "Александр", "Алексей", "Андрей", "Антон", "Артём", "Борис", "Вадим", "Виктор",
            "Владимир", "Глеб", "Дмитрий", "Евгений", "Егор", "Иван", "Илья", "Кирилл",
            "Константин", "Максим", "Михаил", "Никита", "Николай", "Олег", "Павел", "Пётр",
            "Роман", "Сергей", "Степан", "Тимофей", "Фёдор", "Юрий"
    };
    private static final String[] PATRONYMICS = {
            "Александрович", "Алексеевич", "Андреевич", "Викторович", "Владимирович", "Дмитриевич",
            "Евгеньевич", "Иванович", "Игоревич", "Максимович", "Михайлович", "Николаевич",
            "Олегович", "Павлович", "Петрович", "Сергеевич", "Юрьевич"
    };
    private static final String[] ORGANIZATIONS = {
            "ООО Вектор", "АО Сибирь-Телеком", "НГТУ", "ИП Петров", "ООО Альфа", "ПАО Энергия",
            "ООО СтройМаш", "ЗАО Интегра", "ООО Горизонт", "АО НовосибирскЭнерго"
    };
    private static final String[] POSITIONS = {
            "Менеджер", "Инженер", "Бухгалтер", "Разработчик", "Аналитик", "Директор",
            "Юрист", "Секретарь", "Тестировщик", "Администратор"
    };
    private static final String[] DOMAINS = {"mail.ru", "yandex.ru", "gmail.com", "corp.local"};

    private static final double CORPORATE_SHARE = 0.4;
    private static final int PROGRESS_STEP = 500;

    private final Random random;
    private int sequence;

    public ContactGenerator() {
        this(new Random());
    }

    public ContactGenerator(long seed) {
        this(new Random(seed));
    }

    private ContactGenerator(Random random) {
        this.random = random;
    }

    public Contact next() {
        sequence++;
        String name = pick(LAST_NAMES) + " " + pick(FIRST_NAMES) + " " + pick(PATRONYMICS);
        String phone = "+79" + String.format("%09d", random.nextInt(1_000_000_000));
        String email = "user" + sequence + "@" + pick(DOMAINS);
        String org = pick(ORGANIZATIONS);
        if (random.nextDouble() < CORPORATE_SHARE) {
            String internal = String.valueOf(100 + random.nextInt(900));
            return new CorporateContact(name, phone, email, org, pick(POSITIONS), internal);
        }
        return new Contact(name, phone, email, org);
    }

    public int generateInto(EntityStore<String, Contact> store, int count,
                            ProgressListener progress, BooleanSupplier cancelled) {
        if (count < 0) {
            throw new IllegalArgumentException("Количество не может быть отрицательным: " + count);
        }
        int added = 0;
        while (added < count && !cancelled.getAsBoolean()) {
            Contact contact = next();
            String baseName = contact.getName();
            int suffix = 2;
            while (!store.tryAdd(contact)) {
                contact = contact.withName(baseName + " " + suffix++);
            }
            added++;
            if (added % PROGRESS_STEP == 0) {
                progress.onProgress(added, count);
            }
        }
        progress.onProgress(added, count);
        return added;
    }

    public int generateInto(EntityStore<String, Contact> store, int count) {
        return generateInto(store, count, ProgressListener.NONE, () -> false);
    }

    private String pick(String[] values) {
        return values[random.nextInt(values.length)];
    }
}
