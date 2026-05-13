package com.chrionline.clientmodule.client.security;

import com.chrionline.core.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.security.Signature;

public class Signer {
    private static final Logger logger = LoggerFactory.getLogger(Signer.class);

    public static byte[] sign(String challenge, PrivateKey privateKey){
        try{
            Signature signature = Signature.getInstance(AppConfig.SIGNATURE_ALGO);
            signature.initSign(privateKey);
            signature.update(challenge.getBytes());
            return signature.sign();
        } catch (Exception e) {
            logger.error("Signature Generation Failed");
            throw new RuntimeException(e);
        }

    }
}
