package com.chrionline.chrionline.server.repositories;

import com.chrionline.chrionline.server.data.JdbcRepository;
import com.chrionline.chrionline.server.data.mappers.ProductRowMapper;
import com.chrionline.chrionline.server.data.models.Produit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitRepository extends JdbcRepository<Produit> {

    public ProduitRepository(Connection connection) {
        super(connection, "Produit", new ProductRowMapper());
    }

    //  AJOUTER UN PRODUIT
    @Override
    public void add(Produit item) {
        String sql = "INSERT INTO Produit (nom, description, prix, stock, url_image, id_categorie) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, item.getNom());
            stmt.setString(2, item.getDescription());
            stmt.setDouble(3, item.getPrix());
            stmt.setInt(4, item.getStock());
            stmt.setString(5, item.getUrlImage());
            stmt.setInt(6, item.getIdCategorie());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //  AJOUTER PLUSIEURS PRODUITS
    @Override
    public void addAll(List<Produit> items) {
        for (Produit p : items) {
            add(p);
        }
    }

    //  MODIFIER UN PRODUIT
    @Override
    public void update(String id, Produit item) {
        String sql = "UPDATE Produit SET nom=?, description=?, prix=?, stock=?, " +
                "url_image=?, id_categorie=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, item.getNom());
            stmt.setString(2, item.getDescription());
            stmt.setDouble(3, item.getPrix());
            stmt.setInt(4, item.getStock());
            stmt.setString(5, item.getUrlImage());
            stmt.setInt(6, item.getIdCategorie());
            stmt.setInt(7, Integer.parseInt(id));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //  LISTER TOUS LES PRODUITS AVEC CATEGORIE
    public List<Produit> findAll() {
        String sql = "SELECT p.*, c.nom AS nom_categorie " +
                "FROM Produit p " +
                "LEFT JOIN Categorie c ON p.id_categorie = c.id " +
                "ORDER BY p.date_ajout DESC";
        List<Produit> produits = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                produits.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return produits;
    }

    // TROUVER UN PRODUIT PAR ID
    public Produit findById(int id) {
        String sql = "SELECT p.*, c.nom AS nom_categorie " +
                "FROM Produit p " +
                "LEFT JOIN Categorie c ON p.id_categorie = c.id " +
                "WHERE p.id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rowMapper.mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //  RECHERCHE PAR CATEGORIE
    public List<Produit> findByCategorie(int idCategorie) {
        String sql = "SELECT p.*, c.nom AS nom_categorie " +
                "FROM Produit p " +
                "LEFT JOIN Categorie c ON p.id_categorie = c.id " +
                "WHERE p.id_categorie = ? " +
                "ORDER BY p.date_ajout DESC";
        List<Produit> produits = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idCategorie);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                produits.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return produits;
    }

    //  MISE A JOUR DU STOCK
    public void updateStock(int id, int nouveauStock) {
        String sql = "UPDATE Produit SET stock=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, nouveauStock);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //  SUPPRIMER UN PRODUIT
    public void deleteProduit(int id) {
        String sql = "DELETE FROM Produit WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //  RECHERCHE PAR NOM
    public List<Produit> findByNom(String nom) {
        String sql = "SELECT p.*, c.nom AS nom_categorie " +
                "FROM Produit p " +
                "LEFT JOIN Categorie c ON p.id_categorie = c.id " +
                "WHERE p.nom LIKE ? " +
                "ORDER BY p.date_ajout DESC";
        List<Produit> produits = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + nom + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                produits.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return produits;
    }
}