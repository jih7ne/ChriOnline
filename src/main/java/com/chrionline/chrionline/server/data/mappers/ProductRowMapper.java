package com.chrionline.chrionline.server.data.mappers;

import com.chrionline.chrionline.server.data.RowMapper;
import com.chrionline.chrionline.server.data.models.Produit;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductRowMapper implements RowMapper<Produit> {

    @Override
    public Produit mapRow(ResultSet rs) throws SQLException {

        Produit p = new Produit();
        p.setId(rs.getString("id"));
        p.setNom(rs.getString("name"));
        p.setPrix(rs.getDouble("price"));

        return p;
    }
}