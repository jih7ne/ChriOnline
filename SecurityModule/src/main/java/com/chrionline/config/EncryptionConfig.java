package com.chrionline.config;

public final class EncryptionConfig {
    private EncryptionConfig() {}

    // AES-256-GCM
    public static final String AES_ALGORITHM       = "AES";
    public static final String AES_TRANSFORMATION  = "AES/GCM/NoPadding";
    public static final int    AES_KEY_SIZE_BITS    = 256;
    public static final int    GCM_IV_LENGTH_BYTES  = 12;   // 96-bit IV — NIST recommended
    public static final int    GCM_TAG_LENGTH_BITS  = 128;  // authentication tag

    // RSA-2048 for key exchange
    public static final String RSA_ALGORITHM        = "RSA";
    public static final String RSA_TRANSFORMATION   = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    public static final int    RSA_KEY_SIZE_BITS     = 2048;

    // Handshake wire tokens
    public static final String HANDSHAKE_HELLO      = "CHRI_HELLO";
    public static final String HANDSHAKE_KEY_OFFER  = "CHRI_KEY";
    public static final String HANDSHAKE_ACK        = "CHRI_ACK";
    public static final String HANDSHAKE_READY      = "CHRI_READY";
}
