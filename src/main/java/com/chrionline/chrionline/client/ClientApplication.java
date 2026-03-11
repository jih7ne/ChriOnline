package com.chrionline.chrionline.client;

import com.chrionline.chrionline.client.ui.views.LoginView;
import com.chrionline.chrionline.client.ui.views.RegisterView;
import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.network.tcp.TCPClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;

public class ClientApplication extends Application {

    private static TCPClient client;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        Platform.setImplicitExit(true);
        showLoginView();
        AppConfig.getLogger().info("JavaFX Application started successfully");
    }

    private void showLoginView() {
        LoginView view = new LoginView(
                client,
                userData -> {
                    // userData contient : token, role, id, nom, prenom, email, statut
                    String token = (String) userData.get("token");
                    String role  = (String) userData.get("role");
                    client.setAuthToken(token);
                    AppConfig.getLogger().info("Login OK — role: {}, token: {}", role, token);
                    redirectByRole(role, userData);
                },
                this::showRegisterView
        );
        primaryStage.setTitle("ChriOnline — Connexion");
        primaryStage.setScene(new Scene(view, 900, 700));
        primaryStage.show();
    }

    private void showRegisterView() {
        RegisterView view = new RegisterView(
                client,
                this::showLoginView,
                this::showLoginView
        );
        primaryStage.setTitle("ChriOnline — Inscription");
        primaryStage.setScene(new Scene(view, 900, 700));
    }

    private void redirectByRole(String role, Map<String, Object> userData) {
        if ("admin".equals(role)) {
            showAdminView(userData);
        } else {
            showClientView(userData);
        }
    }

    private void showAdminView(Map<String, Object> userData) {
        // TODO Sprint 3 : remplacer par AdminView complète
        Label label = new Label(" Admin Dashboard — " + userData.get("nom") + " " + userData.get("prenom"));
        label.setStyle("-fx-font-size: 18px; -fx-padding: 40; -fx-text-fill: #3D2B1A;");
        primaryStage.setTitle("ChriOnline — Administration");
        primaryStage.setScene(new Scene(label, 1100, 700));
    }

    private void showClientView(Map<String, Object> userData) {
        // TODO Sprint 3 : remplacer par CatalogueView / ProfilView
        Label label = new Label(" Bienvenue " + userData.get("prenom") + " !");
        label.setStyle("-fx-font-size: 18px; -fx-padding: 40; -fx-text-fill: #3D2B1A;");
        primaryStage.setTitle("ChriOnline — Boutique");
        primaryStage.setScene(new Scene(label, 1100, 700));
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