package com.chrionline.chrionline.server;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.network.tcp.TCPServer;
import com.chrionline.chrionline.server.controllers.*;
import com.chrionline.chrionline.server.repositories.ProduitRepository;
import com.chrionline.chrionline.server.repositories.UtilisateurRepository;
import com.chrionline.chrionline.server.services.ProduitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class ServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) {
        try {
            logger.info("Démarrage du serveur ChriOnline...");
            registerRepositories();
            registerServices();
            registerControllers();
            logger.info("Démarrage TCP sur le port {}", AppConstants.SERVER_PORT);
            new TCPServer();
        } catch (Exception e) {
            logger.error("Échec du démarrage", e);
            System.exit(-1);
        }
    }

    private static void registerRepositories() throws SQLException {
        AppConfig.registerRepo(ProduitRepository.class,
                new ProduitRepository(AppConfig.getConnection()));
        AppConfig.registerRepo(UtilisateurRepository.class,
                new UtilisateurRepository(AppConfig.getConnection()));
        logger.info("Repositories enregistrés");
    }

    private static void registerServices() {
        AppConfig.registerService(ProduitService.class,
                new ProduitService(AppConfig.getRepo(ProduitRepository.class)));
        logger.info("Services enregistrés");
    }

    public static void registerControllers() {
        AppConfig.registerController("Auth", new AuthController());
        AppConfig.registerController("Admin", new AdminController());
        AppConfig.registerController("Commande", new CommandeController());
        AppConfig.registerController("Panier", new PanierController());
        AppConfig.registerController("Produit", new ProduitController());
        AppConfig.registerController("Test", new TestClientController());
        logger.info("Controllers enregistrés");
    }
}