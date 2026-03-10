package com.chrionline.chrionline.core.constants;

public final class AppConstants {

    private AppConstants() {

    }

    /* =========================
       APPLICATION
     ========================= */

    public static final String APP_NAME = "ChriOnline";
    public static final String APP_VERSION = "1.0.0";


    /* =========================
       NETWORK
     ========================= */

    public static final String SERVER_HOST = "localhost";

    public static final int SERVER_PORT = 5000;

    public static final int SOCKET_TIMEOUT_MS = 30000;

    public static final int BUFFER_SIZE = 4096;


    /* =========================
       THREADING
     ========================= */

    private static final int CORE_POOL_SIZE = 10;      // Minimum threads to keep alive
    public static final int MAX_CLIENT_THREADS = 50;    // Maximum threads
    private static final int KEEP_ALIVE_TIME = 60;     // Seconds to keep idle threads
    private static final int QUEUE_CAPACITY = 100;     // Pending tasks queue size



    /* =========================
       SECURITY
     ========================= */

    public static final String HASH_ALGORITHM = "SHA-256";


    /* =========================
       DATABASE
     ========================= */

    public static final int DB_CONNECTION_TIMEOUT = 10;

}
