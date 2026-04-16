package com.chrionline.security.handshake;

import com.chrionline.security.config.EncryptionConfig;

/**
 * Wire format for the 3-step handshake.
 * All messages are plain text — encryption begins only AFTER the handshake.
 *
 *  Step 1 — Client → Server:  "CHRI_HELLO"
 *  Step 2 — Server → Client:  "CHRI_KEY:<base64-RSA-public-key>"
 *  Step 3 — Client → Server:  "CHRI_KEY:<base64-RSA-encrypted-AES-key>"
 *  Step 4 — Server → Client:  "CHRI_ACK" (AES-encrypted)
 *  Step 5 — Client → Server:  "CHRI_READY" (AES-encrypted)
 */
public final class HandshakeProtocol {
    private HandshakeProtocol() {}

    public static String buildHello() {
        return EncryptionConfig.HANDSHAKE_HELLO;
    }

    public static String buildKeyMessage(String base64Payload) {
        return EncryptionConfig.HANDSHAKE_KEY_OFFER + ":" + base64Payload;
    }

    public static String extractKeyPayload(String message) {
        if (!message.startsWith(EncryptionConfig.HANDSHAKE_KEY_OFFER + ":")) {
            throw new IllegalArgumentException("Not a KEY message: " + message);
        }
        return message.substring(EncryptionConfig.HANDSHAKE_KEY_OFFER.length() + 1);
    }

    public static boolean isHello(String msg)  { return EncryptionConfig.HANDSHAKE_HELLO.equals(msg);  }
    public static boolean isKeyMsg(String msg) { return msg != null && msg.startsWith(EncryptionConfig.HANDSHAKE_KEY_OFFER + ":"); }
    public static boolean isAck(String msg)    { return EncryptionConfig.HANDSHAKE_ACK.equals(msg);    }
    public static boolean isReady(String msg)  { return EncryptionConfig.HANDSHAKE_READY.equals(msg);  }
}
