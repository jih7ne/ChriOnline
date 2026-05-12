module com.chrionline.security {
    requires org.slf4j;
    requires com.chrionline.sharedmodule;
    exports com.chrionline.security.core;
    exports com.chrionline.security.handshake;
    exports com.chrionline.security.utils;
}