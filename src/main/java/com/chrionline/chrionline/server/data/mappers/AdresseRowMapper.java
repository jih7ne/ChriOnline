package com.chrionline.chrionline.server.data.mappers;

import com.chrionline.chrionline.server.data.RowMapper;
import com.chrionline.chrionline.server.data.models.Adresse;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AdresseRowMapper implements RowMapper<Adresse> {

    @Override
    public Adresse mapRow(ResultSet rs) throws SQLException {
        Adresse a = new Adresse();

        a.setId(rs.getInt("id"));
        a.setId_utilisateur(rs.getInt("id_utilisateur"));
        a.setRue(rs.getString("rue"));
        a.setComplement(rs.getString("complement")); // nullable
        a.setVille(rs.getString("ville"));
        a.setCode_postal(rs.getString("code_postal"));
        a.setPays(rs.getString("pays"));

        // TINYINT(1) -> Boolean (0 = false, 1 = true)
        a.setEst_principale(rs.getInt("est_principale") == 1);

        return a;
    }
}