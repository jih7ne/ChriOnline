package com.chrionline.server.controllers;

import com.chrionline.core.config.AppConfig;
import com.chrionline.core.config.ServerConfig;
import com.chrionline.core.interfaces.IController;
import com.chrionline.core.utils.JsonUtils;
import com.chrionline.network.protocol.AppRequest;
import com.chrionline.network.protocol.AppResponse;
import com.chrionline.server.repositories.UserDeviceRepository;
import com.chrionline.server.security.ChallengeGenerator;
import com.chrionline.server.security.SignatureVerifier;
import com.chrionline.server.store.ChallengeStore;
import com.chrionline.server.utils.EncoderDecoderUtils;
import com.chrionline.shared.models.UserDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * Handles key-based authentication device management.
 *
 * Actions (all lowercase, matching RequestDispatcher reflection):
 *   registerdevice      — register a new public key for this device
 *   listdevices         — list all active devices for the authenticated user
 *   revokedevice        — revoke a device by id
 */
public class KeyAuthController implements IController {

    private static final Logger logger = LoggerFactory.getLogger(KeyAuthController.class);

    private UserDeviceRepository repo() {
        return ServerConfig.getRepo(UserDeviceRepository.class);
    }

    // ─── REGISTER DEVICE ──────────────────────────────────────────────────
    // INPUT : { userEmail, deviceName, publicKey (Base64), fingerprint }
    // OUTPUT: { device: UserDevice }
    public String registerdevice(AppRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = JsonUtils.fromJson(request.getPayload(), Map.class);
            if (payload == null) return AppResponse.badRequest("Payload requis.");

            String email       = (String) payload.get("userEmail");
            String deviceName  = (String) payload.get("deviceName");
            String publicKeyB64= (String) payload.get("publicKey");
            String fingerprint = (String) payload.get("fingerprint");

            if (email == null || deviceName == null || publicKeyB64 == null || fingerprint == null) {
                return AppResponse.badRequest("userEmail, deviceName, publicKey et fingerprint sont requis.");
            }

            // Server-side re-verification: recompute fingerprint from the submitted public key
            // This ensures the client can't send an arbitrary fingerprint
            String verified = recomputeFingerprint(publicKeyB64);
            if (verified == null || !verified.equalsIgnoreCase(fingerprint)) {
                logger.warn("Fingerprint mismatch for {} — client sent: {}, computed: {}",
                        email, fingerprint, verified);
                return AppResponse.error("Fingerprint invalide : ne correspond pas à la clé publique.");
            }

            // Reject duplicate fingerprints
            if (repo().fingerprintExists(fingerprint)) {
                return AppResponse.error("Cette clé est déjà enregistrée pour un appareil.");
            }

            UserDevice device = new UserDevice(email, deviceName, publicKeyB64, fingerprint, "RSA");
            boolean ok = repo().add(device);

            if (!ok) return AppResponse.error("Échec de l'enregistrement de l'appareil.");

            logger.info("Device registered: {} for {}", deviceName, email);
            return AppResponse.success(device, "Appareil enregistré avec succès.");

        } catch (Exception e) {
            logger.error("Erreur registerdevice", e);
            return AppResponse.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ─── LIST DEVICES ─────────────────────────────────────────────────────
    // INPUT : { userEmail }  OR  authToken in header (email extracted from session)
    // OUTPUT: [ UserDevice, ... ]
    public String listdevices(AppRequest request) {
        try {
            String email = extractEmail(request);
            if (email == null) return AppResponse.badRequest("userEmail requis.");

            List<UserDevice> devices = repo().getActiveDevicesByEmail(email);
            logger.info("Listed {} device(s) for {}", devices.size(), email);
            return AppResponse.success(devices);

        } catch (Exception e) {
            logger.error("Erreur listdevices", e);
            return AppResponse.error("Erreur serveur : " + e.getMessage());
        }
    }







    // ─── REVOKE DEVICE ────────────────────────────────────────────────────
    // INPUT : { id }
    // OUTPUT: ok
    public String revokedevice(AppRequest request) {
        try {
            Integer id = request.getInt("id");
            if (id == null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = JsonUtils.fromJson(request.getPayload(), Map.class);
                if (payload != null && payload.get("id") != null) {
                    id = ((Number) payload.get("id")).intValue();
                }
            }
            if (id == null) return AppResponse.badRequest("id est requis.");

            boolean ok = repo().revokeById(id);
            return ok ? AppResponse.ok() : AppResponse.error("Appareil introuvable ou déjà révoqué.");

        } catch (Exception e) {
            logger.error("Erreur revokedevice", e);
            return AppResponse.error("Erreur serveur : " + e.getMessage());
        }
    }





    public String requestLogin(AppRequest request) {
        Map<String, Object> payload = parsePayload(request);
        if (payload == null) return AppResponse.badRequest("No payload provided.");

        String email = (String) payload.get("email");
        logger.info("requestLogin called for email={}", email);

        if (!isValidEmail(email)) return AppResponse.badRequest("Valid userEmail required.");

        boolean isAdmin = repo().isAdmin(email);
        boolean hasKeys = repo().hasKeys(email);
        logger.info("isAdmin={} hasKeys={} for email={}", isAdmin, hasKeys, email);

        boolean authorized = isAdmin && hasKeys;
        if (!authorized) {
            logger.warn("Login denied: isAdmin={} hasKeys={}", isAdmin, hasKeys);
            return AppResponse.error("Login request denied.");
        }

        String id        = UUID.randomUUID().toString();
        String challenge = ChallengeGenerator.generate();
        long   expiresAt = System.currentTimeMillis() + AppConfig.CHALLENGE_TTL_MS;

        try {
            ChallengeStore.save(id, challenge, expiresAt);
        } catch (IOException e) {
            logger.error("Failed to persist challenge for email={}", email, e);
            return AppResponse.error("Could not initiate login. Please try again.");
        }

        return AppResponse.success(Map.of(
                "challengeId", id,
                "challenge",   challenge,
                "expiresAt",   expiresAt
        ));
    }

    public String login(AppRequest request) {
        Map<String, Object> payload = parsePayload(request);
        if (payload == null) return AppResponse.badRequest("No payload provided.");

        String email       = (String) payload.get("email");
        String signature   = (String) payload.get("signature");
        String challengeId = (String) payload.get("challengeId");
        String fingerprint = (String) payload.get("fingerprint");

        if (email == null || signature == null || challengeId == null || fingerprint == null) {
            return AppResponse.badRequest("userEmail, signature, challengeId and fingerprint are required.");
        }

        String base64PublicKey = repo().getPublicKey(fingerprint);
        if (base64PublicKey == null) return AppResponse.badRequest("Invalid fingerprint.");

        ChallengeStore.ChallengeEntry entry;
        try {
            Optional<ChallengeStore.ChallengeEntry> found = ChallengeStore.find(challengeId);
            if (found.isEmpty() || found.get().isExpired()) {
                ChallengeStore.delete(challengeId);
                return AppResponse.error("Challenge invalid or expired.");
            }
            entry = found.get();
        } catch (IOException e) {
            logger.error("Failed to read challenge id={}", challengeId, e);
            return AppResponse.error("Could not process login. Please try again.");
        }

        try {
            PublicKey publicKey = EncoderDecoderUtils.decodePublicKey(base64PublicKey, "RSA");
            boolean verified    = SignatureVerifier.verify(entry.challenge(), signature, publicKey);

            ChallengeStore.delete(challengeId);
            if (!verified) return AppResponse.error("Login failed.");

            Map<String, String> userInfo = repo().getUserInfo(email);
            if (userInfo == null) return AppResponse.error("User not found.");

            return AppResponse.success(userInfo, "Login successful.");

        } catch (Exception e) {
            logger.error("Signature verification error for challengeId={}", challengeId, e);
            return AppResponse.error("Could not verify signature.");
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    /**
     * Recompute fingerprint = hex( SHA-256( decoded(base64PublicKey) ) )
     */
    private String recomputeFingerprint(String base64PublicKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
            // Verify it's a valid public key
            KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
            // Compute SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(keyBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            logger.error("Failed to recompute fingerprint: {}", e.getMessage());
            return null;
        }
    }



    private String extractEmail(AppRequest request) {
        // Try parameter first, then payload
        String email = request.getString("userEmail");
        if (email != null) return email;
        if (request.getPayload() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> p = JsonUtils.fromJson(request.getPayload(), Map.class);
            if (p != null) return (String) p.get("userEmail");
        }
        return null;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(AppRequest request) {
        return JsonUtils.fromJson(request.getPayload(), Map.class);
    }
}