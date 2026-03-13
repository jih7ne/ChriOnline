package com.chrionline.chrionline.server.services;

import com.chrionline.chrionline.core.enums.StatutCommande;
import com.chrionline.chrionline.server.data.models.Commande;
import com.chrionline.chrionline.server.data.models.LigneCommande;
import com.chrionline.chrionline.server.data.models.Produit;
import com.chrionline.chrionline.server.repositories.CommandeRepository;
import com.chrionline.chrionline.server.repositories.LigneCommandeRepository;
import com.chrionline.chrionline.server.repositories.ProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class CommandeService {
    private static final Logger logger = LoggerFactory.getLogger(CommandeService.class);

    private final CommandeRepository commandeRepository;
    private final LigneCommandeRepository ligneCommandeRepository;
    private final ProduitRepository produitRepository;

    public CommandeService(CommandeRepository commandeRepository,
                           LigneCommandeRepository ligneCommandeRepository,
                           ProduitRepository produitRepository) {
        this.commandeRepository = commandeRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
        this.produitRepository = produitRepository;
        logger.info("CommandeService initialized");
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

        // 2 : calcul du prix total
        double prixTotal = lignes.stream()
                .mapToDouble(l -> l.getPrix_unitaire() * l.getQuantite())
                .sum();

        // 3 : création et insertion de la commande
        Commande commande = new Commande();
        commande.setId_utilisateur(idUtilisateur);
        commande.setId_adresse(idAdresse);
        commande.setDate(LocalDateTime.now());
        commande.setStatut(StatutCommande.EN_ATTENTE);
        commande.setPrix_total(prixTotal);

        commandeRepository.add(commande);
        logger.info("Commande insérée en BDD pour utilisateur id={}", idUtilisateur);

        // 4 : récupérer la commande avec son UUID généré

        Commande commandeInsered = commandeRepository.getCommandes(idUtilisateur)
                .stream()
                .reduce((first, second) -> second)
                .orElse(null);

        if (commandeInsered == null) {
            logger.error("Impossible de retrouver la commande après insertion");
            return null;
        }

        // 5 : insertion des lignes avec l'id_commande
        for (LigneCommande ligne : lignes) {
            ligne.setId_commande(commandeInsered.getId_commande());
        }
        ligneCommandeRepository.addAll(lignes);
        logger.info("Lignes de commande insérées (count={})", lignes.size());

        return commandeInsered;
    }

    // CONFIRMER UNE COMMANDE APRÈS PAIEMENT ACCEPTÉ
    // 1. Décrémente le stock de chaque produit
    // 2. Passe le statut de la commande à CONFIRMEE
    public boolean confirmerPaiement(int idCommande) {
        logger.info("Confirmation paiement pour commande id={}", idCommande);

        List<LigneCommande> lignes = ligneCommandeRepository.getLignesCommande(idCommande);
        if (lignes.isEmpty()) {
            logger.warn("Aucune ligne trouvée pour commande id={}", idCommande);
            return false;
        }

        // Décrémentation du stock
        for (LigneCommande ligne : lignes) {
            Produit produit = produitRepository.findById(ligne.getId_produit());
            if (produit == null) continue;

            int nouveauStock = produit.getStock() - ligne.getQuantite();
            produitRepository.updateStock(ligne.getId_produit(), nouveauStock);
            logger.info("Stock produit id={} mis à jour → {}", ligne.getId_produit(), nouveauStock);
        }

        // Changement de statut
        commandeRepository.updateStatut(idCommande, StatutCommande.CONFIRMEE);
        logger.info("Statut commande id={} → CONFIRMEE", idCommande);

        return true;
    }

    // ANNULER UNE COMMANDE
    public void annulerCommande(int idCommande) {
        logger.info("Annulation commande id={}", idCommande);
        commandeRepository.updateStatut(idCommande, StatutCommande.ANNULEE);
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
}