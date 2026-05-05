package com.chrionline.server.data.mappers;

import com.chrionline.server.data.RowMapper;
import com.chrionline.shared.models.Categorie;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CategorieRowMapper implements RowMapper<Categorie> {

    @Override
    public Categorie mapRow(ResultSet rs) throws SQLException {
        Categorie c = new Categorie();
        c.setId(rs.getInt("id"));
        c.setNom(rs.getString("nom"));
        c.setDescription(rs.getString("description"));
        try { c.setNbProduits(rs.getInt("nb_produits")); } catch (SQLException ignored) {}
        return c;
    }
}
