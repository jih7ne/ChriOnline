package com.chrionline.chrionline.client;

import com.chrionline.chrionline.client.ui.views.*;
import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.core.interfaces.ViewManager;
import com.chrionline.chrionline.network.tcp.TCPClient;
import com.chrionline.chrionline.server.data.models.Produit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;

public class ClientApplication extends Application implements ViewManager {

    private static TCPClient client;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        Platform.setImplicitExit(true);
        showLoginView();
        AppConfig.getLogger().info("JavaFX Application started successfully");
    }

    //  LOGIN
    @Override
    public void showLoginView() {
        LoginView view = new LoginView(
                client,
                userData -> {
                    String token = (String) userData.get("token");
                    String role  = (String) userData.get("role");
                    client.setAuthToken(token);
                    if ("admin".equals(role)) {
                        showAdminView(userData);
                    } else {
                        showCatalogueView(userData);
                    }
                },
                this::showRegisterView
        );
        primaryStage.setTitle("ChriOnline — Connexion");
        primaryStage.setScene(new Scene(view, 900, 700));
        primaryStage.show();
    }

    //  REGISTER
    @Override
    public void showRegisterView() {
        RegisterView view = new RegisterView(
                client,
                this::showLoginView,
                this::showLoginView
        );
        primaryStage.setTitle("ChriOnline — Inscription");
        primaryStage.setScene(new Scene(view, 900, 700));
    }

    // ===== CATALOGUE =====
    @Override
    public void showCatalogueView(Map<String, Object> userData) {
        CatalogueView view = new CatalogueView(client, userData, this);
        primaryStage.setTitle("ChriOnline — Catalogue");
        primaryStage.setScene(new Scene(view, 1100, 750));
    }

    //  PANIER
    @Override
    public void showPanierView(Map<String, Object> userData) {
        PanierView view = new PanierView(client, userData, this);
        primaryStage.setTitle("ChriOnline — Mon Panier");
        primaryStage.setScene(new Scene(view, 1100, 750));
    }

    // DETAILS PRODUIT
    @Override
    public void showDetailsProduit(Produit produit, Map<String, Object> userData) {
        // TODO : créer DetailsProduitView
        // Pour l'instant on reste sur le catalogue
        showCatalogueView(userData);
    }

    //  ADMIN
    @Override
    public void showAdminView(Map<String, Object> userData) {
        AdminView view = new AdminView(client, userData, this);
        primaryStage.setTitle("ChriOnline — Administration");
        primaryStage.setScene(new Scene(view, 1100, 750));
    }

    @Override
    public void stop() throws Exception {
        AppConfig.getLogger().info("Shutting down client application...");
        if (client != null && client.isConnected()) client.disconnect();
        super.stop();
    }

    public static void main(String[] args) {
        try {
            AppConfig.getLogger().info("Initializing TCP client...");
            client = new TCPClient();
            if (!client.isConnected()) throw new RuntimeException("Failed to connect to server");
            AppConfig.getLogger().info("Successfully connected to server");
            launch(args);
        } catch (IOException e) {
            AppConfig.getLogger().error("Failed to initialize client", e);
            System.err.println("Could not connect to server: " + e.getMessage());
            System.err.println("Make sure the server is running on " +
                    AppConstants.SERVER_HOST + ":" + AppConstants.SERVER_PORT);
            System.exit(1);
        }
    }
}