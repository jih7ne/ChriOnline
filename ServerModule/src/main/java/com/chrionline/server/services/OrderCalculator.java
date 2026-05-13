package com.chrionline.server.services;

import com.chrionline.core.config.ServerConfig;
import com.chrionline.shared.models.LigneCommande;
import com.chrionline.shared.models.Produit;
import com.chrionline.server.repositories.ProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *  CALCULATEUR SÉCURISÉ DE PRIX DE COMMANDE
 * 
 * ⚠️ SÉCURITÉ CRITIQUE:
 * Ce service recalcule TOUJOURS les prix depuis la base de données.
 * Les prix envoyés par le client sont COMPLÈTEMENT IGNORÉS.
 * 
 * Cela prévient les fraudes où un attaquant pourrait manipuler les prix
 * en envoyant des valeurs personnalisées.
 * 
 * Flux:
 * 1. Client envoie: { id_produit, quantite, prix_unitaire (IGNORÉ) }
 * 2. Serveur ignore prix_unitaire du client
 * 3. Serveur récupère le prix RÉEL depuis la BDD
 * 4. Serveur recalcule le total = sum(prix_réel × quantite)
 * 5. Ce total recalculé est la SEULE source de vérité
 */
public class OrderCalculator {
    private static final Logger logger = LoggerFactory.getLogger(OrderCalculator.class);
    
    private final ProduitRepository produitRepository;
    
    public OrderCalculator() {
        this.produitRepository = ServerConfig.getRepo(ProduitRepository.class);
    }
    
    /**
     *  CALCUL SÉCURISÉ DU PRIX TOTAL
     * 
     * Pour chaque ligne de commande:
     * 1. Ignore complètement le prix envoyé par le client
     * 2. Récupère le produit depuis la BDD (source de vérité)
     * 3. Récupère le prix RÉEL du produit
     * 4. Calcule: sous-total = prix_réel × quantite
     * 5. Accumule dans le total
     * 
     * ⭐ IMPORTANT:
     * Le prix envoyé par le client est JAMAIS utilisé pour le calcul final.
     * Cela garantit que le prix total est toujours correct.
     * 
     * @param lignes les lignes de commande du client
     * @return le prix total recalculé depuis la BDD, ou -1 en cas d'erreur
     */
    public double calculateTotalPrice(List<LigneCommande> lignes) {
        if (lignes == null || lignes.isEmpty()) {
            logger.warn(" Impossible de calculer le prix: liste de lignes vide");
            return -1;
        }
        
        double totalPrice = 0.0;
        int ligneCount = 0;
        
        for (LigneCommande ligne : lignes) {
            int idProduit = ligne.getId_produit();
            int quantite = ligne.getQuantite();
            
            // ⭐ SÉCURITÉ: Récupère le produit depuis la BDD
            // Cela garantit que le prix utilisé est le VRAI prix du produit
            Produit produit = produitRepository.findById(idProduit);
            
            if (produit == null) {
                logger.warn(" Produit id={} introuvable pour le calcul de prix", idProduit);
                return -1; // Erreur: produit introuvable
            }
            
            // ⭐ SÉCURITÉ: Utilise SEULEMENT le prix depuis la BDD
            // Le prix envoyé par le client (ligne.getPrix_unitaire()) est IGNORÉ
            double prixReel = produit.getPrix();
            double sousTotal = prixReel * quantite;
            
            logger.debug(
                "💰 Produit id={} (nom={}): " +
                "prix_réel={} × quantite={} = {}",
                idProduit, produit.getNom(),
                prixReel, quantite, sousTotal
            );
            
            totalPrice += sousTotal;
            ligneCount++;
        }
        
        logger.info(
            " Calcul de prix sécurisé terminé | " +
            "Lignes: {} | Total: {} | " +
            "Source: BDD uniquement (client ignoré)",
            ligneCount, totalPrice
        );
        
        return totalPrice;
    }
    
    /**
     * Valide que les prix envoyés par le client correspondent aux prix réels.
     * Utilisé pour logging/audit, mais n'affecte PAS le calcul final.
     * 
     * ⭐ NOTE: Cette méthode est juste pour détecter les tentatives de fraude.
     * Elle ne change JAMAIS le prix total recalculé.
     * 
     * @param lignes les lignes de commande du client
     * @return true si tous les prix correspondent, false si détecte une manipulation
     */
    public boolean detectPriceManipulation(List<LigneCommande> lignes) {
        if (lignes == null || lignes.isEmpty()) return true;
        
        boolean manipulationDetected = false;
        
        for (LigneCommande ligne : lignes) {
            Produit produit = produitRepository.findById(ligne.getId_produit());
            if (produit == null) continue;
            
            double prixClient = ligne.getPrix_unitaire();
            double prixReel = produit.getPrix();
            
            // Comparaison avec une tolérance de 0.01€ (arrondi)
            if (Math.abs(prixClient - prixReel) > 0.01) {
                logger.warn(
                    " TENTATIVE DE MANIPULATION DE PRIX DÉTECTÉE! " +
                    "Produit id={} | Prix attendu: {} | Prix envoyé: {} | Différence: {}",
                    ligne.getId_produit(),
                    prixReel,
                    prixClient,
                    (prixClient - prixReel)
                );
                manipulationDetected = true;
            }
        }
        
        return !manipulationDetected;
    }
    
    /**
     * Recalcule les prix unitaires des lignes en fonction de la BDD.
     * Corrige silencieusement les prix du client si nécessaire.
     * 
     * ⭐ NOTE: Les prix sont corrigés IN-PLACE sur les objets LigneCommande.
     * Le client sera facturé avec les bons prix, pas ceux qu'il a envoyés.
     * 
     * @param lignes les lignes de commande à corriger
     * @return le nombre de lignes corrigées
     */
    public int correctPrices(List<LigneCommande> lignes) {
        if (lignes == null || lignes.isEmpty()) return 0;
        
        int corrected = 0;
        
        for (LigneCommande ligne : lignes) {
            Produit produit = produitRepository.findById(ligne.getId_produit());
            if (produit == null) continue;
            
            double prixActuel = ligne.getPrix_unitaire();
            double prixReel = produit.getPrix();
            
            if (prixActuel != prixReel) {
                logger.info(
                    " Correction de prix détectée: Produit id={} | Avant: {} | Après: {}",
                    ligne.getId_produit(), prixActuel, prixReel
                );
                ligne.setPrix_unitaire(prixReel);
                corrected++;
            }
        }
        
        if (corrected > 0) {
            logger.info(" {} ligne(s) ont été corrigées avec les prix réels", corrected);
        }
        
        return corrected;
    }
}
