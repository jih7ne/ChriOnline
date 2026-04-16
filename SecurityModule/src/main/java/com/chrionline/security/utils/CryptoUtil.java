package com.chrionline.security.utils;

import com.chrionline.security.config.EncryptionConfig;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * Stateless cryptographic primitives.
 * All methods are thread-safe — no shared mutable state.
 */
public final class CryptoUtil {

    private CryptoUtil() {}

    // ── AES-GCM ─────────────────────────────────────────────────────────────

    /** Generate a fresh 256-bit AES key. */
    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator kg = KeyGenerator.getInstance(EncryptionConfig.AES_ALGORITHM);
        kg.init(EncryptionConfig.AES_KEY_SIZE_BITS, new SecureRandom());
        return kg.generateKey();
    }

    /**
     * Encrypt plaintext with AES-256/GCM.
     * Returns Base64( IV || CipherText+Tag ) — the IV is prepended so the
     * receiver can extract it without any side-channel.
     */
    public static String aesEncrypt(String plaintext, SecretKey key) throws Exception {
        byte[] iv = new byte[EncryptionConfig.GCM_IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(EncryptionConfig.AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key,
                new GCMParameterSpec(EncryptionConfig.GCM_TAG_LENGTH_BITS, iv));

        byte[] cipherBytes = cipher.doFinal(plaintext.getBytes("UTF-8"));

        // Prepend IV so we transmit a single self-contained blob
        byte[] ivAndCipher = new byte[iv.length + cipherBytes.length];
        System.arraycopy(iv,          0, ivAndCipher, 0,         iv.length);
        System.arraycopy(cipherBytes, 0, ivAndCipher, iv.length, cipherBytes.length);

        return Base64.getEncoder().encodeToString(ivAndCipher);
    }

    /** Decrypt a blob produced by {@link #aesEncrypt}. */
    public static String aesDecrypt(String encryptedBase64, SecretKey key) throws Exception {
        byte[] ivAndCipher = Base64.getDecoder().decode(encryptedBase64);

        byte[] iv = new byte[EncryptionConfig.GCM_IV_LENGTH_BYTES];
        byte[] cipherBytes = new byte[ivAndCipher.length - iv.length];
        System.arraycopy(ivAndCipher, 0,         iv,          0, iv.length);
        System.arraycopy(ivAndCipher, iv.length, cipherBytes, 0, cipherBytes.length);

        Cipher cipher = Cipher.getInstance(EncryptionConfig.AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key,
                new GCMParameterSpec(EncryptionConfig.GCM_TAG_LENGTH_BITS, iv));

        return new String(cipher.doFinal(cipherBytes), "UTF-8");
    }

    // ── RSA-OAEP ────────────────────────────────────────────────────────────

    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(EncryptionConfig.RSA_ALGORITHM);
        kpg.initialize(EncryptionConfig.RSA_KEY_SIZE_BITS, new SecureRandom());
        return kpg.generateKeyPair();
    }

    /** Encrypt (wrap) an AES key with the server's RSA public key. */
    public static String rsaEncrypt(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(EncryptionConfig.RSA_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, buildOAEPParams());
        return Base64.getEncoder().encodeToString(cipher.doFinal(data));
    }

    /** Decrypt (unwrap) the AES key with the server's RSA private key. */
    public static byte[] rsaDecrypt(String encryptedBase64, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(EncryptionConfig.RSA_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey, buildOAEPParams());
        return cipher.doFinal(Base64.getDecoder().decode(encryptedBase64));
    }

    /** Encode a public key for wire transmission. */
    public static String encodePublicKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /** Reconstruct a PublicKey from its wire-encoded form. */
    public static PublicKey decodePublicKey(String base64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return java.security.KeyFactory.getInstance(EncryptionConfig.RSA_ALGORITHM)
                .generatePublic(new java.security.spec.X509EncodedKeySpec(bytes));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static OAEPParameterSpec buildOAEPParams() {
        return new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );
    }
}
