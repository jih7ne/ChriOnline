package com.chrionline.server.services;

import com.chrionline.core.config.ServerConfig;
import com.chrionline.core.enums.StatutCommande;
import com.chrionline.shared.models.Commande;
import com.chrionline.shared.models.LigneCommande;
import com.chrionline.shared.models.Produit;
import com.chrionline.server.repositories.CommandeRepository;
import com.chrionline.server.repositories.LigneCommandeRepository;
import com.chrionline.server.repositories.ProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class CommandeService {
    private static final Logger logger = LoggerFactory.getLogger(CommandeService.class);

    private final CommandeRepository commandeRepository;
    private final LigneCommandeRepository ligneCommandeRepository;
    private final ProduitRepository produitRepository;
    private final PanierService panierService;
    private final OrderCalculator orderCalculator;
    private final StockReservationService stockReservationService;

    public CommandeService(CommandeRepository commandeRepository,
                           LigneCommandeRepository ligneCommandeRepository,
                           ProduitRepository produitRepository) {
        this.commandeRepository = commandeRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
        this.produitRepository = produitRepository;
        this.panierService = ServerConfig.getService(PanierService.class);
        this.orderCalculator = new OrderCalculator();
        this.stockReservationService = new StockReservationService(produitRepository);
        logger.info("CommandeService initialized with secure OrderCalculator and Pessimistic Stock Reservation");
    }

    private NotificationService getNotificationService() {
        return ServerConfig.getService(NotificationService.class);
    }

    // VALIDER UNE COMMANDE
    // 1. Vérifie que le stock est suffisant pour chaque ligne
    // 2. Insère la commande en BDD
    // 3. Insère toutes les lignes de commande
    public Commande validerCommande(int idUtilisateur, int idAdresse, List<LigneCommande> lignes) {
        logger.info("Validation commande pour utilisateur id={}", idUtilisateur);

        // 1 : vérification du stock pour chaque ligne
        for (LigneCommande ligne : lignes) {
            Produit produit = produitRepository.findById(ligne.getId_produit());
            if (produit == null) {
                logger.warn("Produit id={} introuvable", ligne.getId_produit());
                return null;
            }
            if (produit.getStock() < ligne.getQuantite()) {
                logger.warn("Stock insuffisant pour produit id={} (dispo={}, demandé={})",
                        produit.getId(), produit.getStock(), ligne.getQuantite());
                return null;
            }
        }

        // SÉCURITÉ: Détecte si le client a tenté de manipuler les prix
        // Les prix envoyés par le client peuvent être ignorés/manipulés
        boolean manipulationDetected = !orderCalculator.detectPriceManipulation(lignes);
        if (manipulationDetected) {
            logger.warn(" Une tentative de manipulation de prix a été détectée et ignorée");
        }

        // SÉCURITÉ: Recalcule le prix total UNIQUEMENT depuis la BDD
        // Les prix envoyés par le client sont COMPLÈTEMENT IGNORÉS
        // Cela garantit que le serveur facture TOUJOURS le bon prix
        double prixTotal = orderCalculator.calculateTotalPrice(lignes);
        if (prixTotal < 0) {
            logger.error("Impossible de recalculer le prix total depuis la BDD");
            return null;
        }

        // Correction optionnelle: actualise les prix dans les lignes avec les vrais prix
        int corrected = orderCalculator.correctPrices(lignes);
        logger.info(" {} ligne(s) ont été corrigées avec les prix réels de la BDD", corrected);

        // 3 : création et insertion de la commande
        Commande commande = new Commande();
        commande.setId_utilisateur(idUtilisateur);
        commande.setId_adresse(idAdresse);
        commande.setDate(LocalDateTime.now());
        commande.setStatut(StatutCommande.EN_ATTENTE);
        commande.setPrix_total(prixTotal);

        commandeRepository.add(commande);

        if (commande.getId_commande() == 0 || commande.getUuid_commande() == null) {
            logger.error("Impossible de récupérer l'ID ou l'UUID de la commande générée");
            return null;
        }

        logger.info("Commande insérée en BDD pour utilisateur id={} avec uuid={}", idUtilisateur, commande.getUuid_commande());

        // 4 : insertion des lignes avec l'id_commande
        for (LigneCommande ligne : lignes) {
            ligne.setId_commande(commande.getId_commande());
        }
        ligneCommandeRepository.addAll(lignes);
        logger.info("Lignes de commande insérées (count={})", lignes.size());

        // Vider le panier dès que la commande est en attente
        panierService.viderPanier(idUtilisateur);
        logger.info("Panier de l'utilisateur id={} vidé avec succès suite à la mise en attente", idUtilisateur);

        return commande;
    }

    // CONFIRMER UNE COMMANDE APRÈS PAIEMENT ACCEPTÉ
    // 1. Décrémente le stock de chaque produit
    // 2. Passe le statut de la commande à VALIDEE
    // 3. Envoie UDP : confirmation de commande (scénario 1) + mise à jour stock (scénario 3)
    public boolean confirmerPaiement(int idCommande) {
        logger.info("Confirmation paiement pour commande id={}", idCommande);

        Commande commande = commandeRepository.getCommandeById(idCommande);
        List<LigneCommande> lignes = ligneCommandeRepository.getLignesCommande(idCommande);
        if (lignes.isEmpty()) {
            logger.warn("Aucune ligne trouvée pour commande id={}", idCommande);
            // ── UDP : Scénario 2 (paiement refusé / commande introuvable)
            if (commande != null) {
                NotificationService ns = getNotificationService();
                if (ns != null) ns.notifyPaymentFailed(commande.getId_utilisateur());
            }
            return false;
        }

        // Décrémentation du stock AVEC Verrouillage Pessimiste
        for (LigneCommande ligne : lignes) {
            boolean reserved = stockReservationService.reserveStock(ligne.getId_produit(), ligne.getQuantite());
            
            if (reserved) {
                Produit produit = produitRepository.findById(ligne.getId_produit());
                if (produit != null) {
                    // ── UDP : Scénario 3 — broadcast mise à jour stock
                    NotificationService ns = getNotificationService();
                    if (ns != null) ns.notifyStockUpdated(produit.getNom(), produit.getStock());
                }
            } else {
                logger.error("ALERTE CRITIQUE : Impossible de décrémenter le stock pour le produit ID={} (quantité: {}). Une survente a été évitée grâce au verrou !", ligne.getId_produit(), ligne.getQuantite());
            }
        }

        // Changement de statut
        commandeRepository.updateStatut(idCommande, StatutCommande.VALIDEE);
        logger.info("Statut commande id={} → VALIDEE", idCommande);

        // ── UDP : Scénario 1 — confirmation au client
        if (commande != null) {
            NotificationService ns = getNotificationService();
            if (ns != null) {
                ns.notifyOrderConfirmed(
                        commande.getId_utilisateur(),
                        commande.getUuid_commande(),
                        commande.getPrix_total()
                );
            }
        }

        return true;
    }

    // ANNULER UNE COMMANDE
    // + UDP Scénario 5 : confirme l'annulation au client
    public boolean annulerCommande(int idCommande, int idUtilisateur) {
        logger.info("Annulation commande id={} pour utilisateur id={}", idCommande, idUtilisateur);

        Commande commande = commandeRepository.getCommandeByIdAndUser(idCommande, idUtilisateur);
        if (commande == null) {
            logger.warn("Commande id={} introuvable ou n'appartient pas à l'utilisateur {}", idCommande, idUtilisateur);
            return false;
        }

        // Seules les commandes EN_ATTENTE peuvent être annulées par le client
        if (commande.getStatut() != StatutCommande.EN_ATTENTE) {
            logger.warn("Commande id={} ne peut pas être annulée (statut={})", idCommande, commande.getStatut());
            return false;
        }

        commandeRepository.updateStatut(idCommande, StatutCommande.ANNULEE);
        logger.info("Commande id={} annulée manuellement.", idCommande);

        // ── UDP : Scénario 5 — confirmation annulation au client
        NotificationService ns = getNotificationService();
        if (ns != null) ns.notifyOrderCancelledByClient(commande.getId_utilisateur(), commande.getUuid_commande());

        return true;
    }

    // CHANGER LE STATUT D'UNE COMMANDE (admin)
    // + UDP Scénario 4 : notifie le client concerné
    public boolean changerStatutCommande(int idCommande, StatutCommande newStatut) {
        logger.info("Changement statut commande id={} → {}", idCommande, newStatut);

        Commande commande = commandeRepository.getCommandeById(idCommande);
        if (commande == null) {
            logger.warn("Commande id={} introuvable", idCommande);
            return false;
        }

        StatutCommande currentStatut = commande.getStatut();
        
        //  MACHINE À ÉTATS: Vérifier si la transition est autorisée
        if (!OrderStateMachine.isValidTransition(currentStatut, newStatut)) {
            logger.warn("⚠️ Transition illégale refusée: {} → {} (Commande id={})", 
                        currentStatut, newStatut, idCommande);
            return false;
        }

        commandeRepository.updateStatut(idCommande, newStatut);
        logger.info("Statut commande id={} → {} (admin)", idCommande, newStatut);

        // ── UDP : Scénario 4 — notification au client concerné
        NotificationService ns = getNotificationService();
        if (ns != null) {
            ns.notifyOrderStatusChanged(
                    commande.getId_utilisateur(),
                    commande.getUuid_commande(),
                    newStatut.name()
            );
        }

        return true;
    }

    // HISTORIQUE DES COMMANDES D'UN UTILISATEUR
    public List<Commande> getHistoriqueCommandes(int idUtilisateur) {
        logger.info("Récupération historique commandes utilisateur id={}", idUtilisateur);
        return commandeRepository.getCommandes(idUtilisateur);
    }

    public List<LigneCommande> getLignesCommande(int idCommande) {
        logger.info("Récupération des lignes de la commande id={}", idCommande);
        List<LigneCommande> lignes = ligneCommandeRepository.getLignesCommande(idCommande);
        if (lignes.isEmpty()) {
            logger.warn("Aucune ligne trouvée pour commande id={}", idCommande);
        }
        return lignes;
    }

    // LIGNES AVEC NOM DU PRODUIT (jointure)
    public List<java.util.Map<String, Object>> getLignesAvecNom(int idCommande) {
        logger.info("Récupération des lignes avec nom produit pour commande id={}", idCommande);
        return ligneCommandeRepository.getLignesAvecNom(idCommande);
    }
}
