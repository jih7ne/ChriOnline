package com.chrionline.chrionline.server;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.network.tcp.TCPServer;
import com.chrionline.chrionline.network.udp.UDPNotificationDispatcher;
import com.chrionline.chrionline.server.controllers.*;
import com.chrionline.chrionline.server.data.mappers.AdresseRowMapper;
import com.chrionline.chrionline.server.data.mappers.CommandeRowMapper;
import com.chrionline.chrionline.server.data.mappers.LigneCommandeRowMapper;
import com.chrionline.chrionline.server.data.mappers.PaiementRowMapper;
import com.chrionline.chrionline.server.repositories.*;
import com.chrionline.chrionline.server.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class ServerApplication {

    private static final Logger              logger = LoggerFactory.getLogger(ServerApplication.class);
    private static UDPNotificationDispatcher udpNotificationDispatcher;

    public static void main(String[] args) {
        try {
            logger.info("Démarrage du serveur ChriOnline...");
            registerRepositories();
            setupUDP();
            registerServices();
            registerControllers();
            logger.info("Démarrage TCP sur le port {}", AppConstants.SERVER_PORT);
            new TCPServer();
        } catch (Exception e) {
            logger.error("Échec du démarrage", e);
            stopInstances();
            System.exit(-1);
        }
    }

    private static void setupUDP() throws Exception {
        AppConfig.getLogger().info("Setting up UDP Services");
        udpNotificationDispatcher = new UDPNotificationDispatcher();
        udpNotificationDispatcher.setNotificationHandler(notification ->
                logger.info("Server received UDP notification: {}", notification.getMessage()));
        udpNotificationDispatcher.start();
        Thread.sleep(500);
        logger.info("Setup complete — UDP Services Initialized");
    }

    private static void stopInstances() {
        AppConfig.getLogger().info("--- Tearing down ---");
        if (udpNotificationDispatcher != null) udpNotificationDispatcher.stop();
    }

    private static void registerRepositories() throws SQLException {
        AppConfig.registerRepo(CategorieRepository.class,
                new CategorieRepository(AppConfig.getConnection()));
        AppConfig.registerRepo(ProduitRepository.class,
                new ProduitRepository(AppConfig.getConnection()));
        AppConfig.registerRepo(UtilisateurRepository.class,
                new UtilisateurRepository(AppConfig.getConnection()));
        AppConfig.registerRepo(PanierRepository.class,
                new PanierRepository(AppConfig.getConnection()));
        AppConfig.registerRepo(CommandeRepository.class,
                new CommandeRepository(AppConfig.getConnection(), new CommandeRowMapper()));
        AppConfig.registerRepo(LigneCommandeRepository.class,
                new LigneCommandeRepository(AppConfig.getConnection(), new LigneCommandeRowMapper()));
        AppConfig.registerRepo(PaiementRepository.class,
                new PaiementRepository(AppConfig.getConnection(), new PaiementRowMapper()));
        AppConfig.registerRepo(AdresseRepository.class,
                new AdresseRepository(AppConfig.getConnection(), new AdresseRowMapper()));
        logger.info("Repositories enregistrés");
    }

    private static void registerServices() {
        AppConfig.registerService(CategorieService.class,
                new CategorieService(AppConfig.getRepo(CategorieRepository.class)));
        AppConfig.registerService(ProduitService.class,
                new ProduitService(AppConfig.getRepo(ProduitRepository.class)));
        AppConfig.registerService(PanierService.class,
                new PanierService(
                        AppConfig.getRepo(PanierRepository.class),
                        AppConfig.getRepo(ProduitRepository.class)));
        AppConfig.registerService(CommandeService.class,
                new CommandeService(
                        AppConfig.getRepo(CommandeRepository.class),
                        AppConfig.getRepo(LigneCommandeRepository.class),
                        AppConfig.getRepo(ProduitRepository.class)));
        AppConfig.registerService(PaiementService.class,
                new PaiementService(
                        AppConfig.getRepo(PaiementRepository.class),
                        AppConfig.getService(CommandeService.class)));
        AppConfig.registerService(AdresseService.class,
                new AdresseService(AppConfig.getRepo(AdresseRepository.class)));
        AppConfig.registerService(AdminService.class, new AdminService(
                AppConfig.getRepo(ProduitRepository.class),
                AppConfig.getRepo(CommandeRepository.class),
                AppConfig.getRepo(UtilisateurRepository.class),
                AppConfig.getRepo(PaiementRepository.class),
                AppConfig.getRepo(CategorieRepository.class)));
        logger.info("Services enregistrés");
    }

    public static void registerControllers() {
        AppConfig.registerController("Auth",      new AuthController());
        AppConfig.registerController("Admin",     new AdminController());
        AppConfig.registerController("Commande",  new CommandeController());
        AppConfig.registerController("Panier",    new PanierController());
        AppConfig.registerController("Produit",   new ProduitController());
        AppConfig.registerController("Test",      new TestClientController());
        AppConfig.registerController("Paiement",  new PaiementController());
        AppConfig.registerController("Adresse",   new AdresseController());
        AppConfig.registerController("Categorie", new CategorieController());

        // ── Inject UDP dispatcher BEFORE registering the controller ──────────
        NotificationController.setDispatcher(udpNotificationDispatcher);
        AppConfig.registerController("Notification", new NotificationController());

        logger.info("Controllers enregistrés");
    }
}