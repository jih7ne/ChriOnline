package com.chrionline.security.core;

import com.chrionline.security.config.EncryptionConfig;
import com.chrionline.security.utils.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

/**
 * Manages the server's long-lived RSA key pair.
 * Keys are persisted to disk so the server doesn't regenerate them on every restart.
 *
 * Usage (in ServerConfig or ServerApplication):
 *   KeyManager.getInstance().init();
 *   PublicKey pub = KeyManager.getInstance().getPublicKey();
 */
public class KeyManager {

    private static final Logger logger = LoggerFactory.getLogger(KeyManager.class);
    private static final KeyManager INSTANCE = new KeyManager();

    private static final Path KEY_DIR     = Paths.get("keys");
    private static final Path PRIVATE_KEY = KEY_DIR.resolve("server_private.key");
    private static final Path PUBLIC_KEY  = KEY_DIR.resolve("server_public.key");

    private KeyPair keyPair;

    private KeyManager() {}

    public static KeyManager getInstance() { return INSTANCE; }

    public synchronized void init() throws Exception {
        if (Files.exists(PRIVATE_KEY) && Files.exists(PUBLIC_KEY)) {
            logger.info("Loading existing RSA key pair from disk");
            keyPair = loadKeyPair();
        } else {
            logger.info("Generating new RSA-{} key pair", EncryptionConfig.RSA_KEY_SIZE_BITS);
            keyPair = CryptoUtil.generateRSAKeyPair();
            persistKeyPair(keyPair);
        }
    }

    public PublicKey  getPublicKey()  { return keyPair.getPublic();  }
    public PrivateKey getPrivateKey() { return keyPair.getPrivate(); }

    // ── Persistence ──────────────────────────────────────────────────────────

    private void persistKeyPair(KeyPair kp) throws Exception {
        Files.createDirectories(KEY_DIR);
        Files.write(PRIVATE_KEY,
                Base64.getEncoder().encode(kp.getPrivate().getEncoded()));
        Files.write(PUBLIC_KEY,
                Base64.getEncoder().encode(kp.getPublic().getEncoded()));
        logger.info("RSA key pair saved to {}", KEY_DIR.toAbsolutePath());
    }

    private KeyPair loadKeyPair() throws Exception {
        KeyFactory kf = KeyFactory.getInstance(EncryptionConfig.RSA_ALGORITHM);

        byte[] privBytes = Base64.getDecoder().decode(Files.readAllBytes(PRIVATE_KEY));
        byte[] pubBytes  = Base64.getDecoder().decode(Files.readAllBytes(PUBLIC_KEY));

        PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
        PublicKey  pub  = kf.generatePublic (new X509EncodedKeySpec (pubBytes));
        return new KeyPair(pub, priv);
    }
}
