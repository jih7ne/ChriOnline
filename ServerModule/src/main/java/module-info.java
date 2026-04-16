module com.chrionline.servermodule {
    requires com.chrionline.sharedmodule;
    requires com.google.gson;
    requires org.slf4j;
    requires com.chrionline.network;
    requires java.sql;

    requires com.google.zxing;
    requires com.google.zxing.javase;

    exports com.chrionline.server.controllers to com.chrionline.network;
    opens com.chrionline.server.data.dto to com.google.gson;
    opens com.chrionline.server.controllers to com.google.gson;
    opens com.chrionline.server.services to com.google.gson;
    opens com.chrionline.server.repositories to com.google.gson;


}