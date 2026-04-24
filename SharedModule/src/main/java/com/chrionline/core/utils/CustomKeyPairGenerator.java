package com.chrionline.core.utils;

import com.chrionline.core.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class CustomKeyPairGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CustomKeyPairGenerator.class);
    public static KeyPair generate(){
        KeyPairGenerator generator = null;
        try {
            generator = KeyPairGenerator.getInstance(AppConfig.KEY_GEN_ALGO);
            generator.initialize(AppConfig.KEY_GEN_SIZE_BITS);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Could not generate key.");
            throw new RuntimeException(e);
        }
    }
}
