module org.example {
    requires javafx.controls;
    requires com.fasterxml.jackson.databind;

    exports org.example;
    exports org.example.model;
    exports org.example.csv;
    exports org.example.store;
    exports org.example.generator;
    exports org.example.io;
    exports org.example.io.dto;
    exports org.example.tasks;
    exports org.example.ui;
    exports org.example.demo;

    opens org.example.io.dto to com.fasterxml.jackson.databind;
}
