package com.chrionline.chrionline.server.services;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.core.enums.StatutCommande;
import com.chrionline.chrionline.server.data.models.DashboardStats;
import com.chrionline.chrionline.server.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminService {
    private final ProduitRepository produitRepository;
    private final CommandeRepository commandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PaiementRepository paiementRepository;
    private final CategorieRepository categorieRepository;

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);


    public AdminService(ProduitRepository produitRepository, CommandeRepository commandeRepository, UtilisateurRepository utilisateurRepository, PaiementRepository paiementRepository, CategorieRepository categorieRepository) {
        this.produitRepository = produitRepository;
        this.commandeRepository = commandeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.paiementRepository = paiementRepository;
        this.categorieRepository = categorieRepository;
    }


    public DashboardStats getDashboardStats() {
        logger.info("Fetching Dashboard Stats");

        DashboardStats dashboardStats = new DashboardStats();

        // Basic counts
        dashboardStats.setTotalProducts(produitRepository.count());
        dashboardStats.setTotalCategories(categorieRepository.count());
        dashboardStats.setTotalUsers(utilisateurRepository.count());
        dashboardStats.setTotalOrders(commandeRepository.count());
        dashboardStats.setTotalPayments(paiementRepository.count());

        //Have to check sockets
        dashboardStats.setActiveUsers(0);
        dashboardStats.setPendingOrders(commandeRepository.getCommandeCountByStatus(StatutCommande.EN_ATTENTE));
        dashboardStats.setCancelledOrders(commandeRepository.getCommandeCountByStatus(StatutCommande.ANNULEE));
        dashboardStats.setApprovedOrders(commandeRepository.getCommandeCountByStatus(StatutCommande.VALIDEE));
        dashboardStats.setTotalRevenue(commandeRepository.getTotalRevenue());
        dashboardStats.setLowStockProducts(produitRepository.getProductStock(AppConstants.LOW_STOCK_PRODUCTS_THRESHOLD));
        dashboardStats.setOutOfStockProducts(produitRepository.getProductStock(0));



        dashboardStats.setMonthlyOrders(commandeRepository.getMonthlyOrders());
        dashboardStats.setMonthlyRevenue(commandeRepository.getMonthlyRevenue());
        dashboardStats.setMonthlyUsers(utilisateurRepository.getMonthlyNewUsers());
        dashboardStats.setProductsByCategory(produitRepository.getProductsByCategory());
        dashboardStats.setRecentOrders(commandeRepository.getRecentOrders(AppConstants.HEAD_LIMIT));
        dashboardStats.setRecentUsers(utilisateurRepository.getRecentUsers(AppConstants.HEAD_LIMIT));


        return dashboardStats;
    }

    public java.util.List<com.chrionline.chrionline.shared.models.OrderSummary> getAllOrders() {
        logger.info("Fetching All Orders for Admin");
        return commandeRepository.getAllOrders();
    }

    public boolean updateOrderStatus(int idCommande, StatutCommande statut) {
        logger.info("Updating order status for id={} to {}", idCommande, statut);
        com.chrionline.chrionline.server.data.models.Commande commande = commandeRepository.getCommandeById(idCommande);
        if (commande == null) {
            logger.warn("Commande id={} introuvable", idCommande);
            return false;
        }

        commandeRepository.updateStatut(idCommande, statut);

        // ── UDP : Scénario 4 — notifier le client concerné
        try {
            NotificationService ns = AppConfig.getService(NotificationService.class);
            if (ns != null) {
                ns.notifyOrderStatusChanged(
                        commande.getId_utilisateur(),
                        commande.getUuid_commande(),
                        statut.name()
                );
            }
        } catch (Exception ex) {
            logger.warn("Impossible d'envoyer la notif de changement de statut : {}", ex.getMessage());
        }

        return true;
    }




}
