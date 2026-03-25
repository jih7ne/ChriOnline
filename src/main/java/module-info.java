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
    requires org.kordamp.ikonli.feather;
    requires org.kordamp.ikonli.core;

    opens com.chrionline.chrionline.core.enums to com.google.gson;

    exports com.chrionline.chrionline.core.interfaces;
    exports com.chrionline.chrionline.core.enums;
    exports com.chrionline.chrionline.core.theme;

    exports com.chrionline.chrionline.network.protocol;
    exports com.chrionline.chrionline.network.tcp;
    exports com.chrionline.chrionline.network.enums;

    exports com.chrionline.chrionline.client;
    exports com.chrionline.chrionline.client.controllers;
    exports com.chrionline.chrionline.client.ui.components;
    exports com.chrionline.chrionline.client.ui.views;

    exports com.chrionline.chrionline.server.controllers;
    exports com.chrionline.chrionline.server.data.models;

    opens com.chrionline.chrionline to javafx.fxml, com.google.gson;
    opens com.chrionline.chrionline.client to javafx.fxml;
    opens com.chrionline.chrionline.client.controllers to javafx.fxml;
    opens com.chrionline.chrionline.client.ui.components to javafx.fxml;
    opens com.chrionline.chrionline.client.ui.views to javafx.fxml;
    opens com.chrionline.chrionline.server.controllers to javafx.fxml;

    opens com.chrionline.chrionline.network.protocol to com.google.gson;
    opens com.chrionline.chrionline.network.enums to com.google.gson;
    opens com.chrionline.chrionline.server.data.models to com.google.gson;
    opens com.chrionline.chrionline.server.data.dto to com.google.gson;

    exports com.chrionline.chrionline.shared.models;
    opens com.chrionline.chrionline.shared.models to com.google.gson;
}