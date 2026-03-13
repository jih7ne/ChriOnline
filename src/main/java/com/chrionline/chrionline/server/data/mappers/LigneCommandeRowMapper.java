package com.chrionline.chrionline.server.data.mappers;

import com.chrionline.chrionline.server.data.RowMapper;
import com.chrionline.chrionline.server.data.models.LigneCommande;

import java.sql.ResultSet;
import java.sql.SQLException;

public class LigneCommandeRowMapper implements RowMapper<LigneCommande> {

    @Override
    public LigneCommande mapRow(ResultSet rs) throws SQLException {
        LigneCommande l = new LigneCommande();

        l.setId(rs.getInt("id"));
        l.setId_commande(rs.getInt("id_commande"));
        l.setId_produit(rs.getInt("id_produit"));
        l.setQuantite(rs.getInt("quantite"));
        l.setPrix_unitaire(rs.getDouble("prix_unitaire"));

        return l;
    }
}