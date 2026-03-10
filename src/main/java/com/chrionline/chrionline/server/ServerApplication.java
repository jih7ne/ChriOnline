package com.chrionline.chrionline.server;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.network.tcp.TCPServer;
import com.chrionline.chrionline.server.controllers.*;
import com.chrionline.chrionline.server.repositories.ProduitRepository;
import com.chrionline.chrionline.server.services.ProduitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class ServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) {
        try {

            logger.info("Starting Server application...");


            registerRepositories();


            registerServices();

            registerControllers();


            logger.info("Starting TCP Server on port {}", AppConstants.SERVER_PORT);
            new TCPServer();

        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(-1);
        }
    }

    private static void registerRepositories() throws SQLException {
        ProduitRepository produitRepo = new ProduitRepository(AppConfig.getConnection());
        AppConfig.registerRepo(ProduitRepository.class, produitRepo);
        logger.info("Repositories registered");
    }

    private static void registerServices() {

        ProduitRepository produitRepo = AppConfig.getRepo(ProduitRepository.class);
        ProduitService produitService = new ProduitService(produitRepo);

        AppConfig.registerService(ProduitService.class, produitService);
        logger.info("Services registered");
    }

    public static void registerControllers(){
        AppConfig.registerController("Auth", new AuthController());
        AppConfig.registerController("Admin", new AdminController());
        AppConfig.registerController("Commande", new CommandeController());
        AppConfig.registerController("Panier", new PanierController());
        AppConfig.registerController("Produit", new ProduitController());
    }

}
