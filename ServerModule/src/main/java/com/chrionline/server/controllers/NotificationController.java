package com.chrionline.server.controllers;

import com.chrionline.core.annotations.RequiresRole;
import com.chrionline.core.enums.UserRole;
import com.chrionline.core.interfaces.IController;
import com.chrionline.network.enums.NotificationType;
import com.chrionline.network.enums.Severity;
import com.chrionline.network.protocol.AppNotification;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.core.network.protocol.AppResponse;
import com.chrionline.network.udp.UDPNotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * Handles admin-triggered UDP notifications.
 * All method names are lowercase — required by RequestDispatcher reflection.
 * 
 * Sécurité: Tous les endpoints sont protégés par @RequiresRole.
 */
public class NotificationController implements IController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    private static UDPNotificationDispatcher dispatcher;

    public static void setDispatcher(UDPNotificationDispatcher d) {
        dispatcher = d;
        logger.info("NotificationController: UDP dispatcher injected");
    }

    // ─── SEND TO SPECIFIC USER ────────────────────────────────────────────────
    // action: "sendnotification"
    // INPUT : { type, title, message, source?, idUtilisateur }
    @RequiresRole(value = {UserRole.ADMIN, UserRole.SUPPORT}, description = "Seuls les administrateurs et support peuvent envoyer des notifications ciblées")
    public String sendnotification(AppRequest request) {
        try {
            if (dispatcher == null)
                return AppResponse.error("Service de notifications non disponible.");

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = request.getPayloadAs(Map.class);
            if (payload == null)
                return AppResponse.badRequest("Payload requis.");

            String title      = str(payload.get("title"));
            String message    = str(payload.get("message"));
            String source     = str(payload.getOrDefault("source", "admin"));
            String typeStr    = str(payload.getOrDefault("type", "INFO"));
            Object idObj      = payload.get("idUtilisateur");

            if (title.isEmpty() || message.isEmpty())
                return AppResponse.badRequest("title et message sont requis.");

            if (idObj == null)
                return AppResponse.badRequest("idUtilisateur est requis pour sendnotification.");

            int idUtilisateur = ((Number) idObj).intValue();

            AppNotification notification = new AppNotification.Builder()
                    .id(UUID.randomUUID().toString())
                    .type(parseType(typeStr))
                    .severity(parseSeverity(typeStr))
                    // Format: "title | message" — client splits on " | " to display both
                    .message(title + " | " + message)
                    .source(source)
                    .timestamp(System.currentTimeMillis())
                    .build();

            boolean sent = dispatcher.sendToUser(idUtilisateur, notification);

            if (sent) {
                logger.info("Notification envoyée à user {} : [{}] {}", idUtilisateur, typeStr, title);
                return AppResponse.success(null, "Notification envoyée.");
            } else {
                logger.warn("User {} non connecté via UDP — notification non délivrée", idUtilisateur);
                return AppResponse.success(null,
                        "Notification non délivrée : l'utilisateur n'est pas connecté.");
            }

        } catch (Exception e) {
            logger.error("Erreur sendnotification", e);
            return AppResponse.error("Erreur lors de l'envoi de la notification.");
        }
    }

    // ─── BROADCAST TO ALL ─────────────────────────────────────────────────────
    // action: "broadcastnotification"
    // INPUT : { type, title, message, source? }
    @RequiresRole(value = UserRole.ADMIN, description = "Seuls les administrateurs peuvent envoyer des broadcasts à tous les utilisateurs")
    public String broadcastnotification(AppRequest request) {
        try {
            if (dispatcher == null)
                return AppResponse.error("Service de notifications non disponible.");

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = request.getPayloadAs(Map.class);
            if (payload == null)
                return AppResponse.badRequest("Payload requis.");

            String title   = str(payload.get("title"));
            String message = str(payload.get("message"));
            String source  = str(payload.getOrDefault("source", "admin"));
            String typeStr = str(payload.getOrDefault("type", "INFO"));

            if (title.isEmpty() || message.isEmpty())
                return AppResponse.badRequest("title et message sont requis.");

            AppNotification notification = new AppNotification.Builder()
                    .id(UUID.randomUUID().toString())
                    .type(parseType(typeStr))
                    .severity(parseSeverity(typeStr))
                    .message(title + " | " + message)
                    .source(source)
                    .timestamp(System.currentTimeMillis())
                    .build();

            dispatcher.broadcastNotification(notification);

            int count = dispatcher.getConnectedClientCount();
            logger.info("Broadcast envoyé à {} client(s) : [{}] {}", count, typeStr, title);
            return AppResponse.success(null, "Broadcast envoyé à " + count + " client(s).");

        } catch (Exception e) {
            logger.error("Erreur broadcastnotification", e);
            return AppResponse.error("Erreur lors du broadcast.");
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private NotificationType parseType(String s) {
        return switch (s.toUpperCase()) {
            case "ERROR"   -> NotificationType.ERROR;
            default        -> NotificationType.INFO;
        };
    }

    private Severity parseSeverity(String s) {
        return switch (s.toUpperCase()) {
            case "WARNING"  -> Severity.WARNING;
            case "ERROR"    -> Severity.ERROR;
            case "CRITICAL" -> Severity.CRITICAL;
            default         -> Severity.LOW;
        };
    }

    private String str(Object o) { return o == null ? "" : String.valueOf(o); }
}
