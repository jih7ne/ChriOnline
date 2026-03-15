package com.chrionline.chrionline.server.repositories;

import com.chrionline.chrionline.core.enums.StatutCommande;
import com.chrionline.chrionline.server.data.JdbcRepository;
import com.chrionline.chrionline.server.data.mappers.CommandeRowMapper;
import com.chrionline.chrionline.server.data.models.Commande;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommandeRepository extends JdbcRepository<Commande> {

    public CommandeRepository(Connection connection, CommandeRowMapper rowMapper) {
        super(connection, "commande", rowMapper);
    }

    public void add(Commande commande) {
        String sql = "INSERT INTO commande (uuid_commande, id_utilisateur, id_adresse, date, statut, prix_total) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String uuid = UUID.randomUUID().toString();
            commande.setUuid_commande(uuid);
            stmt.setString(1, uuid);
            stmt.setInt(2, commande.getId_utilisateur());
            stmt.setInt(3, commande.getId_adresse());
            stmt.setTimestamp(4, commande.getDate() != null
                    ? Timestamp.valueOf(commande.getDate())
                    : Timestamp.valueOf(java.time.LocalDateTime.now()));
            stmt.setString(5, commande.getStatut() != null
                    ? commande.getStatut().name().toLowerCase()
                    : StatutCommande.EN_ATTENTE.name().toLowerCase());
            stmt.setDouble(6, commande.getPrix_total());
            stmt.executeUpdate();
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    commande.setId_commande(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur SQL lors de l'ajout de la commande", e);
        }
    }

    // AJOUTER PLUSIEURS COMMANDES
    @Override
    public void addAll(List<Commande> items) {
        String sql = "INSERT INTO commande (uuid_commande, id_utilisateur, id_adresse, date, statut, prix_total) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Commande commande : items) {
                String uuid = UUID.randomUUID().toString();
                commande.setUuid_commande(uuid);
                stmt.setString(1, uuid);
                stmt.setInt(2, commande.getId_utilisateur());
                stmt.setInt(3, commande.getId_adresse());
                stmt.setTimestamp(4, commande.getDate() != null
                        ? Timestamp.valueOf(commande.getDate())
                        : Timestamp.valueOf(java.time.LocalDateTime.now()));
                stmt.setString(5, commande.getStatut() != null
                        ? commande.getStatut().name().toLowerCase()
                        : StatutCommande.EN_ATTENTE.name().toLowerCase());
                stmt.setDouble(6, commande.getPrix_total());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // MODIFIER UNE COMMANDE (par id sous forme de String)
    @Override
    public void update(String id, Commande commande) {
        String sql = "UPDATE commande SET id_utilisateur=?, id_adresse=?, date=?, statut=?, prix_total=? " +
                     "WHERE id_commande=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, commande.getId_utilisateur());
            stmt.setInt(2, commande.getId_adresse());
            stmt.setTimestamp(3, commande.getDate() != null
                    ? Timestamp.valueOf(commande.getDate())
                    : null);
            stmt.setString(4, commande.getStatut() != null
                    ? commande.getStatut().name().toLowerCase()
                    : null);
            stmt.setDouble(5, commande.getPrix_total());
            stmt.setInt(6, Integer.parseInt(id));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // MODIFIER UNE COMMANDE (SURCHARGE : update avec id sous forme de int)
    public void update(int id, Commande commande) {
        update(String.valueOf(id), commande);
    }

    // RÉCUPÉRER TOUTES LES COMMANDES D'UN UTILISATEUR (plus récentes en premier)
    public List<Commande> getCommandes(int idUtilisateur) {
        String sql = "SELECT * FROM commande WHERE id_utilisateur = ? ORDER BY date DESC";
        List<Commande> commandes = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idUtilisateur);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                commandes.add(rowMapper.mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return commandes;
    }

    // RÉCUPÉRER UNE COMMANDE PAR SON UUID
    public Commande getCommandeByUuid(String uuid) {
        String sql = "SELECT * FROM commande WHERE uuid_commande = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rowMapper.mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // RÉCUPÉRER UNE COMMANDE PAR SON ID
    public Commande getCommandeById(int idCommande) {
        String sql = "SELECT * FROM commande WHERE id_commande = ?";
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

    // METTRE À JOUR UNIQUEMENT LE STATUT D'UNE COMMANDE
    public void updateStatut(int idCommande, StatutCommande statut) {
        String sql = "UPDATE commande SET statut=? WHERE id_commande=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, statut.name().toLowerCase());
            stmt.setInt(2, idCommande);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
