module com.chrionline.network {
    requires com.chrionline.sharedmodule;
    requires com.google.gson;
    requires org.slf4j;
    requires com.chrionline.security;
    exports com.chrionline.network.protocol;
    exports com.chrionline.network.tcp;
    exports com.chrionline.network.udp;
    exports com.chrionline.network.enums;

    opens com.chrionline.network.protocol to com.google.gson;

}