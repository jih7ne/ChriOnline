package com.chrionline.server.controllers;

import com.chrionline.core.config.ServerConfig;
import com.chrionline.core.constants.AppConstants;
import com.chrionline.core.interfaces.IController;
import com.chrionline.core.utils.JsonUtils;
import com.chrionline.network.protocol.AppResponse;
import com.chrionline.network.protocol.AppRequest;
import com.chrionline.server.data.dto.AuthPayloads.*;
import com.chrionline.server.security.ChallengeGenerator;
import com.chrionline.server.security.LoginAttemptGuard;
import com.chrionline.server.security.PasswordValidator;
import com.chrionline.server.services.RecaptchaService;
import com.chrionline.server.services.TwoFactorService;
import com.chrionline.server.services.TwoFactorVerifier;
import com.chrionline.shared.models.Adresse;
import com.chrionline.shared.models.Utilisateur;
import com.chrionline.server.repositories.UtilisateurRepository;
import com.chrionline.server.services.AdresseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuthController implements IController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final Map<String, Utilisateur> sessions = new ConcurrentHashMap<>();

    private final TwoFactorService   twoFactorService   = new TwoFactorService();
    private final TwoFactorVerifier twoFactorVerifier  = new TwoFactorVerifier();

    /**
     * Guard de rate-limiting en mémoire — initialisé une seule fois au chargement
     * de la classe. Plus aucune dépendance à la base de données.
     */
    private static final LoginAttemptGuard attemptGuard = new LoginAttemptGuard();
    private final RecaptchaService recaptchaService = new RecaptchaService();

    // ── Extraction de l'IP depuis les headers de la requête ───────────────

    /**
     * Récupère l'adresse IP du client depuis les headers de la requête.
     * L'IP est injectée par ClientHandler via le header "client-address".
     * Si absent (tests unitaires, etc.), retourne "unknown".
     */
    private static String extractIp(AppRequest request) {
        if (request == null) return "unknown";
        String ip = request.getHeader("client-address");
        if (ip != null && !ip.isBlank()) return ip.trim();
        // Fallback sur le clientId (format "ip:port")
        String clientId = request.getClientId();
        if (clientId != null && clientId.contains(":")) {
            return clientId.substring(0, clientId.lastIndexOf(':'));
        }
        return "unknown";
    }

    // ─── LOGIN ───────────────────────────────────────────────────────────────
    public String login(AppRequest request) {
        String ip = extractIp(request);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rawCheck = JsonUtils.fromJson(request.getPayload(), Map.class);
            String captchaToken = rawCheck != null ? (String) rawCheck.get("captchaToken") : null;
            if (!recaptchaService.verify(captchaToken)) {
                logger.warn("reCAPTCHA invalide depuis IP : {}", ip);
                return AppResponse.error("Validation reCAPTCHA échouée. Veuillez réessayer.");
            }
        } catch (Exception e) {
            logger.error("Erreur vérification captcha", e);
            return AppResponse.error("Erreur lors de la vérification reCAPTCHA.");
        }
        // 1 — Vérification blocage IP
        if (attemptGuard.isBlocked(ip)) {
            long remaining = attemptGuard.minutesRemaining(ip);
            logger.warn("Tentative de connexion depuis IP bloquée : {}", ip);
            return AppResponse.error(
                    "Trop de tentatives échouées. Réessayez dans " + remaining + " minute(s)."
            );
        }

        try {
            LoginPayload p = JsonUtils.fromJson(request.getPayload(), LoginPayload.class);
            if (p == null || p.email == null || p.password == null)
                return AppResponse.badRequest("email et password requis.");

            Utilisateur u = repo().getByEmail(p.email);

            // 2 — Identifiants incorrects → enregistrer l'échec
            if (u == null || !hash(p.password).equals(u.getMotDePasse())) {
                boolean justBlocked = attemptGuard.recordFailure(ip);
                if (justBlocked) {
                    return AppResponse.error(
                            "Compte temporairement bloqué après " + LoginAttemptGuard.MAX_ATTEMPTS +
                                    " tentatives échouées. Réessayez dans " +
                                    LoginAttemptGuard.LOCK_MINUTES + " minutes."
                    );
                }
                int attemptsLeft = LoginAttemptGuard.MAX_ATTEMPTS - attemptGuard.getAttempts(ip);
                String hint = attemptsLeft > 0
                        ? " (" + attemptsLeft + " tentative(s) restante(s))"
                        : "";
                return AppResponse.error("Email ou mot de passe incorrect." + hint);
            }

            // 3 — Compte inactif
            if ("inactif".equals(u.getStatut())) {
                // On enregistre aussi l'échec pour compter cette tentative
                attemptGuard.recordFailure(ip);
                return AppResponse.error("Compte bloqué. Contactez un administrateur.");
            }


            // 4 — Succès : on réinitialise le compteur
            attemptGuard.recordSuccess(ip);

            if (u.isTwoFactorEnabled()) {
                String tempToken = twoFactorService.createPendingToken(u.getId());
                Map<String, Object> data = new HashMap<>();
                data.put("requires2FA", true);
                data.put("tempToken",   tempToken);
                logger.info("2FA requis pour : {}", u.getEmail());
                return AppResponse.success(data);
            }

            String token = UUID.randomUUID().toString();
            sessions.put(token, u);
            logger.info("Login réussi : {} depuis {}", u.getEmail(), ip);
            return AppResponse.success(userData(u, token));

        } catch (Exception e) {
            logger.error("Erreur login", e);
            return AppResponse.error("Erreur serveur : " + e.getMessage());
        }
    }



    public String verifySignedChallenge(AppRequest request){
        return null;
    }

    // ─── REGISTER ────────────────────────────────────────────────────────────
    public String register(AppRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = JsonUtils.fromJson(request.getPayload(), Map.class);
            if (raw == null) return AppResponse.badRequest("Payload invalide.");
            String captchaToken = (String) raw.get("captchaToken");
            if (!recaptchaService.verify(captchaToken)) {
                logger.warn("reCAPTCHA invalide à l'inscription.");
                return AppResponse.error("Validation reCAPTCHA échouée. Veuillez réessayer.");
            }
            String nom             = (String) raw.get("nom");
            String prenom          = (String) raw.get("prenom");
            String email           = (String) raw.get("email");
            String password        = (String) raw.get("password");
            String questionSecrete = (String) raw.get("questionSecrete");
            String reponseSecrete  = (String) raw.get("reponseSecrete");

            if (nom == null || prenom == null || email == null || password == null)
                return AppResponse.badRequest("Tous les champs sont requis.");
            if (!email.contains("@"))
                return AppResponse.badRequest("Email invalide.");

            // Validation de la complexité du mot de passe
            PasswordValidator.ValidationResult pwdResult = PasswordValidator.validate(password);
            if (!pwdResult.valid()) {
                return AppResponse.badRequest(pwdResult.firstError());
            }

            if (repo().emailExiste(email))
                return AppResponse.error("Cet email est déjà utilisé.");

            Utilisateur u = new Utilisateur();
            u.setNom(nom);
            u.setPrenom(prenom);
            u.setEmail(email);
            u.setMotDePasse(hash(password));
            u.setRole("client");
            u.setStatut("actif");
            u.setQuestionSecrete(questionSecrete);
            u.setReponseSecrete(reponseSecrete != null ? reponseSecrete.toLowerCase().trim() : null);

            if (!repo().add(u)) return AppResponse.error("Échec de l'inscription.");

            @SuppressWarnings("unchecked")
            Map<String, Object> adresseData = (Map<String, Object>) raw.get("adresse");
            if (adresseData != null) {
                String rue        = (String) adresseData.get("rue");
                String ville      = (String) adresseData.get("ville");
                String codePostal = (String) adresseData.get("code_postal");

                if (rue != null && !rue.isBlank()
                        && ville != null && !ville.isBlank()
                        && codePostal != null && !codePostal.isBlank()) {

                    Adresse adresse = new Adresse();
                    adresse.setId_utilisateur(u.getId());
                    adresse.setRue(rue);
                    adresse.setComplement((String) adresseData.getOrDefault("complement", ""));
                    adresse.setVille(ville);
                    adresse.setCode_postal(codePostal);
                    adresse.setPays((String) adresseData.getOrDefault("pays", "Maroc"));
                    adresse.setEst_principale(true);

                    AdresseService adresseService = ServerConfig.getService(AdresseService.class);
                    if (adresseService != null) {
                        adresseService.ajouterAdresse(adresse);
                        logger.info("Adresse principale créée pour utilisateur id={}", u.getId());
                    }
                }
            }

            String token = UUID.randomUUID().toString();
            sessions.put(token, u);
            logger.info("Inscription réussie : {}", u.getEmail());
            return AppResponse.success(userData(u, token));

        } catch (Exception e) {
            logger.error("Erreur register", e);
            return AppResponse.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ─── LOGOUT ──────────────────────────────────────────────────────────────
    public String logout(AppRequest request) {
        String token = request.getAuthToken();
        if (token == null) return AppResponse.badRequest("Token manquant.");
        Utilisateur u = sessions.remove(token);
        if (u != null) logger.info("Déconnexion : {}", u.getEmail());
        return AppResponse.ok();
    }

    // ─── PROFIL ──────────────────────────────────────────────────────────────
    public String profil(AppRequest request) {
        Utilisateur u = session(request);
        if (u == null) return AppResponse.unauthorized("Session expirée ou invalide.");
        return AppResponse.success(userData(u, request.getAuthToken()));
    }

    // ─── UPDATE PROFIL ───────────────────────────────────────────────────────
    public String updateprofil(AppRequest request) {
        Utilisateur u = session(request);
        if (u == null) return AppResponse.unauthorized("Session expirée.");
        UpdateProfilPayload p = JsonUtils.fromJson(request.getPayload(), UpdateProfilPayload.class);
        if (p == null) return AppResponse.badRequest("Données invalides.");
        if (p.nom != null)    u.setNom(p.nom);
        if (p.prenom != null) u.setPrenom(p.prenom);
        if (p.email != null && p.email.contains("@")) u.setEmail(p.email);
        return repo().update(u)
                ? AppResponse.success(userData(u, request.getAuthToken()))
                : AppResponse.error("Mise à jour échouée.");
    }

    // ─── UPDATE PASSWORD (authentifié — nécessite l'ancien mot de passe) ─────
    public String updatepassword(AppRequest request) {
        Utilisateur u = session(request);
        if (u == null) return AppResponse.unauthorized("Session expirée.");
        UpdatePasswordPayload p = JsonUtils.fromJson(request.getPayload(), UpdatePasswordPayload.class);
        if (p == null || p.ancien == null || p.nouveau == null)
            return AppResponse.badRequest("ancien et nouveau requis.");
        if (!hash(p.ancien).equals(u.getMotDePasse()))
            return AppResponse.error("Ancien mot de passe incorrect.");

        // Validation de la complexité du nouveau mot de passe
        PasswordValidator.ValidationResult result = PasswordValidator.validate(p.nouveau);
        if (!result.valid()) {
            return AppResponse.badRequest(result.firstError());
        }

        u.setMotDePasse(hash(p.nouveau));
        repo().updatePassword(u.getId(), u.getMotDePasse());
        return AppResponse.ok();
    }

    // ─── GET QUESTION ─────────────────────────────────────────────────────────
    public String getquestion(AppRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> payload = JsonUtils.fromJson(request.getPayload(), Map.class);
            if (payload == null || payload.get("email") == null)
                return AppResponse.badRequest("email requis.");

            Utilisateur u = repo().getByEmail(payload.get("email"));
            if (u == null)
                return AppResponse.error("Aucun compte associé à cet e-mail.");

            String question = u.getQuestionSecrete();
            if (question == null || question.isBlank())
                return AppResponse.error("Aucune question de sécurité enregistrée pour ce compte.");

            Map<String, Object> data = new HashMap<>();
            data.put("question", question);
            return AppResponse.success(data);

        } catch (Exception e) {
            logger.error("Erreur getquestion", e);
            return AppResponse.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ─── VERIFY ANSWER ────────────────────────────────────────────────────────
    public String verifyanswer(AppRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> payload = JsonUtils.fromJson(request.getPayload(), Map.class);
            if (payload == null || payload.get("email") == null || payload.get("reponse") == null)
                return AppResponse.badRequest("email et reponse requis.");

            Utilisateur u = repo().getByEmail(payload.get("email"));
            if (u == null) return AppResponse.error("Compte introuvable.");

            String expected = u.getReponseSecrete();
            String provided = payload.get("reponse").toLowerCase().trim();

            if (expected == null || !expected.toLowerCase().trim().equals(provided))
                return AppResponse.error("Réponse incorrecte. Veuillez réessayer.");

            logger.info("Réponse secrète vérifiée pour : {}", u.getEmail());
            return AppResponse.ok();

        } catch (Exception e) {
            logger.error("Erreur verifyanswer", e);
            return AppResponse.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ─── RESET PASSWORD ───────────────────────────────────────────────────────
    public String resetpassword(AppRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> payload = JsonUtils.fromJson(request.getPayload(), Map.class);
            if (payload == null || payload.get("email") == null || payload.get("nouveau") == null)
                return AppResponse.badRequest("email et nouveau requis.");

            Utilisateur u = repo().getByEmail(payload.get("email"));
            if (u == null) return AppResponse.error("Compte introuvable.");

            String nouveau = payload.get("nouveau");

            // Validation de la complexité du nouveau mot de passe
            PasswordValidator.ValidationResult result = PasswordValidator.validate(nouveau);
            if (!result.valid()) {
                return AppResponse.badRequest(result.firstError());
            }

            repo().updatePassword(u.getId(), hash(nouveau));
            sessions.entrySet().removeIf(e -> e.getValue().getId() == u.getId());

            logger.info("Mot de passe réinitialisé pour : {}", u.getEmail());
            return AppResponse.ok();

        } catch (Exception e) {
            logger.error("Erreur resetpassword", e);
            return AppResponse.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ─── ADMIN : LIST ─────────────────────────────────────────────────────────
    public String listusers(AppRequest request) {
        if (!isAdmin(request)) return AppResponse.unauthorized("Droits admin requis.");
        return AppResponse.success(repo().getAll());
    }

    // ─── ADMIN : BLOCK ────────────────────────────────────────────────────────
    public String blockuser(AppRequest request) {
        if (!isAdmin(request)) return AppResponse.unauthorized("Droits admin requis.");
        IdPayload p = JsonUtils.fromJson(request.getPayload(), IdPayload.class);
        if (p == null) return AppResponse.badRequest("id requis.");
        return repo().updateStatut(p.id, "inactif") ? AppResponse.ok() : AppResponse.error("Échec.");
    }

    // ─── ADMIN : UNBLOCK ──────────────────────────────────────────────────────
    public String unblockuser(AppRequest request) {
        if (!isAdmin(request)) return AppResponse.unauthorized("Droits admin requis.");
        IdPayload p = JsonUtils.fromJson(request.getPayload(), IdPayload.class);
        if (p == null) return AppResponse.badRequest("id requis.");
        return repo().updateStatut(p.id, "actif") ? AppResponse.ok() : AppResponse.error("Échec.");
    }

    // ─── ADMIN : DELETE ───────────────────────────────────────────────────────
    public String deleteuser(AppRequest request) {
        if (!isAdmin(request)) return AppResponse.unauthorized("Droits admin requis.");
        IdPayload p = JsonUtils.fromJson(request.getPayload(), IdPayload.class);
        if (p == null) return AppResponse.badRequest("id requis.");
        sessions.entrySet().removeIf(e -> e.getValue().getId() == p.id);
        return repo().delete(p.id) ? AppResponse.ok() : AppResponse.error("Échec.");
    }

    // ─── TWO FACTOR AUTH ─────────────────────────────────────────────────────

    public String twofactorenable(AppRequest request) {
        Utilisateur u = session(request);
        if (u == null) return AppResponse.unauthorized("Session expirée.");

        String qrCodeBase64 = twoFactorService.initEnable(u.getId());
        if (qrCodeBase64 == null) return AppResponse.error("Impossible d'activer le 2FA.");

        Map<String, Object> data = new HashMap<>();
        data.put("qrCode", qrCodeBase64);
        return AppResponse.success(data);
    }

    public String twofactorenableconfirm(AppRequest request) {
        Utilisateur u = session(request);
        if (u == null) return AppResponse.unauthorized("Session expirée.");

        @SuppressWarnings("unchecked")
        Map<String, String> payload = JsonUtils.fromJson(request.getPayload(), Map.class);
        if (payload == null || payload.get("code") == null)
            return AppResponse.badRequest("Code requis.");

        return twoFactorService.confirmEnable(u.getId(), payload.get("code"))
                ? AppResponse.ok()
                : AppResponse.error("Code invalide. Réessayez.");
    }

    public String twofactorverify(AppRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> payload = JsonUtils.fromJson(request.getPayload(), Map.class);
            if (payload == null) return AppResponse.badRequest("Payload invalide.");

            Utilisateur u = twoFactorVerifier.verify(payload.get("tempToken"), payload.get("code"));
            if (u == null) return AppResponse.error("Code invalide ou expiré.");

            String token = UUID.randomUUID().toString();
            sessions.put(token, u);
            return AppResponse.success(userData(u, token));

        } catch (Exception e) {
            return AppResponse.error("Erreur 2FA : " + e.getMessage());
        }
    }

    public String twofactordisable(AppRequest request) {
        Utilisateur u = session(request);
        if (u == null) return AppResponse.unauthorized("Session expirée.");

        @SuppressWarnings("unchecked")
        Map<String, String> payload = JsonUtils.fromJson(request.getPayload(), Map.class);
        if (payload == null || payload.get("code") == null)
            return AppResponse.badRequest("Code requis.");

        return twoFactorService.disable(u.getId(), payload.get("code"))
                ? AppResponse.ok()
                : AppResponse.error("Code invalide.");
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private Utilisateur session(AppRequest request) {
        String token = request.getAuthToken();
        return token != null ? sessions.get(token) : null;
    }

    private boolean isAdmin(AppRequest request) {
        Utilisateur u = session(request);
        return u != null && "admin".equals(u.getRole());
    }

    private Map<String, Object> userData(Utilisateur u, String token) {
        Map<String, Object> data = new HashMap<>();
        data.put("token",  token);
        data.put("id",     u.getId());
        data.put("nom",    u.getNom());
        data.put("prenom", u.getPrenom());
        data.put("email",  u.getEmail());
        data.put("role",   u.getRole());
        data.put("statut", u.getStatut());
        return data;
    }

    private UtilisateurRepository repo() {
        return ServerConfig.getRepo(UtilisateurRepository.class);
    }

    public static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance(AppConstants.HASH_ALGORITHM);
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException("Hash error", e); }
    }


    public String twofactorstatus(AppRequest request) {
        Utilisateur u = session(request);
        if (u == null) return AppResponse.unauthorized("Session expirée.");
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", u.isTwoFactorEnabled());
        return AppResponse.success(data);
    }

    public static Map<String, Utilisateur> getSessions() { return sessions; }
}