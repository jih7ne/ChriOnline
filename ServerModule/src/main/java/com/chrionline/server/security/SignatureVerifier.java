package com.chrionline.server.security;

import com.chrionline.core.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class SignatureVerifier {

    private static final Logger logger = LoggerFactory.getLogger(SignatureVerifier.class);

    public static boolean verify(String challenge, String signatureB64, PublicKey publicKey) {
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(signatureB64);

            Signature signature = Signature.getInstance(AppConfig.SIGNATURE_ALGO);
            signature.initVerify(publicKey);
            signature.update(challenge.getBytes(StandardCharsets.UTF_8));
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            logger.error("Signature verification failed", e);
            throw new RuntimeException(e);
        }
    }
}
