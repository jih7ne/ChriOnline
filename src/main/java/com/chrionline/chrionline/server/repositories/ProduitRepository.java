package com.chrionline.chrionline.server.repositories;

import com.chrionline.chrionline.server.data.JdbcRepository;
import com.chrionline.chrionline.server.data.mappers.ProductRowMapper;
import com.chrionline.chrionline.server.data.models.Produit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class ProduitRepository extends JdbcRepository<Produit> {

    public ProduitRepository(Connection connection) {
        super(connection, "products", new ProductRowMapper());
    }

    @Override
    public void add(Produit item) {
        String sql = "INSERT INTO products (id, name, price) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, item.getId());
            stmt.setString(2, item.getNom());
            stmt.setDouble(3, item.getPrix());

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addAll(List<Produit> items) {
        for (Produit p : items) {
            add(p);
        }
    }

    @Override
    public void update(String id, Produit item) {
        String sql = "UPDATE products SET name=?, price=? WHERE id=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, item.getNom());
            stmt.setDouble(2, item.getPrix());
            stmt.setString(3, id);

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
