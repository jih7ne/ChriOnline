package com.chrionline.server.repositories;

import com.chrionline.core.enums.StatutCommande;
import com.chrionline.server.data.JdbcRepository;
import com.chrionline.server.data.mappers.CommandeRowMapper;
import com.chrionline.shared.models.Commande;
import com.chrionline.shared.models.MonthlyRevenueStats;
import com.chrionline.shared.models.MonthlyStats;
import com.chrionline.shared.models.OrderSummary;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
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




    public BigDecimal getTotalRevenue() {
        String sql = "SELECT sum(prix_total) FROM commande WHERE statut=?";

        try(PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, StatutCommande.VALIDEE.name().toLowerCase());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal(1);
            }

        }catch(SQLException e){
            e.printStackTrace();
        }

        return new BigDecimal(0);
    }


    public List<MonthlyStats> getMonthlyOrders() {
        List<MonthlyStats> monthlyStats = new ArrayList<>();
        LocalDate now = LocalDate.now();

        String query = """
            SELECT 
                YEAR(date) AS year,
                MONTH(date) AS month,
                COUNT(*) AS count
            FROM Commande
            WHERE YEAR(date) = ?
                      AND statut = ?
            GROUP BY YEAR(date), MONTH(date)
            ORDER BY year, month
        """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, now.getYear());
            stmt.setString(2, StatutCommande.VALIDEE.name().toLowerCase());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int year = rs.getInt("year");
                    int month = rs.getInt("month");
                    long count = rs.getLong("count");

                    MonthlyStats stats = new MonthlyStats();
                    stats.setYear(year);
                    stats.setMonth(month);
                    stats.setMonthName(
                            java.time.Month.of(month)
                                    .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                    );
                    stats.setCount(count);

                    monthlyStats.add(stats);
                }
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return monthlyStats;
    }

    public List<MonthlyRevenueStats> getMonthlyRevenue() {
        List<MonthlyRevenueStats> monthlyRevenue = new ArrayList<>();
        LocalDate now = LocalDate.now();

        String query = """
            SELECT 
                YEAR(date) AS year,
                MONTH(date) AS month,
                SUM(prix_total) AS revenue
            FROM Commande
            WHERE YEAR(date) = ?
              AND statut = ?
            GROUP BY YEAR(date), MONTH(date)
            ORDER BY month
        """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, now.getYear());
            stmt.setString(2, StatutCommande.VALIDEE.name().toLowerCase());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int year = rs.getInt("year");
                    int month = rs.getInt("month");
                    BigDecimal revenue = rs.getBigDecimal("revenue");


                    if (revenue == null) {
                        revenue = BigDecimal.ZERO;
                    }

                    MonthlyRevenueStats stats = new MonthlyRevenueStats();
                    stats.setYear(year);
                    stats.setMonth(month);
                    stats.setMonthName(
                            java.time.Month.of(month)
                                    .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                    );
                    stats.setRevenue(revenue);

                    monthlyRevenue.add(stats);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return monthlyRevenue;
    }

    public int getCommandeCountByStatus(StatutCommande statut) {
        String sql = "SELECT COUNT(*) FROM commande WHERE statut=? ";

        try(PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, statut.name().toLowerCase());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        }catch(SQLException e){
            e.printStackTrace();
        }

        return 0;

    }

    public List<OrderSummary> getRecentOrders(int limit) {
        List<OrderSummary> orders = new ArrayList<>();

        String query = """
            SELECT 
                c.id_commande,
                c.uuid_commande,
                u.nom,
                u.prenom,
                u.email,
                c.prix_total,
                c.statut,
                c.date
            FROM Commande c
            JOIN Utilisateur u ON c.id_utilisateur = u.id
            ORDER BY c.date DESC
            LIMIT ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OrderSummary order = new OrderSummary();

                    order.setOrderId(rs.getLong("id_commande"));
                    order.setUuid(rs.getString("uuid_commande"));
                    order.setUsername(rs.getString("nom").toUpperCase() + " " + rs.getString("prenom"));
                    order.setEmail(rs.getString("email"));
                    order.setTotal(rs.getBigDecimal("prix_total"));


                    String statusStr = rs.getString("statut");
                    order.setStatus(StatutCommande.valueOf(statusStr.toUpperCase()));

                    order.setDate(rs.getTimestamp("date").toLocalDateTime());

                    orders.add(order);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return orders;
    }

    public List<OrderSummary> getAllOrders() {
        List<OrderSummary> orders = new ArrayList<>();

        String query = """
            SELECT 
                c.id_commande,
                c.uuid_commande,
                u.nom,
                u.prenom,
                u.email,
                c.prix_total,
                c.statut,
                c.date
            FROM Commande c
            JOIN Utilisateur u ON c.id_utilisateur = u.id
            ORDER BY c.date DESC
        """;

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OrderSummary order = new OrderSummary();

                    order.setOrderId(rs.getLong("id_commande"));
                    order.setUuid(rs.getString("uuid_commande"));
                    order.setUsername(rs.getString("nom").toUpperCase() + " " + rs.getString("prenom"));
                    order.setEmail(rs.getString("email"));
                    order.setTotal(rs.getBigDecimal("prix_total"));

                    String statusStr = rs.getString("statut");
                    order.setStatus(StatutCommande.valueOf(statusStr.toUpperCase()));

                    order.setDate(rs.getTimestamp("date").toLocalDateTime());

                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return orders;
    }
}
