module com.chrionline.clientmodule {
    requires javafx.fxml;
    requires org.slf4j;

    requires org.controlsfx.controls;
    requires com.chrionline.sharedmodule;
    requires com.chrionline.network;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;
    requires org.kordamp.ikonli.core;
    requires com.google.gson;
    requires javafx.web;
    requires javafx.graphics;
    requires jdk.jsobject;
    requires jdk.httpserver;

    opens com.chrionline.clientmodule to javafx.fxml;
    exports com.chrionline.clientmodule.client;
    opens com.chrionline.clientmodule.utils to javafx.fxml;
}