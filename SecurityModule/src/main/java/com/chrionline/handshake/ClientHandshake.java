package com.chrionline.handshake;

import com.chrionline.config.EncryptionConfig;
import com.chrionline.core.SessionCipher;
import com.chrionline.utils.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.*;

/**
 * Runs the client side of the handshake.
 */
public class ClientHandshake {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandshake.class);

    public static SessionCipher perform(BufferedReader in, PrintWriter out) throws Exception {

        // Step 1 — send HELLO
        out.println(HandshakeProtocol.buildHello());
        logger.debug("[Handshake] Client → HELLO");

        // Step 2 — receive server RSA public key
        String keyMsg = in.readLine();
        if (!HandshakeProtocol.isKeyMsg(keyMsg)) {
            throw new IOException("Expected server RSA key, got: " + keyMsg);
        }
        String encodedPub = HandshakeProtocol.extractKeyPayload(keyMsg);
        java.security.PublicKey serverPub = CryptoUtil.decodePublicKey(encodedPub);
        logger.debug("[Handshake] Received server RSA public key");

        // Step 3 — generate AES key, encrypt with server's RSA key, send
        SecretKey aesKey = CryptoUtil.generateAESKey();
        SessionCipher cipher = new SessionCipher();
        cipher.initFromGeneratedKey(aesKey);

        String encryptedKey = CryptoUtil.rsaEncrypt(cipher.getRawKeyBytes(), serverPub);
        out.println(HandshakeProtocol.buildKeyMessage(encryptedKey));
        logger.debug("[Handshake] Client → encrypted AES session key");

        // Step 4 — receive encrypted ACK, verify it decrypts correctly
        String ackEnc = in.readLine();
        String ack    = cipher.decrypt(ackEnc);
        if (!HandshakeProtocol.isAck(ack)) {
            throw new IOException("Handshake failed: bad ACK from server");
        }

        // Step 5 — send READY
        out.println(cipher.encrypt(EncryptionConfig.HANDSHAKE_READY));
        logger.info("[Handshake] Secure channel established with server");

        return cipher;
    }
}
