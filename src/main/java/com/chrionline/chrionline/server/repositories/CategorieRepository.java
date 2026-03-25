package com.chrionline.chrionline.server.repositories;

import com.chrionline.chrionline.server.data.JdbcRepository;
import com.chrionline.chrionline.server.data.mappers.CategorieRowMapper;
import com.chrionline.chrionline.server.data.models.Categorie;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategorieRepository extends JdbcRepository<Categorie> {

    public CategorieRepository(Connection connection) {
        super(connection, "Categorie", new CategorieRowMapper());
    }

    public List<Categorie> findAll() {
        String sql =
                "SELECT c.id, c.nom, c.description, COUNT(p.id) AS nb_produits " +
                        "FROM Categorie c " +
                        "LEFT JOIN Produit p ON p.id_categorie = c.id " +
                        "GROUP BY c.id, c.nom, c.description " +
                        "ORDER BY c.nom";
        List<Categorie> categories = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                categories.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    public Categorie findById(int id) {
        String sql = "SELECT id, nom, description FROM Categorie WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rowMapper.mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int countProduits(int id) {
        String sql = "SELECT COUNT(*) FROM Produit WHERE id_categorie = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void add(Categorie item) {
        String sql = "INSERT INTO Categorie (nom, description) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, item.getNom());
            stmt.setString(2, item.getDescription());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addAll(List<Categorie> items) {
        for (Categorie c : items) add(c);
    }

    @Override
    public void update(String id, Categorie item) {
        String sql = "UPDATE Categorie SET nom=?, description=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, item.getNom());
            stmt.setString(2, item.getDescription());
            stmt.setInt(3, Integer.parseInt(id));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteCategorie(int id) {
        String sql = "DELETE FROM Categorie WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}