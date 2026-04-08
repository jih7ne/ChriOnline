package com.chrionline.server.services;

import com.chrionline.core.config.ServerConfig;
import com.chrionline.server.repositories.UtilisateurRepository;
import com.chrionline.server.utils.QrCodeUtils;
import com.chrionline.shared.models.Utilisateur;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Service 2FA basé sur TOTP (RFC 6238).
 * Compatible Google Authenticator, Authy, etc.
 */
public class TwoFactorService {

    // ── Tokens temporaires (après login réussi, avant validation 2FA) ────
    // tempToken → userId
    private static final Map<String, Integer> pendingTokens = new ConcurrentHashMap<>();

    private static final int    CODE_DIGITS  = 6;
    private static final int    TIME_STEP    = 30;  // secondes
    private static final String ISSUER       = "ChriOnline";
    private static final String ALGORITHM    = "HmacSHA1";

    // ── Génération du secret ──────────────────────────────────────────────

    /**
     * Génère un secret Base32 aléatoire de 20 octets (160 bits).
     */
    public String generateSecret() {
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return base32Encode(buffer);
    }

    // ── QR Code URL ───────────────────────────────────────────────────────

    /**
     * Retourne l'URL otpauth:// à encoder en QR code côté client.
     * Format : otpauth://totp/ISSUER:email?secret=SECRET&issuer=ISSUER
     */
    public String buildOtpAuthUrl(String email, String secret) {
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                ISSUER, email, secret, ISSUER, CODE_DIGITS, TIME_STEP
        );
    }

    // ── Activation 2FA ────────────────────────────────────────────────────

    /**
     * Étape 1 : génère un secret et le sauvegarde (non vérifié encore).
     * Retourne l'URL otpauth pour afficher le QR code.
     */

    public String initEnable(int userId) {
        Utilisateur u = repo().getById(userId);
        if (u == null) return null;
        String secret = generateSecret();
        repo().saveTwoFactorSecret(userId, secret);
        String otpUrl = buildOtpAuthUrl(u.getEmail(), secret);
        return QrCodeUtils.toBase64Png(otpUrl, 200, 200); // retourne le QR en Base64
    }

    /**
     * Étape 2 : l'utilisateur scanne le QR et saisit le premier code.
     * Si valide → 2FA activé définitivement.
     */
    public boolean confirmEnable(int userId, String code) {
        Utilisateur u = repo().getById(userId);
        if (u == null || u.getTwoFactorSecret() == null) return false;

        if (validateCode(u.getTwoFactorSecret(), code)) {
            repo().enableTwoFactor(userId);
            return true;
        }
        return false;
    }

    // ── Désactivation 2FA ─────────────────────────────────────────────────

    public boolean disable(int userId, String code) {
        Utilisateur u = repo().getById(userId);
        if (u == null || !u.isTwoFactorEnabled()) return false;

        if (validateCode(u.getTwoFactorSecret(), code)) {
            repo().disableTwoFactor(userId);
            return true;
        }
        return false;
    }

    // ── Validation du code TOTP ───────────────────────────────────────────

    /**
     * Valide le code TOTP avec une fenêtre de ±1 période (tolérance horloge).
     */
    public boolean validateCode(String secret, String code) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) return false;
        try {
            long timeIndex = Instant.now().getEpochSecond() / TIME_STEP;
            // Vérifier la période courante et les deux adjacentes (±30s)
            for (long delta = -1; delta <= 1; delta++) {
                String expected = generateTotp(secret, timeIndex + delta);
                if (expected.equals(code)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Token temporaire (entre login réussi et validation 2FA) ──────────

    public String createPendingToken(int userId) {
        String tempToken = "2fa_" + java.util.UUID.randomUUID();
        pendingTokens.put(tempToken, userId);
        // Expiration automatique après 5 minutes
        new Thread(() -> {
            try {
                Thread.sleep(5 * 60 * 1000L);
                pendingTokens.remove(tempToken);
            } catch (InterruptedException ignored) {}
        }).start();
        return tempToken;
    }

    public Integer getUserIdFromPendingToken(String tempToken) {
        return pendingTokens.get(tempToken);
    }

    public void removePendingToken(String tempToken) {
        pendingTokens.remove(tempToken);
    }

    // ── TOTP (RFC 6238) ───────────────────────────────────────────────────

    private String generateTotp(String secret, long timeIndex) throws Exception {
        byte[] key  = base32Decode(secret);
        byte[] data = longToBytes(timeIndex);

        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(key, ALGORITHM));
        byte[] hash = mac.doFinal(data);

        // Dynamic truncation
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset]     & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                |  (hash[offset + 3] & 0xFF);

        int otp = binary % (int) Math.pow(10, CODE_DIGITS);
        return String.format("%0" + CODE_DIGITS + "d", otp);
    }

    // ── Utils ─────────────────────────────────────────────────────────────

    private byte[] longToBytes(long value) {
        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return data;
    }

    // Encodage Base32 simple (alphabet RFC 4648)
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private String base32Encode(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(BASE32_CHARS.charAt((buffer >> (bitsLeft - 5)) & 31));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) result.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 31));
        return result.toString();
    }

    private byte[] base32Decode(String encoded) {
        encoded = encoded.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int buffer = 0, bitsLeft = 0, idx = 0;
        byte[] result = new byte[encoded.length() * 5 / 8];
        for (char c : encoded.toCharArray()) {
            buffer = (buffer << 5) | BASE32_CHARS.indexOf(c);
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[idx++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return result;
    }

    private UtilisateurRepository repo() {
        return ServerConfig.getRepo(UtilisateurRepository.class);
    }
}
