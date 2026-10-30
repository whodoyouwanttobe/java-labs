module org.example {
    requires javafx.controls;
    requires com.fasterxml.jackson.databind;

    exports org.example;
    exports org.example.ui;

    opens org.example.io.dto to com.fasterxml.jackson.databind;
}
