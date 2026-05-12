package com.chrionline.security.utils;

import com.chrionline.core.constants.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HmacUtil {

    private static final Logger logger = LoggerFactory.getLogger(HmacUtil.class);
    private static final String HMAC_ALGO = "HmacSHA256";

    /**
     * Génère une signature HMAC-SHA256 pour les données fournies.
     * @param data Les données à signer.
     * @return La signature encodée en Base64, ou null en cas d'erreur.
     */
    public static String sign(String data) {
        if (data == null) return null;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    AppConstants.HMAC_SECRET_KEY.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            logger.error("Erreur lors de la génération HMAC", e);
            return null;
        }
    }

    /**
     * Vérifie si la signature fournie correspond aux données.
     * @param data Les données d'origine.
     * @param signature La signature encodée en Base64.
     * @return true si la signature est valide.
     */
    public static boolean verify(String data, String signature) {
        if (data == null || signature == null) return false;
        String expectedSignature = sign(data);
        return expectedSignature != null && expectedSignature.equals(signature);
    }
}
