package com.chrionline.clientmodule.client.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.spec.*;
import java.util.Base64;
import java.math.BigInteger;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Manages RSA key pairs on the client machine using a PKCS12 KeyStore.
 *
 * Storage: ~/.chrionline/keys/<deviceName>.chrikey  (same path, now PKCS12 format)
 *
 * KeyStore layout per file:
 *  - One PrivateKeyEntry aliased as "keypair"
 *  - The entry holds the RSA PrivateKey + a self-signed X.509 certificate
 *    wrapping the corresponding PublicKey (KeyStore requires a cert chain
 *    alongside any stored private key).
 *
 * The keystore password is derived from the device name so no separate
 * secret needs to be managed at this layer.  Swap in a stronger secret
 * (e.g. from a system credential store) by changing derivePassword().
 */
public class KeyPairManager {

    private static final Logger logger = LoggerFactory.getLogger(KeyPairManager.class);

    private static final String ALG           = "RSA";
    private static final int    KEY_SIZE      = 2048;
    private static final String KS_TYPE       = "PKCS12";
    private static final String KEY_ALIAS     = "keypair";
    private static final String SIG_ALG       = "SHA256withRSA";

    private static final Path KEY_DIR = Paths.get(
            System.getProperty("user.home"), ".chrionline", "keys"
    );

    // ── Key Generation ────────────────────────────────────────────────────

    /**
     * Generate a fresh RSA-2048 key pair and persist it as a PKCS12 .chrikey file.
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

    // ── KeyStore File I/O ─────────────────────────────────────────────────

    /**
     * Persist a KeyPair into a PKCS12 KeyStore file (.chrikey).
     *
     * A self-signed X.509 certificate is generated on the fly because
     * Java's KeyStore.PrivateKeyEntry mandates a certificate chain.
     */
    public static void saveToFile(String deviceName, KeyPair kp) throws Exception {
        Files.createDirectories(KEY_DIR);
        Path filePath = getKeyFilePath(deviceName);

        // Build a minimal self-signed cert to satisfy KeyStore requirements
        Certificate selfSigned = generateSelfSignedCert(deviceName, kp);

        KeyStore ks = KeyStore.getInstance(KS_TYPE);
        ks.load(null, null);  // initialise empty keystore

        ks.setKeyEntry(
                KEY_ALIAS,
                kp.getPrivate(),
                entryPassword(deviceName),
                new Certificate[]{ selfSigned }
        );

        char[] ksPassword = derivePassword(deviceName);
        try (OutputStream out = Files.newOutputStream(filePath)) {
            ks.store(out, ksPassword);
        }

        logger.info("Key file written: {}", filePath);
    }

    /**
     * Load a KeyPair from a PKCS12 .chrikey file.
     *
     * @param deviceName  The device name used when generating the key
     */
    public static KeyPair loadFromFile(String deviceName) throws Exception {
        Path filePath = getKeyFilePath(deviceName);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Key file not found: " + filePath);
        }

        char[] ksPassword = derivePassword(deviceName);
        KeyStore ks = KeyStore.getInstance(KS_TYPE);
        try (InputStream in = Files.newInputStream(filePath)) {
            ks.load(in, ksPassword);
        }

        Key privateKey = ks.getKey(KEY_ALIAS, entryPassword(deviceName));
        if (!(privateKey instanceof PrivateKey)) {
            throw new KeyStoreException("No private key found under alias: " + KEY_ALIAS);
        }

        Certificate cert = ks.getCertificate(KEY_ALIAS);
        PublicKey publicKey = cert.getPublicKey();

        logger.info("Key pair loaded from file: {}", filePath);
        return new KeyPair(publicKey, (PrivateKey) privateKey);
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

    /** Derive the public key's Base64 encoding (X509) for server storage. */
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

    /** Reconstruct a PublicKey from its Base64-encoded X509 bytes. */
    public static PublicKey decodePublicKey(String base64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64);
        KeyFactory kf = KeyFactory.getInstance(ALG);
        return kf.generatePublic(new X509EncodedKeySpec(bytes));
    }

    // ── File Path ─────────────────────────────────────────────────────────

    /** Returns the canonical path for a device's key file. */
    public static Path getKeyFilePath(String deviceName) {
        String safeName = deviceName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return KEY_DIR.resolve(safeName + ".chrikey");
    }

    /** Return the key directory path (for display in UI). */
    public static Path getKeyDirectory() {
        return KEY_DIR;
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    /**
     * Derive a keystore password from the device name.
     *
     * This is intentionally simple for portability; replace the body with
     * a call to the OS credential store (macOS Keychain, Windows DPAPI,
     * libsecret on Linux) if stronger protection is needed.
     */
    private static char[] derivePassword(String deviceName) {
        // Using a fixed application prefix + sanitised device name.
        // Swap for SecretKeyFactory + PBKDF2 if a user-supplied passphrase
        // is available in your authentication flow.
        return ("chrionline:" + deviceName).toCharArray();
    }

    /**
     * Password used to protect the individual key entry inside the keystore.
     * Using the same derivation as the store password keeps things simple,
     * but the two can diverge without any API changes.
     */
    private static char[] entryPassword(String deviceName) {
        return derivePassword(deviceName);
    }

    /**
     * Generate a minimal self-signed X.509 v1 certificate for the key pair.
     *
     * KeyStore.setKeyEntry() requires at least one certificate in the chain
     * even when the key won't be used for TLS.  This cert is never sent to
     * the server — only the raw public key bytes (via encodePublicKey) are.
     *
     * Uses Bouncy Castle if present; otherwise falls back to the internal
     * sun.security.x509 API (available in Oracle/OpenJDK, not on Android).
     * For Android targets, replace this method with a BouncyCastle-only
     * implementation and add bcpkix-jdk18on to your dependencies.
     */

    private static Certificate generateSelfSignedCert(String deviceName, KeyPair kp)
            throws Exception {

        X500Name dn = new X500Name("CN=" + deviceName + ", O=chrionline");

        Date from = new Date();
        Date to   = new Date(from.getTime() + 10L * 365 * 24 * 3600 * 1000); // 10 years

        BigInteger serial = new BigInteger(64, new SecureRandom());

        ContentSigner signer = new JcaContentSignerBuilder(SIG_ALG)
                .build(kp.getPrivate());

        X509CertificateHolder holder = new JcaX509v1CertificateBuilder(
                dn,         // issuer
                serial,
                from,
                to,
                dn,         // subject (same as issuer → self-signed)
                kp.getPublic()
        ).build(signer);

        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(holder);
    }
}