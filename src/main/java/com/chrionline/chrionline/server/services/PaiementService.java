package com.chrionline.chrionline.server.services;

import com.chrionline.chrionline.core.enums.MethodePaiement;
import com.chrionline.chrionline.core.enums.StatutPaiement;
import com.chrionline.chrionline.server.data.models.Paiement;
import com.chrionline.chrionline.server.repositories.PaiementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class PaiementService {
    private static final Logger logger = LoggerFactory.getLogger(PaiementService.class);

    private final PaiementRepository paiementRepository;
    private final CommandeService commandeService;

    public PaiementService(PaiementRepository paiementRepository,
                           CommandeService commandeService) {
        this.paiementRepository = paiementRepository;
        this.commandeService = commandeService;
        logger.info("PaiementService initialized");
    }

    // VALIDER LE FORMAT DES DONNÉES DE PAIEMENT
    // Retourne null si valide, sinon un message d'erreur
    public String validerPaiement(String numeroCarte, String cvv, String dateExpiration) {

        // Vérif numéro de carte (16 chiffres)
        if (numeroCarte == null || !numeroCarte.matches("\\d{16}")) {
            logger.warn("Numéro de carte invalide");
            return "Numéro de carte invalide (16 chiffres requis)";
        }

        // Vérif CVV (3 chiffres)
        if (cvv == null || !cvv.matches("\\d{3}")) {
            logger.warn("CVV invalide");
            return "CVV invalide (3 chiffres requis)";
        }

        // Vérif date d'expiration : format MM/YY et non expirée
        if (dateExpiration == null || !dateExpiration.matches("\\d{2}/\\d{2}")) {
            logger.warn("Format date expiration invalide");
            return "Date d'expiration invalide (format MM/YY requis)";
        }
        try {
            String[] parts = dateExpiration.split("/");
            int mois = Integer.parseInt(parts[0]);
            int annee = 2000 + Integer.parseInt(parts[1]);
            YearMonth expiration = YearMonth.of(annee, mois);
            if (expiration.isBefore(YearMonth.now())) {
                logger.warn("Carte expirée : {}", dateExpiration);
                return "Carte expirée";
            }
        } catch (Exception e) {
            return "Date d'expiration invalide";
        }

        // Logique fictive : refus si carte finit par "0000" ou CVV = "000"
        if (numeroCarte.endsWith("0000")) {
            logger.warn("Carte refusée (finit par 0000)");
            return "Paiement refusé par la banque";
        }
        if (cvv.equals("000")) {
            logger.warn("Carte refusée (CVV = 000)");
            return "Paiement refusé (CVV invalide)";
        }

        logger.info("Données de paiement valides");
        return null;
    }

    // ENREGISTRER UN PAIEMENT ET DÉCLENCHER LA CONFIRMATION DE COMMANDE
    public boolean enregistrerPaiement(int idCommande, String numeroCarte,
                                       String cvv, String dateExpiration,
                                       MethodePaiement methode) {

        logger.info("Traitement paiement pour commande id={}", idCommande);

        // 1 : validation du format
        String erreur = validerPaiement(numeroCarte, cvv, dateExpiration);

        StatutPaiement statut = (erreur == null)
                ? StatutPaiement.CONFIRME
                : StatutPaiement.REFUSE;

        // 2 : masquer le numéro de carte (garder seulement les 4 derniers chiffres pour raison de sécurité)
        String numeroMasque = numeroCarte != null && numeroCarte.length() == 16
                ? numeroCarte.substring(12)
                : null;

        // 3 : insertion du paiement en BDD
        Paiement paiement = new Paiement();
        paiement.setId_commande(idCommande);
        paiement.setDate_paiement(LocalDateTime.now());
        paiement.setMethode_paiement(methode);
        paiement.setStatut(statut);
        paiement.setNumero_masque(numeroMasque);

        paiementRepository.add(paiement);
        logger.info("Paiement enregistré avec statut={}", statut);

        // 4 : si paiement accepté → confirmer la commande
        if (statut == StatutPaiement.CONFIRME) {
            boolean confirme = commandeService.confirmerPaiement(idCommande);
            if (!confirme) {
                logger.error("Échec confirmation commande id={} après paiement accepté", idCommande);
                return false;
            }
            logger.info("Commande id={} confirmée avec succès", idCommande);
            return true;
        }

        logger.warn("Paiement refusé pour commande id={} : {}", idCommande, erreur);
        return false;
    }

    // RÉCUPÉRER LE PAIEMENT D'UNE COMMANDE
    public Paiement getPaiementByCommande(int idCommande) {
        return paiementRepository.getPaiementByCommande(idCommande);
    }
}