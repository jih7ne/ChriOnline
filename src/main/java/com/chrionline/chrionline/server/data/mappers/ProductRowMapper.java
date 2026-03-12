package com.chrionline.chrionline.server.data.mappers;

import com.chrionline.chrionline.server.data.RowMapper;
import com.chrionline.chrionline.server.data.models.Produit;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductRowMapper implements RowMapper<Produit> {

    @Override
    public Produit mapRow(ResultSet rs) throws SQLException {
        Produit p = new Produit();
        p.setId(rs.getInt("id"));
        p.setNom(rs.getString("nom"));
        p.setDescription(rs.getString("description"));
        p.setPrix(rs.getDouble("prix"));
        p.setStock(rs.getInt("stock"));
        p.setUrlImage(rs.getString("url_image"));
        p.setIdCategorie(rs.getInt("id_categorie"));

        // si on fait un JOIN avec Categorie
        try {
            p.setNomCategorie(rs.getString("nom_categorie"));
        } catch (SQLException e) {
            // pas de JOIN, on ignore
        }

        return p;
    }
}