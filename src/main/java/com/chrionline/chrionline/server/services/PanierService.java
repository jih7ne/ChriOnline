package com.chrionline.chrionline.server.services;

import com.chrionline.chrionline.server.data.models.Panier;
import com.chrionline.chrionline.server.data.models.PanierProduit;
import com.chrionline.chrionline.server.repositories.PanierRepository;
import com.chrionline.chrionline.server.repositories.ProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PanierService {

    private static final Logger logger = LoggerFactory.getLogger(PanierService.class);
    private final PanierRepository panierRepository;
    private final ProduitRepository produitRepository;

    public PanierService(PanierRepository panierRepository, ProduitRepository produitRepository) {
        this.panierRepository = panierRepository;
        this.produitRepository = produitRepository;
        logger.info("PanierService initialized");
    }

    //  OBTENIR LE PANIER
    public Panier getPanier(int idUtilisateur) {
        logger.info("Récupération du panier de l'utilisateur id={}", idUtilisateur);
        Panier panier = panierRepository.findByUtilisateur(idUtilisateur);
        if (panier == null) {
            // créer un panier si l'utilisateur n'en a pas
            logger.info("Aucun panier trouvé, création d'un nouveau panier");
            int idPanier = panierRepository.creerPanier(idUtilisateur);
            panier = panierRepository.findByUtilisateur(idUtilisateur);
        }
        return panier;
    }

    //  AJOUTER UN PRODUIT
    public boolean ajouterProduit(int idUtilisateur, int idProduit, int quantite) {
        logger.info("Ajout produit id={} quantite={} au panier utilisateur id={}",
                idProduit, quantite, idUtilisateur);
        // vérifier stock disponible
        if (produitRepository.findById(idProduit) == null) {
            logger.warn("Produit id={} non trouvé", idProduit);
            return false;
        }
        if (produitRepository.findById(idProduit).getStock() < quantite) {
            logger.warn("Stock insuffisant pour produit id={}", idProduit);
            return false;
        }
        Panier panier = getPanier(idUtilisateur);
        panierRepository.ajouterProduit(panier.getId(), idProduit, quantite);
        return true;
    }

    //  SUPPRIMER UN PRODUIT
    public void supprimerProduit(int idUtilisateur, int idProduit) {
        logger.info("Suppression produit id={} du panier utilisateur id={}",
                idProduit, idUtilisateur);
        Panier panier = getPanier(idUtilisateur);
        panierRepository.supprimerProduit(panier.getId(), idProduit);
    }

    //  MODIFIER QUANTITE
    public boolean modifierQuantite(int idUtilisateur, int idProduit, int nouvelleQuantite) {
        logger.info("Modification quantité produit id={} à {} pour utilisateur id={}",
                idProduit, nouvelleQuantite, idUtilisateur);
        if (nouvelleQuantite <= 0) {
            // si quantité = 0 on supprime le produit
            supprimerProduit(idUtilisateur, idProduit);
            return true;
        }
        if (produitRepository.findById(idProduit).getStock() < nouvelleQuantite) {
            logger.warn("Stock insuffisant pour produit id={}", idProduit);
            return false;
        }
        Panier panier = getPanier(idUtilisateur);
        panierRepository.modifierQuantite(panier.getId(), idProduit, nouvelleQuantite);
        return true;
    }

    //  CALCUL TOTAL
    public double calculerTotal(int idUtilisateur) {
        Panier panier = getPanier(idUtilisateur);
        double total = panier.getTotal();
        logger.info("Total panier utilisateur id={} : {}",  idUtilisateur, total);
        return total;
    }

    //  VIDER LE PANIER
    public void viderPanier(int idUtilisateur) {
        logger.info("Vidage du panier utilisateur id={}", idUtilisateur);
        Panier panier = getPanier(idUtilisateur);
        panierRepository.viderPanier(panier.getId());
    }
}