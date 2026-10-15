package org.example.io;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

final class Fixtures {

    private Fixtures() {
    }

    static Path path(String name) {
        URL url = Fixtures.class.getResource("/fixtures/" + name);
        if (url == null) {
            throw new IllegalStateException("Нет фикстуры " + name);
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
