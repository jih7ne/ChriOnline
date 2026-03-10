package com.chrionline.chrionline.client;

import com.chrionline.chrionline.client.ui.views.LoginView;
import com.chrionline.chrionline.client.ui.views.RegisterView;
import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.network.tcp.TCPClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

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
                token -> {

                    client.setAuthToken(token);
                    AppConfig.getLogger().info("Login successful, token stored: {}", token);
                    showWelcomeView(token);
                },
                this::showRegisterView
        );
        primaryStage.setTitle("ChriOnline — Connexion");
        primaryStage.setScene(new Scene(view, 900, 650));
        primaryStage.show();
    }

    private void showRegisterView() {
        RegisterView view = new RegisterView(
                client,
                this::showLoginView,   // inscription réussie → retour login
                this::showLoginView    // bouton "Déjà un compte ?"
        );
        primaryStage.setTitle("ChriOnline — Inscription");
        primaryStage.setScene(new Scene(view, 900, 700));
    }

    private void showWelcomeView(String token) {

        javafx.scene.control.Label label = new javafx.scene.control.Label(
                " Connecté ! Token : " + token
        );
        label.setStyle("-fx-font-size: 14px; -fx-padding: 40;");
        primaryStage.setTitle("ChriOnline — Accueil");
        primaryStage.setScene(new Scene(label, 900, 600));
    }

    // ─── Lifecycle ────────────────────────────────────────────────

    @Override
    public void stop() throws Exception {
        AppConfig.getLogger().info("Shutting down client application...");
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
        super.stop();
    }

    public static void main(String[] args) {
        try {
            AppConfig.getLogger().info("Initializing TCP client...");
            client = new TCPClient();

            if (!client.isConnected()) {
                throw new RuntimeException("Failed to connect to server");
            }

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