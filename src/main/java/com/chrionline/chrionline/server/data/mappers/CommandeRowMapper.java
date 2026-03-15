package com.chrionline.chrionline.server.data.mappers;

import com.chrionline.chrionline.core.enums.StatutCommande;
import com.chrionline.chrionline.server.data.RowMapper;
import com.chrionline.chrionline.server.data.models.Commande;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class CommandeRowMapper implements RowMapper<Commande> {

    @Override
    public Commande mapRow(ResultSet rs) throws SQLException {
        Commande c = new Commande();

        c.setId_commande(rs.getInt("id_commande"));
        
        try {
            c.setUuid_commande(rs.getString("uuid_commande"));
        } catch (SQLException e) {
            // Ignorer si la colonne n'existe pas
        }
        
        c.setId_utilisateur(rs.getInt("id_utilisateur"));
        Integer idPanier = rs.getInt("id_panier");
        if (rs.wasNull()) {
            c.setId_panier(null);
        } else {
            c.setId_panier(idPanier);
        }
        
        try {
            c.setId_adresse(rs.getInt("id_adresse"));
        } catch (SQLException e) {
            // Ignorer si la colonne n'existe pas
        }
        // Conversion Timestamp -> LocalDateTime 
        Timestamp ts = rs.getTimestamp("date");
        if (ts != null) {
            c.setDate(ts.toLocalDateTime());
        }

        // Conversion String -> enum StatutCommande
        String statutStr = rs.getString("statut");
        if (statutStr != null) {
            try {
                c.setStatut(StatutCommande.valueOf(statutStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.err.println("Statut inconnu en base de donnees : " + statutStr);
                c.setStatut(StatutCommande.EN_ATTENTE);
            }
        }

        c.setPrix_total(rs.getDouble("prix_total"));

        return c;
    }
}
