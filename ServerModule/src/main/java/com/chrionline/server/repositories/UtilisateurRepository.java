package com.chrionline.server.repositories;

import com.chrionline.shared.models.MonthlyUserStats;
import com.chrionline.shared.models.UserSummary;
import com.chrionline.shared.models.Utilisateur;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurRepository {

    private static final Logger logger = LoggerFactory.getLogger(UtilisateurRepository.class);
    private final Connection connection;

    public UtilisateurRepository(Connection connection) {
        this.connection = connection;
    }

    public int count() {
        String query = "SELECT COUNT(*) FROM Utilisateur";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            if (resultSet.next()) return resultSet.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ── Mapper ────────────────────────────────────────────────────────────
    private Utilisateur mapRow(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasse(rs.getString("mot_de_passe"));
        u.setRole(rs.getString("role"));
        u.setStatut(rs.getString("statut"));
        u.setQuestionSecrete(rs.getString("question_secrete"));
        u.setReponseSecrete(rs.getString("reponse_secrete"));

        // ── Champs 2FA (NULL-safe) ────────────────────────────────────────
        u.setTwoFactorEnabled(rs.getBoolean("two_factor_enabled"));
        u.setTwoFactorSecret(rs.getString("two_factor_secret"));
        u.setTwoFactorVerified(rs.getBoolean("two_factor_verified"));
        return u;
    }

    // ── CRUD de base ──────────────────────────────────────────────────────

    public boolean add(Utilisateur u) {
        String sql = "INSERT INTO Utilisateur (nom, prenom, email, mot_de_passe, role, statut, question_secrete, reponse_secrete) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, u.getNom());
            stmt.setString(2, u.getPrenom());
            stmt.setString(3, u.getEmail());
            stmt.setString(4, u.getMotDePasse());
            stmt.setString(5, u.getRole()   != null ? u.getRole()   : "client");
            stmt.setString(6, u.getStatut() != null ? u.getStatut() : "actif");
            stmt.setString(7, u.getQuestionSecrete());
            stmt.setString(8, u.getReponseSecrete());
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

    // ── 2FA ───────────────────────────────────────────────────────────────

    public boolean saveTwoFactorSecret(int userId, String secret) {
        String sql = "UPDATE Utilisateur SET two_factor_secret = ?, two_factor_verified = FALSE WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, secret);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { logger.error("Erreur saveTwoFactorSecret : {}", e.getMessage()); }
        return false;
    }

    public boolean enableTwoFactor(int userId) {
        String sql = "UPDATE Utilisateur SET two_factor_enabled = TRUE, two_factor_verified = TRUE WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { logger.error("Erreur enableTwoFactor : {}", e.getMessage()); }
        return false;
    }

    public boolean disableTwoFactor(int userId) {
        String sql = "UPDATE Utilisateur SET two_factor_enabled = FALSE, two_factor_secret = NULL, two_factor_verified = FALSE WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { logger.error("Erreur disableTwoFactor : {}", e.getMessage()); }
        return false;
    }

    // ── Stats ─────────────────────────────────────────────────────────────

    public List<MonthlyUserStats> getMonthlyNewUsers() {
        List<MonthlyUserStats> statsList = new ArrayList<>();
        String query = """
            SELECT YEAR(u.created_at) AS year, MONTH(u.created_at) AS month, COUNT(*) AS new_users
            FROM Utilisateur u
            WHERE YEAR(u.created_at) = ?
            GROUP BY YEAR(u.created_at), MONTH(u.created_at)
            ORDER BY month
        """;
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, LocalDate.now().getYear());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    MonthlyUserStats stats = new MonthlyUserStats();
                    stats.setYear(rs.getInt("year"));
                    stats.setMonth(rs.getInt("month"));
                    stats.setMonthName(
                            java.time.Month.of(rs.getInt("month"))
                                    .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                    );
                    stats.setNewUsers(rs.getLong("new_users"));
                    statsList.add(stats);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return statsList;
    }

    public List<UserSummary> getRecentUsers(int limit) {
        List<UserSummary> users = new ArrayList<>();
        String query = """
            SELECT u.id, CONCAT(u.nom, ' ', u.prenom) AS username, u.email,
                   u.created_at, COUNT(c.id_commande) AS order_count
            FROM Utilisateur u
            LEFT JOIN Commande c ON u.id = c.id_utilisateur
            GROUP BY u.id, u.nom, u.prenom, u.email, u.created_at
            ORDER BY u.created_at DESC LIMIT ?
        """;
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UserSummary user = new UserSummary();
                    user.setUserId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setRegistrationDate(rs.getTimestamp("created_at").toLocalDateTime());
                    user.setOrderCount(rs.getLong("order_count"));
                    users.add(user);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return users;
    }

    public boolean emailExiste(String email) { return getByEmail(email) != null; }
}
