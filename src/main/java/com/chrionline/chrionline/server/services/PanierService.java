package com.chrionline.chrionline.server.services;

import com.chrionline.chrionline.server.data.models.Panier;
import com.chrionline.chrionline.server.data.models.Utilisateur;
import com.chrionline.chrionline.server.repositories.PanierRepository;
import com.chrionline.chrionline.server.repositories.ProduitRepository;
import com.chrionline.chrionline.server.repositories.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PanierService {

    private static final Logger logger = LoggerFactory.getLogger(PanierService.class);
    private final PanierRepository panierRepository;
    private final ProduitRepository produitRepository;
    private final UtilisateurRepository utilisateurRepository;

    public PanierService(PanierRepository panierRepository,
                         ProduitRepository produitRepository,
                         UtilisateurRepository utilisateurRepository) {
        this.panierRepository = panierRepository;
        this.produitRepository = produitRepository;
        this.utilisateurRepository = utilisateurRepository;
        logger.info("PanierService initialized");
    }

    /**
     * Vérifie que l'utilisateur existe et possède le rôle "client".
     * Lève une exception si ce n'est pas le cas.
     */
    private void verifierRoleClient(int idUtilisateur) {
        Utilisateur utilisateur = utilisateurRepository.getById(idUtilisateur);
        if (utilisateur == null) {
            throw new IllegalArgumentException("Utilisateur introuvable id=" + idUtilisateur);
        }
        if (!"client".equalsIgnoreCase(utilisateur.getRole())) {
            throw new SecurityException("Accès refusé : seuls les clients peuvent avoir un panier");
        }
    }

    public Panier getPanier(int idUtilisateur) {
        verifierRoleClient(idUtilisateur);
        logger.info("Récupération du panier de l'utilisateur id={}", idUtilisateur);
        Panier panier = panierRepository.findByUtilisateur(idUtilisateur);
        if (panier == null) {
            logger.info("Aucun panier trouvé, création d'un nouveau panier");
            panierRepository.creerPanier(idUtilisateur);
            panier = panierRepository.findByUtilisateur(idUtilisateur);
        }
        return panier;
    }

    public boolean ajouterProduit(int idUtilisateur, int idProduit, int quantite) {
        logger.info("Ajout produit id={} quantite={} au panier utilisateur id={}",
                idProduit, quantite, idUtilisateur);
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

    public void supprimerProduit(int idUtilisateur, int idProduit) {
        logger.info("Suppression produit id={} du panier utilisateur id={}",
                idProduit, idUtilisateur);
        Panier panier = getPanier(idUtilisateur);
        panierRepository.supprimerProduit(panier.getId(), idProduit);
    }

    public boolean modifierQuantite(int idUtilisateur, int idProduit, int nouvelleQuantite) {
        logger.info("Modification quantité produit id={} à {} pour utilisateur id={}",
                idProduit, nouvelleQuantite, idUtilisateur);
        if (nouvelleQuantite <= 0) {
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

    public double calculerTotal(int idUtilisateur) {
        Panier panier = getPanier(idUtilisateur);
        double total = panier.getTotal();
        logger.info("Total panier utilisateur id={} : {}", idUtilisateur, total);
        return total;
    }

    public void viderPanier(int idUtilisateur) {
        logger.info("Vidage du panier utilisateur id={}", idUtilisateur);
        Panier panier = getPanier(idUtilisateur);
        panierRepository.viderPanier(panier.getId());
    }
}