package org.example.csv;

import java.util.ArrayList;
import java.util.List;

final class CsvFormat {

    static final char SEPARATOR = ';';
    private static final char QUOTE = '"';

    private CsvFormat() {
    }

    static String[] split(String line, int lineNumber) throws CsvParseException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (quoted) {
                if (c != QUOTE) {
                    field.append(c);
                } else if (i + 1 < line.length() && line.charAt(i + 1) == QUOTE) {
                    field.append(QUOTE);
                    i++;
                } else {
                    quoted = false;
                    if (i + 1 < line.length() && line.charAt(i + 1) != SEPARATOR) {
                        throw new CsvParseException("После закрывающей кавычки ожидался '" + SEPARATOR + "'",
                                lineNumber, CsvErrorCode.BAD_QUOTES);
                    }
                }
            } else if (c == SEPARATOR) {
                fields.add(field.toString());
                field.setLength(0);
            } else if (c == QUOTE && field.isEmpty()) {
                quoted = true;
            } else {
                field.append(c);
            }
            i++;
        }
        if (quoted) {
            throw new CsvParseException("Кавычка не закрыта", lineNumber, CsvErrorCode.BAD_QUOTES);
        }
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        String flat = value.replace('\r', ' ').replace('\n', ' ');
        boolean needsQuotes = flat.indexOf(SEPARATOR) >= 0 || flat.indexOf(QUOTE) >= 0;
        if (!needsQuotes) {
            return flat;
        }
        return QUOTE + flat.replace("\"", "\"\"") + QUOTE;
    }
}
