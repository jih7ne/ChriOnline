package com.chrionline.chrionline.server;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.network.tcp.TCPServer;
import com.chrionline.chrionline.server.controllers.*;
import com.chrionline.chrionline.server.data.mappers.AdresseRowMapper;
import com.chrionline.chrionline.server.data.mappers.CommandeRowMapper;
import com.chrionline.chrionline.server.data.mappers.LigneCommandeRowMapper;
import com.chrionline.chrionline.server.data.mappers.PaiementRowMapper;
import com.chrionline.chrionline.server.repositories.*;
import com.chrionline.chrionline.server.services.CommandeService;
import com.chrionline.chrionline.server.services.PaiementService;
import com.chrionline.chrionline.server.services.ProduitService;
import com.chrionline.chrionline.server.services.PanierService;
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
        AppConfig.registerService(ProduitService.class,
                new ProduitService(AppConfig.getRepo(ProduitRepository.class)));
        AppConfig.registerService(PanierService.class,
                new PanierService(
                        AppConfig.getRepo(PanierRepository.class),
                        AppConfig.getRepo(ProduitRepository.class)
                ));
        AppConfig.registerService(CommandeService.class,
                new CommandeService(
                        AppConfig.getRepo(CommandeRepository.class),
                        AppConfig.getRepo(LigneCommandeRepository.class),
                        AppConfig.getRepo(ProduitRepository.class)
                ));
        AppConfig.registerService(PaiementService.class,
                new PaiementService(
                        AppConfig.getRepo(PaiementRepository.class),
                        AppConfig.getService(CommandeService.class)
                ));

        logger.info("Services enregistrés");
    }

    public static void registerControllers() {
        AppConfig.registerController("Auth", new AuthController());
        AppConfig.registerController("Admin", new AdminController());
        AppConfig.registerController("Commande", new CommandeController());
        AppConfig.registerController("Panier", new PanierController());
        AppConfig.registerController("Produit", new ProduitController());
        AppConfig.registerController("Test", new TestClientController());
        AppConfig.registerController("Commande",  new CommandeController());
        AppConfig.registerController("Paiement",  new PaiementController());
        logger.info("Controllers enregistrés");
    }
}