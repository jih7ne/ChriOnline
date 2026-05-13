package com.chrionline.server.services;
 
import com.chrionline.core.config.ServerConfig;
import com.chrionline.server.repositories.ProduitRepository;
import com.chrionline.shared.models.Produit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import java.sql.Connection;
import java.sql.SQLException;
 
/**
 * Service de gestion des stocks avec verrouillage pessimiste.
 * Prévient les race conditions lors de commandes simultanées (surventes).
 */
public class StockReservationService {
    private static final Logger logger = LoggerFactory.getLogger(StockReservationService.class);
    private final ProduitRepository produitRepository;
 
    public StockReservationService(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
    }
 
    /**
     * Réserve une quantité de produit en utilisant le Pessimistic Locking.
     * 
     * Processus:
     * 1. Démarre une transaction
     * 2. Pose un verrou PESSIMISTIC_WRITE (FOR UPDATE) sur la ligne du produit
     * 3. Vérifie le stock actuel sous le verrou
     * 4. Décrémente le stock si suffisant
     * 5. Commit la transaction (libère le verrou)
     * 
     * @param produitId ID du produit
     * @param quantite Quantité à réserver
     * @return true si réservé avec succès, false si stock insuffisant ou erreur
     */
    public boolean reserveStock(int produitId, int quantite) {
        if (quantite <= 0) return false;
        
        Connection conn = null;
        boolean originalAutoCommit = true;
 
        try {
            conn = ServerConfig.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            
            // ⭐ DÉBUT TRANSACTION
            conn.setAutoCommit(false);
 
            try {
                // 1. SELECT ... FOR UPDATE (Verrouillage pessimiste)
                // Cette ligne bloque les autres threads tentant de modifier ce produit
                Produit produit = produitRepository.findByIdForUpdate(produitId);
 
                if (produit == null) {
                    logger.warn("Réservation échouée: Produit {} introuvable", produitId);
                    conn.rollback();
                    return false;
                }
 
                // 2. Vérification critique du stock sous verrou
                if (produit.getStock() < quantite) {
                    logger.warn("Survente évitée ! Stock insuffisant pour {}: dispo={}, demandé={}", 
                            produit.getNom(), produit.getStock(), quantite);
                    conn.rollback();
                    return false;
                }
 
                // 3. Mise à jour atomique
                int nouveauStock = produit.getStock() - quantite;
                produitRepository.updateStock(produitId, nouveauStock);
 
                // ⭐ COMMIT (Libère le verrou)
                conn.commit();
                
                logger.info("Stock réservé avec succès [Lock Persisted]: {} (-{})", 
                        produit.getNom(), quantite);
                return true;
 
            } catch (Exception e) {
                logger.error("Erreur lors de la transaction de stock, rollback...", e);
                if (conn != null) conn.rollback();
                return false;
            } finally {
                // Restaurer l'état initial de la connexion
                if (conn != null) conn.setAutoCommit(originalAutoCommit);
            }
 
        } catch (SQLException e) {
            logger.error("Erreur SQL lors de la réservation de stock", e);
            return false;
        }
    }
}
