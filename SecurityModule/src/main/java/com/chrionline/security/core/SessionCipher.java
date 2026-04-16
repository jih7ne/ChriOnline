package com.chrionline.security.core;

import com.chrionline.security.config.EncryptionConfig;
import com.chrionline.security.utils.CryptoUtil;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


/**
 * Holds the negotiated AES session key for one TCP connection.
 * One instance per ClientHandler / TCPClient.
 */
public class SessionCipher {

    private volatile SecretKey sessionKey;
    private volatile boolean   ready = false;

    /** Called on the server side after RSA-decrypting the client's key offer. */
    public void initFromRawBytes(byte[] aesKeyBytes) {
        this.sessionKey = new SecretKeySpec(aesKeyBytes, EncryptionConfig.AES_ALGORITHM);
        this.ready      = true;
    }

    /** Called on the client side after generating a fresh AES key. */
    public void initFromGeneratedKey(SecretKey key) {
        this.sessionKey = key;
        this.ready      = true;
    }

    public String encrypt(String plaintext) throws Exception {
        assertReady();
        return CryptoUtil.aesEncrypt(plaintext, sessionKey);
    }

    public String decrypt(String ciphertext) throws Exception {
        assertReady();
        return CryptoUtil.aesDecrypt(ciphertext, sessionKey);
    }

    /** Export the raw key bytes so the client can RSA-encrypt them for the server. */
    public byte[] getRawKeyBytes() {
        assertReady();
        return sessionKey.getEncoded();
    }

    public boolean isReady() { return ready; }

    private void assertReady() {
        if (!ready) throw new IllegalStateException("SessionCipher not yet initialised");
    }
}
