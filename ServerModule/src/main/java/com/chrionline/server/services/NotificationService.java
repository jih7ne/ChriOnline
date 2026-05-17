package com.chrionline.server.services;

import com.chrionline.network.enums.NotificationType;
import com.chrionline.network.enums.Severity;
import com.chrionline.network.protocol.AppNotification;
import com.chrionline.network.udp.UDPNotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final UDPNotificationDispatcher dispatcher;

    public NotificationService(UDPNotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        logger.info("NotificationService initialized");
    }

    // ─── SCENARIO 1 : Confirmation de commande ────────────────────────────────
    // Envoyé à l'utilisateur quand le paiement est accepté
    public void notifyOrderConfirmed(int userId, String uuid, double total) {
        String shortUuid = uuid != null && uuid.length() > 8 ? uuid.substring(0, 8).toUpperCase() : uuid;
        AppNotification notif = new AppNotification.Builder()
                .type(NotificationType.ORDER_CONFIRMED)
                .severity(Severity.LOW)
                .message("✅ Commande #" + shortUuid + " confirmée ! Total : " + String.format("%.2f", total) + " DH")
                .source("CommandeService")
                .timestamp(System.currentTimeMillis())
                .build();

        boolean sent = dispatcher.sendToUser(userId, notif);
        logger.info("notifyOrderConfirmed → user={} sent={}", userId, sent);
    }

    // ─── SCENARIO 2 : Refus de paiement ──────────────────────────────────────
    // Envoyé à l'utilisateur quand ses infos de paiement sont incorrectes
    public void notifyPaymentFailed(int userId) {
        AppNotification notif = new AppNotification.Builder()
                .type(NotificationType.PAYMENT_FAILED)
                .severity(Severity.HIGH)
                .message("❌ Paiement refusé. Vérifiez vos informations.")
                .source("PaiementService")
                .timestamp(System.currentTimeMillis())
                .build();

        boolean sent = dispatcher.sendToUser(userId, notif);
        logger.info("notifyPaymentFailed → user={} sent={}", userId, sent);
    }

    // ─── SCENARIO 3 : Mise à jour du stock ───────────────────────────────────
    // Broadcast à tous les clients connectés après décrémentation du stock
    public void notifyStockUpdated(String produitNom, int newStock) {
        AppNotification notif = new AppNotification.Builder()
                .type(NotificationType.STOCK_UPDATE)
                .severity(newStock <= 5 ? Severity.HIGH : Severity.LOW)
                .message("📦 Stock mis à jour : " + produitNom + " — " + newStock + " restant(s)")
                .source("CommandeService")
                .timestamp(System.currentTimeMillis())
                .build();

        dispatcher.broadcastNotification(notif);
        logger.info("notifyStockUpdated → produit='{}' newStock={} broadcast", produitNom, newStock);
    }

    // ─── SCENARIO 4 : Changement de statut (côté admin) ──────────────────────
    // Envoyé au client concerné quand l'admin modifie le statut de sa commande
    public void notifyOrderStatusChanged(int userId, String uuid, String newStatut) {
        String shortUuid = uuid != null && uuid.length() > 8 ? uuid.substring(0, 8).toUpperCase() : uuid;
        String emoji;
        String statutFr;
        NotificationType type;
        Severity severity;

        switch (newStatut.toLowerCase()) {
            case "validee" -> {
                emoji = "✅"; statutFr = "validée";
                type = NotificationType.ORDER_CONFIRMED; severity = Severity.LOW;
            }
            case "expediee" -> {
                emoji = "🚚"; statutFr = "expédiée";
                type = NotificationType.ORDER_SHIPPED; severity = Severity.LOW;
            }
            case "livree" -> {
                emoji = "📦"; statutFr = "livrée";
                type = NotificationType.ORDER_DELIVERED; severity = Severity.LOW;
            }
            case "annulee" -> {
                emoji = "📋"; statutFr = "annulée";
                type = NotificationType.ORDER_CANCELLED; severity = Severity.WARNING;
            }
            default -> {
                emoji = "🔄"; statutFr = newStatut;
                type = NotificationType.INFO; severity = Severity.LOW;
            }
        }

        AppNotification notif = new AppNotification.Builder()
                .type(type)
                .severity(severity)
                .message(emoji + " Votre commande #" + shortUuid + " a été " + statutFr + ".")
                .source("AdminCommandeController")
                .timestamp(System.currentTimeMillis())
                .build();

        boolean sent = dispatcher.sendToUser(userId, notif);
        logger.info("notifyOrderStatusChanged → user={} statut={} sent={}", userId, newStatut, sent);
    }

    // ─── SCENARIO 5 : Annulation par le client ────────────────────────────────
    // Confirmation d'annulation envoyée au client après qu'il ait annulé
    public void notifyOrderCancelledByClient(int userId, String uuid) {
        String shortUuid = uuid != null && uuid.length() > 8 ? uuid.substring(0, 8).toUpperCase() : uuid;
        AppNotification notif = new AppNotification.Builder()
                .type(NotificationType.ORDER_CANCELLED)
                .severity(Severity.WARNING)
                .message("🗑️ Commande #" + shortUuid + " annulée avec succès.")
                .source("CommandeService")
                .timestamp(System.currentTimeMillis())
                .build();

        boolean sent = dispatcher.sendToUser(userId, notif);
        logger.info("notifyOrderCancelledByClient → user={} sent={}", userId, sent);
    }
}
