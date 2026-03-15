package com.chrionline.chrionline.server.repositories;

import com.chrionline.chrionline.core.enums.StatutPaiement;
import com.chrionline.chrionline.server.data.JdbcRepository;
import com.chrionline.chrionline.server.data.mappers.PaiementRowMapper;
import com.chrionline.chrionline.server.data.models.Paiement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaiementRepository extends JdbcRepository<Paiement> {

    public PaiementRepository(Connection connection, PaiementRowMapper rowMapper) {
        super(connection, "paiement", rowMapper);
    }

    // AJOUTER UN PAIEMENT
    @Override
    public void add(Paiement paiement) {
        String sql = "INSERT INTO paiement (id_commande, date_paiement, numero_masque, methode_paiement, statut) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, paiement.getId_commande());
            stmt.setTimestamp(2, paiement.getDate_paiement() != null
                    ? Timestamp.valueOf(paiement.getDate_paiement())
                    : Timestamp.valueOf(java.time.LocalDateTime.now()));
            stmt.setString(3, paiement.getNumero_masque());
            stmt.setString(4, paiement.getMethode_paiement() != null
                    ? paiement.getMethode_paiement().name().toLowerCase()
                    : null);
            stmt.setString(5, paiement.getStatut() != null
                    ? paiement.getStatut().name().toLowerCase()
                    : StatutPaiement.EN_ATTENTE.name().toLowerCase());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur SQL lors de l'ajout du paiement: " + e.getMessage(), e);
        }
    }

    // AJOUTER PLUSIEURS PAIEMENTS (batch insert)
    @Override
    public void addAll(List<Paiement> items) {
        String sql = "INSERT INTO paiement (id_commande, date_paiement, numero_masque, methode_paiement, statut) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Paiement paiement : items) {
                stmt.setInt(1, paiement.getId_commande());
                stmt.setTimestamp(2, paiement.getDate_paiement() != null
                        ? Timestamp.valueOf(paiement.getDate_paiement())
                        : Timestamp.valueOf(java.time.LocalDateTime.now()));
                stmt.setString(3, paiement.getNumero_masque());
                stmt.setString(4, paiement.getMethode_paiement() != null
                        ? paiement.getMethode_paiement().name().toLowerCase()
                        : null);
                stmt.setString(5, paiement.getStatut() != null
                        ? paiement.getStatut().name().toLowerCase()
                        : StatutPaiement.EN_ATTENTE.name().toLowerCase());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // MODIFIER UN PAIEMENT (id sous forme de String)
    @Override
    public void update(String id, Paiement paiement) {
        String sql = "UPDATE paiement SET id_commande=?, date_paiement=?, numero_masque=?, " +
                "methode_paiement=?, statut=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, paiement.getId_commande());
            stmt.setTimestamp(2, paiement.getDate_paiement() != null
                    ? Timestamp.valueOf(paiement.getDate_paiement())
                    : null);
            stmt.setString(3, paiement.getNumero_masque());
            stmt.setString(4, paiement.getMethode_paiement() != null
                    ? paiement.getMethode_paiement().name().toLowerCase()
                    : null);
            stmt.setString(5, paiement.getStatut() != null
                    ? paiement.getStatut().name().toLowerCase()
                    : null);
            stmt.setInt(6, Integer.parseInt(id));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // MODIFIER UN PAIEMENT (surcharge avec id int)
    public void update(int id, Paiement paiement) {
        update(String.valueOf(id), paiement);
    }

    // METTRE À JOUR UNIQUEMENT LE STATUT
    public void updateStatut(int idPaiement, StatutPaiement statut) {
        String sql = "UPDATE paiement SET statut=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, statut.name().toLowerCase());
            stmt.setInt(2, idPaiement);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // RÉCUPÉRER LE PAIEMENT D'UNE COMMANDE (relation 1-1 UNIQUE)
    public Paiement getPaiementByCommande(int idCommande) {
        String sql = "SELECT * FROM paiement WHERE id_commande = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idCommande);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rowMapper.mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // RÉCUPÉRER TOUS LES PAIEMENTS
    public List<Paiement> getAll() {
        String sql = "SELECT * FROM paiement";
        List<Paiement> paiements = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                paiements.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return paiements;
    }
}