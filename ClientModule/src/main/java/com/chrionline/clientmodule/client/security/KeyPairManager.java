package com.chrionline.clientmodule.client.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

/**
 * Manages RSA key pairs on the client machine.
 *
 * Binary file format (.chrikey):
 * ┌──────────────────────────────────────────────────┐
 * │  MAGIC  (4 bytes)  : 0x43484B59  ("CHKY")        │
 * │  VERSION (2 bytes) : 0x0001                       │
 * │  ALG_LEN (2 bytes) : length of algorithm string  │
 * │  ALG     (n bytes) : algorithm (e.g. "RSA")       │
 * │  PRIV_LEN(4 bytes) : private key byte length      │
 * │  PRIV    (n bytes) : PKCS8 encoded private key    │
 * │  PUB_LEN (4 bytes) : public key byte length       │
 * │  PUB     (n bytes) : X509 encoded public key      │
 * └──────────────────────────────────────────────────┘
 *
 * The file is stored in the user's home directory under .chrionline/keys/<deviceName>.chrikey
 * It is listed in .gitignore under "keys".
 */
public class KeyPairManager {

    private static final Logger logger = LoggerFactory.getLogger(KeyPairManager.class);

    private static final int    MAGIC    = 0x43484B59;  // "CHKY"
    private static final short  VERSION  = 0x0001;
    private static final String ALG      = "RSA";
    private static final int    KEY_SIZE = 2048;

    // Base directory: ~/.chrionline/keys/
    private static final Path KEY_DIR = Paths.get(
            System.getProperty("user.home"), ".chrionline", "keys"
    );

    // ── Key Generation ────────────────────────────────────────────────────

    /**
     * Generate a fresh RSA-2048 key pair and persist it as a binary .chrikey file.
     *
     * @param deviceName  Label for this device (e.g. "Laptop 1")
     * @return            The generated KeyPair
     */
    public static KeyPair generateAndSave(String deviceName) throws Exception {
        logger.info("Generating RSA-{} key pair for device: {}", KEY_SIZE, deviceName);

        KeyPairGenerator gen = KeyPairGenerator.getInstance(ALG);
        gen.initialize(KEY_SIZE, new SecureRandom());
        KeyPair kp = gen.generateKeyPair();

        saveToFile(deviceName, kp);

        logger.info("Key pair saved for device: {}", deviceName);
        return kp;
    }

    // ── Binary File I/O ───────────────────────────────────────────────────

    /**
     * Persist a KeyPair to a binary .chrikey file.
     */
    public static void saveToFile(String deviceName, KeyPair kp) throws IOException {
        Files.createDirectories(KEY_DIR);
        Path filePath = getKeyFilePath(deviceName);

        byte[] algBytes  = ALG.getBytes("UTF-8");
        byte[] privBytes = kp.getPrivate().getEncoded();   // PKCS8
        byte[] pubBytes  = kp.getPublic().getEncoded();    // X509

        int totalSize = 4 + 2 + 2 + algBytes.length + 4 + privBytes.length + 4 + pubBytes.length;
        ByteBuffer buf = ByteBuffer.allocate(totalSize);

        buf.putInt(MAGIC);
        buf.putShort(VERSION);
        buf.putShort((short) algBytes.length);
        buf.put(algBytes);
        buf.putInt(privBytes.length);
        buf.put(privBytes);
        buf.putInt(pubBytes.length);
        buf.put(pubBytes);

        Files.write(filePath, buf.array());
        logger.info("Key file written: {}", filePath);
    }

    /**
     * Load a KeyPair from a binary .chrikey file.
     *
     * @param deviceName  The device name used when generating the key
     */
    public static KeyPair loadFromFile(String deviceName) throws Exception {
        Path filePath = getKeyFilePath(deviceName);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Key file not found: " + filePath);
        }

        byte[] raw = Files.readAllBytes(filePath);
        ByteBuffer buf = ByteBuffer.wrap(raw);

        int magic = buf.getInt();
        if (magic != MAGIC) {
            throw new IOException("Invalid key file: bad magic bytes");
        }

        short version = buf.getShort();
        if (version != VERSION) {
            throw new IOException("Unsupported key file version: " + version);
        }

        short algLen = buf.getShort();
        byte[] algBytes = new byte[algLen];
        buf.get(algBytes);
        String algorithm = new String(algBytes, "UTF-8");

        int privLen = buf.getInt();
        byte[] privBytes = new byte[privLen];
        buf.get(privBytes);

        int pubLen = buf.getInt();
        byte[] pubBytes = new byte[pubLen];
        buf.get(pubBytes);

        KeyFactory kf = KeyFactory.getInstance(algorithm);
        PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
        PublicKey  pub  = kf.generatePublic(new X509EncodedKeySpec(pubBytes));

        logger.info("Key pair loaded from file: {}", filePath);
        return new KeyPair(pub, priv);
    }

    /**
     * Check whether a key file exists for the given device name.
     */
    public static boolean keyFileExists(String deviceName) {
        return Files.exists(getKeyFilePath(deviceName));
    }

    /**
     * Delete the key file for a device (e.g. when revoking locally).
     */
    public static boolean deleteKeyFile(String deviceName) {
        try {
            return Files.deleteIfExists(getKeyFilePath(deviceName));
        } catch (IOException e) {
            logger.error("Failed to delete key file for {}: {}", deviceName, e.getMessage());
            return false;
        }
    }

    // ── Crypto Utilities ──────────────────────────────────────────────────

    /**
     * Derive the public key's Base64 encoding (X509) for server storage.
     */
    public static String encodePublicKey(PublicKey pub) {
        return Base64.getEncoder().encodeToString(pub.getEncoded());
    }

    /**
     * Compute the SHA-256 fingerprint of a public key.
     * fingerprint = hex( SHA-256( X509_bytes(publicKey) ) )
     */
    public static String computeFingerprint(PublicKey pub) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(pub.getEncoded());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /**
     * Reconstruct a PublicKey from its Base64-encoded X509 bytes.
     */
    public static PublicKey decodePublicKey(String base64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64);
        KeyFactory kf = KeyFactory.getInstance(ALG);
        return kf.generatePublic(new X509EncodedKeySpec(bytes));
    }

    // ── File Path ─────────────────────────────────────────────────────────

    /**
     * Returns the canonical path for a device's key file.
     * Sanitises the device name so it's safe for filesystem use.
     */
    public static Path getKeyFilePath(String deviceName) {
        String safeName = deviceName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return KEY_DIR.resolve(safeName + ".chrikey");
    }

    /**
     * Return the key directory path (for display in UI).
     */
    public static Path getKeyDirectory() {
        return KEY_DIR;
    }
}