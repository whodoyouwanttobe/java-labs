package org.example.csv;

public class CsvParseException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int lineNumber;
    private final CsvErrorCode errorCode;

    public CsvParseException(String message, int lineNumber, CsvErrorCode errorCode) {
        super(message);
        this.lineNumber = lineNumber;
        this.errorCode = errorCode;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public CsvErrorCode getErrorCode() {
        return errorCode;
    }
}
