module com.chrionline.sharedmodule {
    requires javafx.controls;
    requires org.slf4j;
    requires java.sql;
    requires com.google.gson;
    exports com.chrionline.core.config;
    exports com.chrionline.core.constants;
    exports com.chrionline.core.interfaces;
    exports com.chrionline.shared.models;
    exports com.chrionline.core.utils;
    exports com.chrionline.core.theme;
    exports com.chrionline.core.enums;
    exports com.chrionline.core.exceptions;
    exports com.chrionline.core.security;
    exports com.chrionline.core.network.protocol;
    exports com.chrionline.core.annotations;

    opens com.chrionline.shared.models to com.google.gson;
    opens com.chrionline.core.network.protocol to com.google.gson;

}