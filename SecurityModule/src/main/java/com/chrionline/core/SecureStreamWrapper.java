package com.chrionline.core;

import java.io.*;

/**
 * Thin wrapper that makes encryption transparent.
 * After construction, callers read/write exactly as before —
 * encryption/decryption happens inside.
 */
public class SecureStreamWrapper {

    private final BufferedReader rawIn;
    private final PrintWriter    rawOut;
    private final SessionCipher  cipher;

    public SecureStreamWrapper(BufferedReader rawIn, PrintWriter rawOut, SessionCipher cipher) {
        this.rawIn  = rawIn;
        this.rawOut = rawOut;
        this.cipher = cipher;
    }

    /** Encrypt and send one line. */
    public void writeLine(String plaintext) throws Exception {
        rawOut.println(cipher.encrypt(plaintext));
    }

    /** Read one line and decrypt it. Returns null on EOF. */
    public String readLine() throws Exception {
        String encrypted = rawIn.readLine();
        if (encrypted == null) return null;
        return cipher.decrypt(encrypted);
    }
}
