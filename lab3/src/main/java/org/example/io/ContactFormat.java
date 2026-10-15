package org.example.io;

import org.example.model.Contact;

import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Supplier;

public enum ContactFormat {
    JSON("JSON", "json", JsonContactSerializer::new),
    BINARY("Бинарный", "ctb", BinaryContactSerializer::new);

    private final String title;
    private final String extension;
    private final Supplier<Serializer<Contact>> factory;

    ContactFormat(String title, String extension, Supplier<Serializer<Contact>> factory) {
        this.title = title;
        this.extension = extension;
        this.factory = factory;
    }

    public String title() {
        return title;
    }

    public String extension() {
        return extension;
    }

    public String pattern() {
        return "*." + extension;
    }

    public Serializer<Contact> serializer() {
        return factory.get();
    }

    public static ContactFormat of(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        for (ContactFormat format : values()) {
            if (name.endsWith("." + format.extension)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Неизвестное расширение файла: " + file.getFileName());
    }
}
