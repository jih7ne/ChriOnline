package com.chrionline.security.handshake;

import com.chrionline.security.config.EncryptionConfig;
import com.chrionline.security.core.KeyManager;
import com.chrionline.security.core.SessionCipher;
import com.chrionline.security.utils.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Runs the server side of the handshake on an already-open socket.
 * Returns a ready {@link SessionCipher} or throws.
 */
public class ServerHandshake {

    private static final Logger logger = LoggerFactory.getLogger(ServerHandshake.class);

    /**
     * @param in       the socket's BufferedReader (still in plaintext mode)
     * @param out      the socket's PrintWriter    (still in plaintext mode)
     * @param clientId just for logging
     */
    public static SessionCipher perform(BufferedReader in, PrintWriter out, String clientId)
            throws Exception {

        // Step 1 — wait for HELLO
        String hello = in.readLine();
        if (!HandshakeProtocol.isHello(hello)) {
            throw new IOException("Expected HELLO from " + clientId + ", got: " + hello);
        }
        logger.debug("[Handshake] {} → HELLO", clientId);

        // Step 2 — send our RSA public key
        String encodedPub = CryptoUtil.encodePublicKey(KeyManager.getInstance().getPublicKey());
        out.println(HandshakeProtocol.buildKeyMessage(encodedPub));
        logger.debug("[Handshake] Server → {} : RSA public key", clientId);

        // Step 3 — receive RSA-encrypted AES session key
        String keyMsg = in.readLine();
        if (!HandshakeProtocol.isKeyMsg(keyMsg)) {
            throw new IOException("Expected KEY offer from " + clientId);
        }
        String encryptedAESKey = HandshakeProtocol.extractKeyPayload(keyMsg);
        byte[] aesKeyBytes = CryptoUtil.rsaDecrypt(encryptedAESKey,
                KeyManager.getInstance().getPrivateKey());

        SessionCipher cipher = new SessionCipher();
        cipher.initFromRawBytes(aesKeyBytes);
        logger.debug("[Handshake] {} : AES session key established ({} bytes)", clientId, aesKeyBytes.length);

        // Step 4 — send ACK (first encrypted message)
        out.println(cipher.encrypt(EncryptionConfig.HANDSHAKE_ACK));

        // Step 5 — wait for READY
        String readyEnc = in.readLine();
        String ready    = cipher.decrypt(readyEnc);
        if (!HandshakeProtocol.isReady(ready)) {
            throw new IOException("Expected READY from " + clientId);
        }

        logger.info("[Handshake] Secure channel established with {}", clientId);
        return cipher;
    }
}
