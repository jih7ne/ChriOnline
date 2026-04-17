package com.chrionline.server.security;

import com.chrionline.core.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;

public class SignatureVerifier {

    private static final Logger logger = LoggerFactory.getLogger(SignatureVerifier.class);

    public static boolean verify(String challenge, byte[] signatureBytes, PublicKey publicKey){
        try{
            Signature signature = Signature.getInstance(AppConfig.SIGNATURE_ALGO);
            signature.initVerify(publicKey);
            signature.update(challenge.getBytes());
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            logger.error("Signature Verification Failed");
            throw new RuntimeException(e);
        }

    }
}
