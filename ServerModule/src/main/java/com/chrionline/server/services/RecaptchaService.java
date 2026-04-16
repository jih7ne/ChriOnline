package com.chrionline.server.services;

import com.chrionline.core.constants.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;

public class RecaptchaService {

    private static final Logger logger = LoggerFactory.getLogger(RecaptchaService.class);

    public boolean verify(String captchaToken) {
        if (captchaToken == null || captchaToken.isBlank()) {
            logger.warn("Token reCAPTCHA absent ou vide.");
            return false;
        }
        try {
            URL url = new URL(AppConstants.RECAPTCHA_VERIFY_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            String params = "secret=" + AppConstants.RECAPTCHA_SECRET_KEY
                    + "&response=" + captchaToken;
            try (OutputStream os = conn.getOutputStream()) {
                os.write(params.getBytes());
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            String body = sb.toString();
            logger.debug("Réponse reCAPTCHA : {}", body);
            return body.contains("\"success\": true") || body.contains("\"success\":true");

        } catch (Exception e) {
            logger.error("Erreur lors de la vérification reCAPTCHA", e);
            return false;
        }
    }
}