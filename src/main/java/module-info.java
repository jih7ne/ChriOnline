module com.chrionline.chrionline {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    requires java.sql;
    requires org.slf4j;
    requires com.google.gson;
    requires mysql.connector.j;

    exports com.chrionline.chrionline.client;
    exports com.chrionline.chrionline.client.controllers;
    exports com.chrionline.chrionline.server.controllers;
    exports com.chrionline.chrionline.core.enums;

    opens com.chrionline.chrionline to javafx.fxml, com.google.gson;
    opens com.chrionline.chrionline.client to javafx.fxml;
    opens com.chrionline.chrionline.client.controllers to javafx.fxml;
    opens com.chrionline.chrionline.server.controllers to javafx.fxml;

    opens com.chrionline.chrionline.network.protocol to com.google.gson;
    exports com.chrionline.chrionline.network.enums;
    opens com.chrionline.chrionline.network.enums to com.google.gson;
    opens com.chrionline.chrionline.server.data.models to com.google.gson;
    opens com.chrionline.chrionline.server.data.dto to com.google.gson;

}