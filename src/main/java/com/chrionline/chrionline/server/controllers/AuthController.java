package com.chrionline.chrionline.server.controllers;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.core.interfaces.IController;
import com.chrionline.chrionline.core.utils.JsonUtils;
import com.chrionline.chrionline.network.protocol.ApiResponse;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.server.data.dto.AuthPayloads.*;
import com.chrionline.chrionline.server.data.models.Utilisateur;
import com.chrionline.chrionline.server.repositories.UtilisateurRepository;
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
                return ApiResponse.badRequest("email et password requis.");

            Utilisateur u = repo().getByEmail(p.email);
            if (u == null)
                return ApiResponse.error("Email ou mot de passe incorrect.");
            if ("inactif".equals(u.getStatut()))
                return ApiResponse.error("Compte bloqué. Contactez un administrateur.");
            if (!hash(p.password).equals(u.getMotDePasse()))
                return ApiResponse.error("Email ou mot de passe incorrect.");

            String token = UUID.randomUUID().toString();
            sessions.put(token, u);
            logger.info("Login réussi : {}", u.getEmail());
            return ApiResponse.success(userData(u, token));
        } catch (Exception e) {
            logger.error("Erreur login", e);
            return ApiResponse.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ─── REGISTER ────────────────────────────────────────────────────────────
    public String register(AppRequest request) {
        try {
            RegisterPayload p = JsonUtils.fromJson(request.getPayload(), RegisterPayload.class);
            if (p == null || p.nom == null || p.prenom == null || p.email == null || p.password == null)
                return ApiResponse.badRequest("Tous les champs sont requis.");
            if (!p.email.contains("@"))
                return ApiResponse.badRequest("Email invalide.");
            if (p.password.length() < 6)
                return ApiResponse.badRequest("Mot de passe trop court (min. 6 caractères).");
            if (repo().emailExiste(p.email))
                return ApiResponse.error("Cet email est déjà utilisé.");

            Utilisateur u = new Utilisateur();
            u.setNom(p.nom);
            u.setPrenom(p.prenom);
            u.setEmail(p.email);
            u.setMotDePasse(hash(p.password));
            u.setRole("client");
            u.setStatut("actif");

            if (!repo().add(u)) return ApiResponse.error("Échec de l'inscription.");

            String token = UUID.randomUUID().toString();
            sessions.put(token, u);
            logger.info("Inscription réussie : {}", u.getEmail());
            return ApiResponse.success(userData(u, token));
        } catch (Exception e) {
            logger.error("Erreur register", e);
            return ApiResponse.error("Erreur serveur : " + e.getMessage());
        }
    }

    // ─── LOGOUT ──────────────────────────────────────────────────────────────
    public String logout(AppRequest request) {
        String token = request.getAuthToken();
        if (token == null) return ApiResponse.badRequest("Token manquant.");
        Utilisateur u = sessions.remove(token);
        if (u != null) logger.info("Déconnexion : {}", u.getEmail());
        return ApiResponse.ok();
    }

    // ─── PROFIL ──────────────────────────────────────────────────────────────
    public String profil(AppRequest request) {
        Utilisateur u = session(request);
        if (u == null) return ApiResponse.unauthorized("Session expirée ou invalide.");
        return ApiResponse.success(userData(u, request.getAuthToken()));
    }

    // ─── UPDATE PROFIL ───────────────────────────────────────────────────────
    public String updateprofil(AppRequest request) {
        Utilisateur u = session(request);
        if (u == null) return ApiResponse.unauthorized("Session expirée.");
        UpdateProfilPayload p = JsonUtils.fromJson(request.getPayload(), UpdateProfilPayload.class);
        if (p == null) return ApiResponse.badRequest("Données invalides.");
        if (p.nom != null) u.setNom(p.nom);
        if (p.prenom != null) u.setPrenom(p.prenom);
        if (p.email != null && p.email.contains("@")) u.setEmail(p.email);
        return repo().update(u)
                ? ApiResponse.success(userData(u, request.getAuthToken()))
                : ApiResponse.error("Mise à jour échouée.");
    }

    // ─── UPDATE PASSWORD ─────────────────────────────────────────────────────
    public String updatepassword(AppRequest request) {
        Utilisateur u = session(request);
        if (u == null) return ApiResponse.unauthorized("Session expirée.");
        UpdatePasswordPayload p = JsonUtils.fromJson(request.getPayload(), UpdatePasswordPayload.class);
        if (p == null || p.ancien == null || p.nouveau == null)
            return ApiResponse.badRequest("ancien et nouveau requis.");
        if (!hash(p.ancien).equals(u.getMotDePasse()))
            return ApiResponse.error("Ancien mot de passe incorrect.");
        if (p.nouveau.length() < 6)
            return ApiResponse.badRequest("Nouveau mot de passe trop court.");
        u.setMotDePasse(hash(p.nouveau));
        repo().updatePassword(u.getId(), u.getMotDePasse());
        return ApiResponse.ok();
    }

    // ─── ADMIN : LIST ────────────────────────────────────────────────────────
    public String listusers(AppRequest request) {
        if (!isAdmin(request)) return ApiResponse.unauthorized("Droits admin requis.");
        return ApiResponse.success(repo().getAll());
    }

    // ─── ADMIN : BLOCK ───────────────────────────────────────────────────────
    public String blockuser(AppRequest request) {
        if (!isAdmin(request)) return ApiResponse.unauthorized("Droits admin requis.");
        IdPayload p = JsonUtils.fromJson(request.getPayload(), IdPayload.class);
        if (p == null) return ApiResponse.badRequest("id requis.");
        return repo().updateStatut(p.id, "inactif") ? ApiResponse.ok() : ApiResponse.error("Échec.");
    }

    // ─── ADMIN : UNBLOCK ─────────────────────────────────────────────────────
    public String unblockuser(AppRequest request) {
        if (!isAdmin(request)) return ApiResponse.unauthorized("Droits admin requis.");
        IdPayload p = JsonUtils.fromJson(request.getPayload(), IdPayload.class);
        if (p == null) return ApiResponse.badRequest("id requis.");
        return repo().updateStatut(p.id, "actif") ? ApiResponse.ok() : ApiResponse.error("Échec.");
    }

    // ─── ADMIN : DELETE ──────────────────────────────────────────────────────
    public String deleteuser(AppRequest request) {
        if (!isAdmin(request)) return ApiResponse.unauthorized("Droits admin requis.");
        IdPayload p = JsonUtils.fromJson(request.getPayload(), IdPayload.class);
        if (p == null) return ApiResponse.badRequest("id requis.");
        sessions.entrySet().removeIf(e -> e.getValue().getId() == p.id);
        return repo().delete(p.id) ? ApiResponse.ok() : ApiResponse.error("Échec.");
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
        data.put("token", token);
        data.put("id", u.getId());
        data.put("nom", u.getNom());
        data.put("prenom", u.getPrenom());
        data.put("email", u.getEmail());
        data.put("role", u.getRole());
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