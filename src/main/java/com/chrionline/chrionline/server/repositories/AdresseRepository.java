package com.chrionline.chrionline.server.repositories;

import com.chrionline.chrionline.server.data.JdbcRepository;
import com.chrionline.chrionline.server.data.mappers.AdresseRowMapper;
import com.chrionline.chrionline.server.data.models.Adresse;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdresseRepository extends JdbcRepository<Adresse> {

    public AdresseRepository(Connection connection, AdresseRowMapper rowMapper) {
        super(connection, "adresse", rowMapper);
    }

    // AJOUTER UNE ADRESSE
    @Override
    public void add(Adresse adresse) {
        // Si la nouvelle adresse est principale, remettre toutes les autres à 0
        if (Boolean.TRUE.equals(adresse.getEst_principale())) {
            resetAdressesPrincipales(adresse.getId_utilisateur());
        }
        String sql = "INSERT INTO adresse (id_utilisateur, rue, complement, ville, code_postal, pays, est_principale) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, adresse.getId_utilisateur());
            stmt.setString(2, adresse.getRue());
            stmt.setString(3, adresse.getComplement()); // nullable, pas de vérification nécessaire
            stmt.setString(4, adresse.getVille());
            stmt.setString(5, adresse.getCode_postal());
            stmt.setString(6, adresse.getPays() != null ? adresse.getPays() : "Maroc");
            stmt.setInt(7, Boolean.TRUE.equals(adresse.getEst_principale()) ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // AJOUTER PLUSIEURS ADRESSES (batch insert)
    @Override
    public void addAll(List<Adresse> items) {
        String sql = "INSERT INTO adresse (id_utilisateur, rue, complement, ville, code_postal, pays, est_principale) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Adresse adresse : items) {
                stmt.setInt(1, adresse.getId_utilisateur());
                stmt.setString(2, adresse.getRue());
                stmt.setString(3, adresse.getComplement());
                stmt.setString(4, adresse.getVille());
                stmt.setString(5, adresse.getCode_postal());
                stmt.setString(6, adresse.getPays() != null ? adresse.getPays() : "Maroc");
                stmt.setInt(7, Boolean.TRUE.equals(adresse.getEst_principale()) ? 1 : 0);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // MODIFIER UNE ADRESSE (id sous forme de String)
    @Override
    public void update(String id, Adresse adresse) {
        // Si l'adresse modifiée devient principale, remettre toutes les autres à 0
        if (Boolean.TRUE.equals(adresse.getEst_principale())) {
            resetAdressesPrincipales(adresse.getId_utilisateur());
        }
        String sql = "UPDATE adresse SET id_utilisateur=?, rue=?, complement=?, ville=?, " +
                "code_postal=?, pays=?, est_principale=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, adresse.getId_utilisateur());
            stmt.setString(2, adresse.getRue());
            stmt.setString(3, adresse.getComplement());
            stmt.setString(4, adresse.getVille());
            stmt.setString(5, adresse.getCode_postal());
            stmt.setString(6, adresse.getPays() != null ? adresse.getPays() : "Maroc");
            stmt.setInt(7, Boolean.TRUE.equals(adresse.getEst_principale()) ? 1 : 0);
            stmt.setInt(8, Integer.parseInt(id));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // MODIFIER UNE ADRESSE (surcharge avec id int)
    public void update(int id, Adresse adresse) {
        update(String.valueOf(id), adresse);
    }

    // RÉCUPÉRER TOUTES LES ADRESSES D'UN UTILISATEUR
    public List<Adresse> getAdressesUtilisateur(int idUtilisateur) {
        String sql = "SELECT * FROM adresse WHERE id_utilisateur = ?";
        List<Adresse> adresses = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idUtilisateur);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                adresses.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return adresses;
    }

    // RÉCUPÉRER L'ADRESSE PRINCIPALE D'UN UTILISATEUR
    public Adresse getAdressePrincipale(int idUtilisateur) {
        String sql = "SELECT * FROM adresse WHERE id_utilisateur = ? AND est_principale = 1 LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idUtilisateur);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rowMapper.mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // RESET toutes les adresses principales d'un utilisateur à 0
    private void resetAdressesPrincipales(int idUtilisateur) {
        String sqlReset = "UPDATE adresse SET est_principale = 0 WHERE id_utilisateur = ?";
        try (PreparedStatement stmtReset = connection.prepareStatement(sqlReset)) {
            stmtReset.setInt(1, idUtilisateur);
            stmtReset.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DÉFINIR UNE ADRESSE COMME PRINCIPALE
    // (remet toutes les autres à 0 d'abord pour garantir l'unicité)
    public void setAdressePrincipale(int idUtilisateur, int idAdresse) {
        String sqlSet = "UPDATE adresse SET est_principale = 1 WHERE id = ? AND id_utilisateur = ?";
        try {
            // Étape 1 : reset toutes les adresses de l'utilisateur
            resetAdressesPrincipales(idUtilisateur);
            // Étape 2 : marquer la nouvelle adresse principale
            try (PreparedStatement stmtSet = connection.prepareStatement(sqlSet)) {
                stmtSet.setInt(1, idAdresse);
                stmtSet.setInt(2, idUtilisateur);
                stmtSet.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // SUPPRIMER UNE ADRESSE
    public void delete(int idAdresse) {
        String sql = "DELETE FROM adresse WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idAdresse);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}