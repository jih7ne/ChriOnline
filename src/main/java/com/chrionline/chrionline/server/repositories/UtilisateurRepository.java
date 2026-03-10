package com.chrionline.chrionline.server.repositories;

import com.chrionline.chrionline.server.data.models.Utilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurRepository {

    private static final Logger logger = LoggerFactory.getLogger(UtilisateurRepository.class);
    private final Connection connection;

    public UtilisateurRepository(Connection connection) {
        this.connection = connection;
    }

    private Utilisateur mapRow(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasse(rs.getString("mot_de_passe"));
        u.setRole(rs.getString("role"));
        u.setStatut(rs.getString("statut"));
        return u;
    }

    public boolean add(Utilisateur u) {
        String sql = "INSERT INTO Utilisateur (nom, prenom, email, mot_de_passe, role, statut) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, u.getNom());
            stmt.setString(2, u.getPrenom());
            stmt.setString(3, u.getEmail());
            stmt.setString(4, u.getMotDePasse());
            stmt.setString(5, u.getRole() != null ? u.getRole() : "client");
            stmt.setString(6, u.getStatut() != null ? u.getStatut() : "actif");
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) u.setId(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) { logger.error("Erreur add : {}", e.getMessage()); }
        return false;
    }

    public Utilisateur getById(int id) {
        String sql = "SELECT * FROM Utilisateur WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { logger.error("Erreur getById : {}", e.getMessage()); }
        return null;
    }

    public Utilisateur getByEmail(String email) {
        String sql = "SELECT * FROM Utilisateur WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { logger.error("Erreur getByEmail : {}", e.getMessage()); }
        return null;
    }

    public List<Utilisateur> getAll() {
        String sql = "SELECT * FROM Utilisateur";
        List<Utilisateur> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { logger.error("Erreur getAll : {}", e.getMessage()); }
        return list;
    }

    public boolean update(Utilisateur u) {
        String sql = "UPDATE Utilisateur SET nom=?, prenom=?, email=?, role=?, statut=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, u.getNom());
            stmt.setString(2, u.getPrenom());
            stmt.setString(3, u.getEmail());
            stmt.setString(4, u.getRole());
            stmt.setString(5, u.getStatut());
            stmt.setInt(6, u.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { logger.error("Erreur update : {}", e.getMessage()); }
        return false;
    }

    public boolean updateStatut(int id, String statut) {
        String sql = "UPDATE Utilisateur SET statut=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, statut);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { logger.error("Erreur updateStatut : {}", e.getMessage()); }
        return false;
    }

    public boolean updatePassword(int id, String newHash) {
        String sql = "UPDATE Utilisateur SET mot_de_passe=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newHash);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { logger.error("Erreur updatePassword : {}", e.getMessage()); }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM Utilisateur WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { logger.error("Erreur delete : {}", e.getMessage()); }
        return false;
    }

    public boolean emailExiste(String email) { return getByEmail(email) != null; }
}