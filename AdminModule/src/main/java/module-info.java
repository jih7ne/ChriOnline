module com.chrionline.adminmodule {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.chrionline.sharedmodule;
    requires com.chrionline.network;
    requires com.google.gson;
    requires org.kordamp.ikonli.feather;
    requires org.bouncycastle.provider;
    requires org.slf4j;
    requires org.bouncycastle.pkix;

    opens com.chrionline.adminmodule to javafx.fxml;
    exports com.chrionline.adminmodule.admin;
    opens com.chrionline.adminmodule.admin to javafx.fxml;
}