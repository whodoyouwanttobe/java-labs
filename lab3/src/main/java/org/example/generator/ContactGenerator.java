package org.example.generator;

import org.example.model.Contact;
import org.example.model.CorporateContact;
import org.example.store.Store;

import java.util.Random;

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

    private final Random random;
    private int counter;

    public ContactGenerator() {
        random = new Random();
    }

    public ContactGenerator(long seed) {
        random = new Random(seed);
    }

    public Contact next(Store<Contact> store) {
        counter++;
        String name = uniqueName(store);
        String phone = "+79" + String.format("%09d", random.nextInt(1_000_000_000));
        String email = "user" + counter + "@" + pick(DOMAINS);
        String organization = pick(ORGANIZATIONS);
        if (random.nextDouble() < CORPORATE_SHARE) {
            String internalNumber = String.valueOf(100 + random.nextInt(900));
            return new CorporateContact(name, phone, email, organization, pick(POSITIONS), internalNumber);
        }
        return new Contact(name, phone, email, organization);
    }

    public void fill(Store<Contact> store, int count) {
        for (int i = 0; i < count; i++) {
            store.add(next(store));
        }
    }

    private String uniqueName(Store<Contact> store) {
        String base = pick(LAST_NAMES) + " " + pick(FIRST_NAMES) + " " + pick(PATRONYMICS);
        String name = base;
        int number = 2;
        while (store.contains(name)) {
            name = base + " " + number;
            number++;
        }
        return name;
    }

    private String pick(String[] values) {
        return values[random.nextInt(values.length)];
    }
}
