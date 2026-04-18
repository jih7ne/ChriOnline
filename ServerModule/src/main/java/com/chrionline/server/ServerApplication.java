package com.chrionline.server;

import com.chrionline.core.config.ServerConfig;
import com.chrionline.core.constants.AppConstants;
import com.chrionline.network.tcp.TCPServer;
import com.chrionline.network.udp.UDPNotificationDispatcher;
import com.chrionline.security.core.KeyManager;
import com.chrionline.server.controllers.*;
import com.chrionline.server.data.mappers.AdresseRowMapper;
import com.chrionline.server.data.mappers.CommandeRowMapper;
import com.chrionline.server.data.mappers.LigneCommandeRowMapper;
import com.chrionline.server.data.mappers.PaiementRowMapper;
import com.chrionline.server.repositories.*;
import com.chrionline.server.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;


public class ServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);
    private static UDPNotificationDispatcher udpNotificationDispatcher;

    public static void main(String[] args) {
        try {
            logger.info("Démarrage du serveur ChriOnline...");
            registerRepositories();
            setupUDP();
            registerServices();
            registerControllers();
            logger.info("Démarrage TCP sur le port {}", AppConstants.SERVER_PORT);
            KeyManager.getInstance().init();
            new TCPServer();

        } catch (Exception e) {
            logger.error("Échec du démarrage", e);
            stopInstances();
            System.exit(-1);
        }
    }


    private static void setupUDP() throws Exception {
        ServerConfig.getLogger().info("Setting up UDP Services");

        udpNotificationDispatcher = new UDPNotificationDispatcher();
        udpNotificationDispatcher.setNotificationHandler(notification -> {
            logger.info("Server received notification: {}", notification.getMessage());
        });
        udpNotificationDispatcher.start();

        // Register NotificationService AFTER dispatcher is started
        ServerConfig.registerService(NotificationService.class,
                new NotificationService(udpNotificationDispatcher));

        Thread.sleep(500);
        logger.info("Setup complete — UDP Services Initialized");
    }

    private static void stopInstances() {
        ServerConfig.getLogger().info("--- Tearing down ---");
        if (udpNotificationDispatcher != null) udpNotificationDispatcher.stop();
    }

    private static void registerRepositories() throws SQLException {
        ServerConfig.registerRepo(CategorieRepository.class,
                new CategorieRepository(ServerConfig.getConnection()));
        ServerConfig.registerRepo(ProduitRepository.class,
                new ProduitRepository(ServerConfig.getConnection()));
        ServerConfig.registerRepo(UtilisateurRepository.class,
                new UtilisateurRepository(ServerConfig.getConnection()));
        ServerConfig.registerRepo(PanierRepository.class,
                new PanierRepository(ServerConfig.getConnection()));
        ServerConfig.registerRepo(CommandeRepository.class,
                new CommandeRepository(ServerConfig.getConnection(), new CommandeRowMapper()));
        ServerConfig.registerRepo(LigneCommandeRepository.class,
                new LigneCommandeRepository(ServerConfig.getConnection(), new LigneCommandeRowMapper()));
        ServerConfig.registerRepo(PaiementRepository.class,
                new PaiementRepository(ServerConfig.getConnection(), new PaiementRowMapper()));
        ServerConfig.registerRepo(AdresseRepository.class,
                new AdresseRepository(ServerConfig.getConnection(), new AdresseRowMapper()));
        ServerConfig.registerRepo(UserDeviceRepository.class,
                new UserDeviceRepository(ServerConfig.getConnection()));
        logger.info("Repositories enregistrés");
    }

    private static void registerServices() {
        ServerConfig.registerService(CategorieService.class,
                new CategorieService(ServerConfig.getRepo(CategorieRepository.class)));
        ServerConfig.registerService(ProduitService.class,
                new ProduitService(ServerConfig.getRepo(ProduitRepository.class)));
        ServerConfig.registerService(PanierService.class,
                new PanierService(
                        ServerConfig.getRepo(PanierRepository.class),
                        ServerConfig.getRepo(ProduitRepository.class),
                        ServerConfig.getRepo(UtilisateurRepository.class)
                ));
        ServerConfig.registerService(CommandeService.class,
                new CommandeService(
                        ServerConfig.getRepo(CommandeRepository.class),
                        ServerConfig.getRepo(LigneCommandeRepository.class),
                        ServerConfig.getRepo(ProduitRepository.class)
                ));
        ServerConfig.registerService(PaiementService.class,
                new PaiementService(
                        ServerConfig.getRepo(PaiementRepository.class),
                        ServerConfig.getService(CommandeService.class)
                ));
        ServerConfig.registerService(AdresseService.class,
                new AdresseService(ServerConfig.getRepo(AdresseRepository.class)));

        //AdminService
        ServerConfig.registerService(AdminService.class, new AdminService(
                ServerConfig.getRepo(ProduitRepository.class),
                ServerConfig.getRepo(CommandeRepository.class),
                ServerConfig.getRepo(UtilisateurRepository.class),
                ServerConfig.getRepo(PaiementRepository.class),
                ServerConfig.getRepo(CategorieRepository.class)
        ));

        logger.info("Services enregistrés");
    }

    public static void registerControllers() {
        ServerConfig.registerController("Auth", new AuthController());
        ServerConfig.registerController("Admin", new AdminController( ));
        ServerConfig.registerController("Commande", new CommandeController());
        ServerConfig.registerController("Panier", new PanierController());
        ServerConfig.registerController("Produit", new ProduitController());
        ServerConfig.registerController("Test", new TestClientController());
        ServerConfig.registerController("Commande",  new CommandeController());
        ServerConfig.registerController("Paiement",  new PaiementController());
        ServerConfig.registerController("Adresse", new AdresseController());
        ServerConfig.registerController("Categorie", new CategorieController());
        ServerConfig.registerController("KeyAuth", new KeyAuthController());
        logger.info("Controllers enregistrés");
    }



}