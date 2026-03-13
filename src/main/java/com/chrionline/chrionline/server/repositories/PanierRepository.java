package com.chrionline.chrionline.server.repositories;

import com.chrionline.chrionline.server.data.models.Panier;
import com.chrionline.chrionline.server.data.models.PanierProduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PanierRepository {

    private static final Logger logger = LoggerFactory.getLogger(PanierRepository.class);
    private final Connection connection;

    public PanierRepository(Connection connection) {
        this.connection = connection;
    }

    //  CRÉER UN PANIER
    public int creerPanier(int idUtilisateur) {
        String sql = "INSERT INTO Panier (id_utilisateur) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, idUtilisateur);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1); // retourne l'id du panier créé
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la création du panier", e);
        }
        return -1;
    }

    //  TROUVER LE PANIER D'UN UTILISATEUR
    public Panier findByUtilisateur(int idUtilisateur) {
        String sql = "SELECT * FROM Panier WHERE id_utilisateur = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idUtilisateur);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Panier panier = new Panier();
                panier.setId(rs.getInt("id"));
                panier.setIdUtilisateur(rs.getInt("id_utilisateur"));
                panier.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
                panier.setProduits(getProduitsDuPanier(panier.getId()));
                return panier;
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération du panier", e);
        }
        return null;
    }

    //  PRODUITS DU PANIER
    public List<PanierProduit> getProduitsDuPanier(int idPanier) {
        String sql = "SELECT pp.*, p.nom AS nom_produit, p.prix, p.url_image " +
                "FROM Produit_Panier pp " +
                "JOIN Produit p ON pp.id_produit = p.id " +
                "WHERE pp.id_panier = ?";
        List<PanierProduit> produits = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idPanier);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                PanierProduit pp = new PanierProduit();
                pp.setIdPanier(rs.getInt("id_panier"));
                pp.setIdProduit(rs.getInt("id_produit"));
                pp.setQuantite(rs.getInt("quantite"));
                pp.setNomProduit(rs.getString("nom_produit"));
                pp.setPrix(rs.getDouble("prix"));
                pp.setUrlImage(rs.getString("url_image"));
                produits.add(pp);
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de la récupération des produits du panier", e);
        }
        return produits;
    }

    //  AJOUTER UN PRODUIT AU PANIER
    public void ajouterProduit(int idPanier, int idProduit, int quantite) {
        // si le produit existe déjà on met à jour la quantité
        String sqlCheck = "SELECT quantite FROM Produit_Panier WHERE id_panier=? AND id_produit=?";
        try (PreparedStatement stmt = connection.prepareStatement(sqlCheck)) {
            stmt.setInt(1, idPanier);
            stmt.setInt(2, idProduit);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // produit déjà dans le panier → on augmente la quantité
                int nouvelleQuantite = rs.getInt("quantite") + quantite;
                modifierQuantite(idPanier, idProduit, nouvelleQuantite);
            } else {
                // nouveau produit → on l'ajoute
                String sql = "INSERT INTO Produit_Panier (id_panier, id_produit, quantite) VALUES (?, ?, ?)";
                try (PreparedStatement stmtInsert = connection.prepareStatement(sql)) {
                    stmtInsert.setInt(1, idPanier);
                    stmtInsert.setInt(2, idProduit);
                    stmtInsert.setInt(3, quantite);
                    stmtInsert.executeUpdate();
                }
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de l'ajout du produit au panier", e);
        }
    }

    // SUPPRIMER UN PRODUIT DU PANIER
    public void supprimerProduit(int idPanier, int idProduit) {
        String sql = "DELETE FROM Produit_Panier WHERE id_panier=? AND id_produit=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idPanier);
            stmt.setInt(2, idProduit);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression du produit du panier", e);
        }
    }

    //  MODIFIER LA QUANTITE
    public void modifierQuantite(int idPanier, int idProduit, int nouvelleQuantite) {
        String sql = "UPDATE Produit_Panier SET quantite=? WHERE id_panier=? AND id_produit=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, nouvelleQuantite);
            stmt.setInt(2, idPanier);
            stmt.setInt(3, idProduit);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Erreur lors de la modification de la quantité", e);
        }
    }

    //  VIDER LE PANIER
    public void viderPanier(int idPanier) {
        String sql = "DELETE FROM Produit_Panier WHERE id_panier=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idPanier);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Erreur lors du vidage du panier", e);
        }
    }
}