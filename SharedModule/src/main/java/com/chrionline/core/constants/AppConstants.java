package com.chrionline.core.constants;

public final class AppConstants {

    private AppConstants() {

    }

    /* =========================
       APPLICATION
     ========================= */

    public static final String APP_NAME = "ChriOnline";
    public static final String APP_VERSION = "1.0.0";
    public static final int LOW_STOCK_PRODUCTS_THRESHOLD = 10;
    public static final int HIGH_STOCK_PRODUCTS_THRESHOLD = 100;
    public static final int HEAD_LIMIT = 10;


    /* =========================
       NETWORK
     ========================= */

    public static final String SERVER_HOST = "localhost";

    public static final int SERVER_PORT = 5000;
    public static final int UDP_PORT = 5009;

    public static final int SOCKET_TIMEOUT_MS = 30000;
    public static final String BUFFER_CHARSET = "UTF-8";

    public static final int BUFFER_SIZE = 8192; //8KB
    public static final int MAX_BUFFER_SIZE = 65507;
    public static final int RECONNECT_DELAY_MS = 2000;



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
    // ── reCAPTCHA ──────────────────────────────────────
    public static final String RECAPTCHA_SITE_KEY   = "6LdVSKosAAAAANyQFpO5gKauqVkovhHfCix-fdf1";
    public static final String RECAPTCHA_SECRET_KEY = "6LdVSKosAAAAAOvnDSHVwx_gihG0vDRzRG6eSVl4";
    public static final String RECAPTCHA_VERIFY_URL =
            "https://www.google.com/recaptcha/api/siteverify";

}
