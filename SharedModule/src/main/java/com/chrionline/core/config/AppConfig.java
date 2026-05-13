package com.chrionline.core.config;

public final class AppConfig {
    public static final String SIGNATURE_ALGO = "SHA256withRSA";
    public static final String KEY_GEN_ALGO        = "RSA";
    public static final int    KEY_GEN_SIZE_BITS     = 2048;
    public static final long    CHALLENGE_TTL_MS     = 5 * 60 * 1000L;
}
