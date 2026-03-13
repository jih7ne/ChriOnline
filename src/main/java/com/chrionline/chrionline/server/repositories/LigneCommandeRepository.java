package com.chrionline.chrionline.server.repositories;

import com.chrionline.chrionline.server.data.JdbcRepository;
import com.chrionline.chrionline.server.data.mappers.LigneCommandeRowMapper;
import com.chrionline.chrionline.server.data.models.LigneCommande;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LigneCommandeRepository extends JdbcRepository<LigneCommande> {

    public LigneCommandeRepository(Connection connection, LigneCommandeRowMapper rowMapper) {
        super(connection, "ligne_commande", rowMapper);
    }

    // AJOUTER UNE LIGNE DE COMMANDE
    @Override
    public void add(LigneCommande ligne) {
        String sql = "INSERT INTO ligne_commande (id_commande, id_produit, quantite, prix_unitaire) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, ligne.getId_commande());
            stmt.setInt(2, ligne.getId_produit());
            stmt.setInt(3, ligne.getQuantite());
            stmt.setDouble(4, ligne.getPrix_unitaire());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // AJOUTER PLUSIEURS LIGNES EN LOT (batch insert)
    @Override
    public void addAll(List<LigneCommande> items) {
        String sql = "INSERT INTO ligne_commande (id_commande, id_produit, quantite, prix_unitaire) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (LigneCommande ligne : items) {
                stmt.setInt(1, ligne.getId_commande());
                stmt.setInt(2, ligne.getId_produit());
                stmt.setInt(3, ligne.getQuantite());
                stmt.setDouble(4, ligne.getPrix_unitaire());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // MODIFIER UNE LIGNE (id sous forme de String)
    @Override
    public void update(String id, LigneCommande ligne) {
        String sql = "UPDATE ligne_commande SET id_commande=?, id_produit=?, quantite=?, prix_unitaire=? " +
                "WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, ligne.getId_commande());
            stmt.setInt(2, ligne.getId_produit());
            stmt.setInt(3, ligne.getQuantite());
            stmt.setDouble(4, ligne.getPrix_unitaire());
            stmt.setInt(5, Integer.parseInt(id));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // MODIFIER UNE LIGNE (surcharge avec id int)
    public void update(int id, LigneCommande ligne) {
        update(String.valueOf(id), ligne);
    }

    // RÉCUPÉRER TOUTES LES LIGNES D'UNE COMMANDE
    public List<LigneCommande> getLignesCommande(int idCommande) {
        String sql = "SELECT * FROM ligne_commande WHERE id_commande = ?";
        List<LigneCommande> lignes = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idCommande);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                lignes.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lignes;
    }

    // SUPPRIMER TOUTES LES LIGNES D'UNE COMMANDE (nettoyage en cascade)
    public void deleteLignesCommande(int idCommande) {
        String sql = "DELETE FROM ligne_commande WHERE id_commande = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idCommande);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}