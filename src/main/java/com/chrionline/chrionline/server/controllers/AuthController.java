package com.chrionline.chrionline.server.controllers;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.core.interfaces.IController;
import com.chrionline.chrionline.core.utils.JsonUtils;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.server.data.dto.AuthPayloads.*;
import com.chrionline.chrionline.server.data.models.Adresse;
import com.chrionline.chrionline.server.data.models.Utilisateur;
import com.chrionline.chrionline.server.repositories.UtilisateurRepository;
import com.chrionline.chrionline.server.services.AdresseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthController implements IController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final Map<String, Utilisateur> sessions = new ConcurrentHashMap<>();

    // ─── LOGIN ───────────────────────────────────────────────────────────────
    public String login(AppRequest request) {
        try {
            LoginPayload p = JsonUtils.fromJson(request.getPayload(), LoginPayload.class);
            if (p == null || p.email == null || p.password == null)
                return AppResponse.badRequest("email et password requis.");

            Utilisateur u = repo().getByEmail(p.email);
            if (u == null)
                return AppResponse.error("Email ou mot de passe incorrect.");
            if ("inactif".equals(u.getStatut()))
                return AppResponse.error("Compte bloqué. Contactez un administrateur.");
            if (!hash(p.password).equals(u.getMotDePasse()))
                return AppResponse.error("Email ou mot de passe incorrect.");

            String token = UUID.randomUUID().toString();
            sessions.put(token, u);
            logger.info("Login réussi : {}", u.getEmail());
            return AppResponse.success(userData(u, token));
        } catch (Exception e) {
            logger.error("Erreur login", e);
            return AppResponse.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ─── REGISTER ────────────────────────────────────────────────────────────
    public String register(AppRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = JsonUtils.fromJson(request.getPayload(), Map.class);
            if (raw == null) return AppResponse.badRequest("Payload invalide.");

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
            if (password.length() < 6)
                return AppResponse.badRequest("Mot de passe trop court (min. 6 caractères).");
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

                    AdresseService adresseService = AppConfig.getService(AdresseService.class);
                    if (adresseService != null) {
                        adresseService.ajouterAdresse(adresse);
                        logger.info("Adresse principale créée pour utilisateur id={}", u.getId());
                    } else {
                        logger.warn("AdresseService non disponible, adresse non sauvegardée");
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

    // ─── UPDATE PASSWORD (authenticated — requires old password) ─────────────
    public String updatepassword(AppRequest request) {
        Utilisateur u = session(request);
        if (u == null) return AppResponse.unauthorized("Session expirée.");
        UpdatePasswordPayload p = JsonUtils.fromJson(request.getPayload(), UpdatePasswordPayload.class);
        if (p == null || p.ancien == null || p.nouveau == null)
            return AppResponse.badRequest("ancien et nouveau requis.");
        if (!hash(p.ancien).equals(u.getMotDePasse()))
            return AppResponse.error("Ancien mot de passe incorrect.");
        if (p.nouveau.length() < 6)
            return AppResponse.badRequest("Nouveau mot de passe trop court.");
        u.setMotDePasse(hash(p.nouveau));
        repo().updatePassword(u.getId(), u.getMotDePasse());
        return AppResponse.ok();
    }

    // ─── GET QUESTION — step 1 of forgot-password flow ───────────────────────
    // IMPORTANT: method name is all lowercase because RequestDispatcher calls
    // action names via reflection using action.toLowerCase()
    // Client must send action: "getquestion"
    // INPUT  : { email }
    // OUTPUT : { question }
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

    // ─── VERIFY ANSWER — step 2 of forgot-password flow ─────────────────────
    // Client must send action: "verifyanswer"
    // INPUT  : { email, reponse }
    // OUTPUT : success / error
    public String verifyanswer(AppRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> payload = JsonUtils.fromJson(request.getPayload(), Map.class);
            if (payload == null || payload.get("email") == null || payload.get("reponse") == null)
                return AppResponse.badRequest("email et reponse requis.");

            Utilisateur u = repo().getByEmail(payload.get("email"));
            if (u == null)
                return AppResponse.error("Compte introuvable.");

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

    // ─── RESET PASSWORD — step 3 of forgot-password flow ────────────────────
    // Client must send action: "resetpassword"
    // INPUT  : { email, nouveau }
    // OUTPUT : success / error
    public String resetpassword(AppRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> payload = JsonUtils.fromJson(request.getPayload(), Map.class);
            if (payload == null || payload.get("email") == null || payload.get("nouveau") == null)
                return AppResponse.badRequest("email et nouveau requis.");

            Utilisateur u = repo().getByEmail(payload.get("email"));
            if (u == null)
                return AppResponse.error("Compte introuvable.");

            String nouveau = payload.get("nouveau");
            if (nouveau.length() < 6)
                return AppResponse.badRequest("Mot de passe trop court (min. 6 caractères).");

            repo().updatePassword(u.getId(), hash(nouveau));

            // Invalider toutes les sessions actives de cet utilisateur
            sessions.entrySet().removeIf(e -> e.getValue().getId() == u.getId());

            logger.info("Mot de passe réinitialisé pour : {}", u.getEmail());
            return AppResponse.ok();

        } catch (Exception e) {
            logger.error("Erreur resetpassword", e);
            return AppResponse.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ─── ADMIN : LIST ────────────────────────────────────────────────────────
    public String listusers(AppRequest request) {
        if (!isAdmin(request)) return AppResponse.unauthorized("Droits admin requis.");
        return AppResponse.success(repo().getAll());
    }

    // ─── ADMIN : BLOCK ───────────────────────────────────────────────────────
    public String blockuser(AppRequest request) {
        if (!isAdmin(request)) return AppResponse.unauthorized("Droits admin requis.");
        IdPayload p = JsonUtils.fromJson(request.getPayload(), IdPayload.class);
        if (p == null) return AppResponse.badRequest("id requis.");
        return repo().updateStatut(p.id, "inactif") ? AppResponse.ok() : AppResponse.error("Échec.");
    }

    // ─── ADMIN : UNBLOCK ─────────────────────────────────────────────────────
    public String unblockuser(AppRequest request) {
        if (!isAdmin(request)) return AppResponse.unauthorized("Droits admin requis.");
        IdPayload p = JsonUtils.fromJson(request.getPayload(), IdPayload.class);
        if (p == null) return AppResponse.badRequest("id requis.");
        return repo().updateStatut(p.id, "actif") ? AppResponse.ok() : AppResponse.error("Échec.");
    }

    // ─── ADMIN : DELETE ──────────────────────────────────────────────────────
    public String deleteuser(AppRequest request) {
        if (!isAdmin(request)) return AppResponse.unauthorized("Droits admin requis.");
        IdPayload p = JsonUtils.fromJson(request.getPayload(), IdPayload.class);
        if (p == null) return AppResponse.badRequest("id requis.");
        sessions.entrySet().removeIf(e -> e.getValue().getId() == p.id);
        return repo().delete(p.id) ? AppResponse.ok() : AppResponse.error("Échec.");
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
        return AppConfig.getRepo(UtilisateurRepository.class);
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

    public static Map<String, Utilisateur> getSessions() { return sessions; }
}