module com.example.demo {
    requires javafx.controls;
    requires javafx.media;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;
    requires javafx.swing;
    requires java.desktop;

    opens com.example.demo to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.example.demo;
}