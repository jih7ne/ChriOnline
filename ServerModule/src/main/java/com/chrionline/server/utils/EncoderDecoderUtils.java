package com.chrionline.server.utils;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class EncoderDecoderUtils {

    public static PublicKey decodePublicKey(String base64, String algorithm) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(base64);
        KeyFactory kf = KeyFactory.getInstance(algorithm);
        return kf.generatePublic(new X509EncodedKeySpec(bytes));
    }
}
