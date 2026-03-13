package com.chrionline.chrionline.server.data.mappers;

import com.chrionline.chrionline.core.enums.MethodePaiement;
import com.chrionline.chrionline.core.enums.StatutPaiement;
import com.chrionline.chrionline.server.data.RowMapper;
import com.chrionline.chrionline.server.data.models.Paiement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class PaiementRowMapper implements RowMapper<Paiement> {

    @Override
    public Paiement mapRow(ResultSet rs) throws SQLException {
        Paiement p = new Paiement();

        p.setId(rs.getInt("id"));
        p.setId_commande(rs.getInt("id_commande"));

        // Conversion Timestamp -> LocalDateTime
        Timestamp ts = rs.getTimestamp("date_paiement");
        if (ts != null) {
            p.setDate_paiement(ts.toLocalDateTime());
        }

        // Conversion String -> enum MethodePaiement
        String methodeStr = rs.getString("methode_paiement");
        if (methodeStr != null) {
            p.setMethode_paiement(MethodePaiement.valueOf(methodeStr));
        }

        // Conversion String -> enum StatutPaiement
        String statutStr = rs.getString("statut");
        if (statutStr != null) {
            p.setStatut(StatutPaiement.valueOf(statutStr));
        }

        p.setNumero_masque(rs.getString("numero_masque"));

        return p;
    }
}