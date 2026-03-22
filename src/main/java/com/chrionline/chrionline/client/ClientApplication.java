package com.chrionline.chrionline.client;

import com.chrionline.chrionline.client.ui.views.*;
import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.core.interfaces.ViewManager;
import com.chrionline.chrionline.network.protocol.AppNotification;
import com.chrionline.chrionline.network.tcp.TCPClient;
import com.chrionline.chrionline.network.udp.UDPNotificationListener;
import com.chrionline.chrionline.server.data.models.PanierProduit;
import com.chrionline.chrionline.server.data.models.Produit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ClientApplication extends Application implements ViewManager {

    private static TCPClient client;
    private static UDPNotificationListener udpListener;
    private static ExecutorService listenerHandlerExecutor;
    private static List<AppNotification> notifications = new ArrayList<>();

    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        Platform.setImplicitExit(true);

        LoginView loginView = new LoginView(
                client,
                userData -> {
                    String token = (String) userData.get("token");
                    String role  = (String) userData.get("role");
                    client.setAuthToken(token);
                    if ("admin".equals(role)) showAdminView(userData);
                    else showCatalogueView(userData);
                },
                this::showRegisterView
        );
        primaryStage.setTitle("ChriOnline — Connexion");
        primaryStage.setScene(new Scene(loginView, 900, 700));
        primaryStage.show();

        AppConfig.getLogger().info("JavaFX Application started successfully");
    }

    @Override
    public void showLoginView() {
        LoginView view = new LoginView(
                client,
                userData -> {
                    String token = (String) userData.get("token");
                    String role  = (String) userData.get("role");
                    client.setAuthToken(token);
                    if ("admin".equals(role)) showAdminView(userData);
                    else showCatalogueView(userData);
                },
                this::showRegisterView
        );
        primaryStage.setTitle("ChriOnline — Connexion");
        if (primaryStage.getScene() == null) {
            primaryStage.setScene(new Scene(view, 900, 700));
        } else {
            primaryStage.getScene().setRoot(view);
        }
        primaryStage.show();
    }

    @Override
    public void showRegisterView() {
        RegisterView view = new RegisterView(
                client,
                this::showLoginView,
                this::showLoginView
        );
        primaryStage.setTitle("ChriOnline — Inscription");
        primaryStage.getScene().setRoot(view);
    }

    @Override
    public void showAdminDashboard(){
        AdminDashboardView adminDashboardView = new AdminDashboardView(client);
        primaryStage.setTitle("Admin Dashboard");
        primaryStage.getScene().setRoot(adminDashboardView);
    }

    @Override
    public void showProfileView(Map<String, Object> userData) {
        ProfileView view = new ProfileView(client, userData, this);
        primaryStage.setTitle("ChriOnline — Mon Profil");
        primaryStage.getScene().setRoot(view);
    }

    @Override
    public void showCatalogueView(Map<String, Object> userData) {
        CatalogueView view = new CatalogueView(client, userData, this);
        primaryStage.setTitle("ChriOnline — Catalogue");
        primaryStage.getScene().setRoot(view);
    }

    @Override
    public void showPanierView(Map<String, Object> userData) {
        PanierView view = new PanierView(client, userData, this);
        primaryStage.setTitle("ChriOnline — Mon Panier");
        primaryStage.getScene().setRoot(view);
    }

    @Override
    public void showAdminView(Map<String, Object> userData) {
        AdminView view = new AdminView(client, userData, this);
        primaryStage.setTitle("ChriOnline — Administration");
        primaryStage.getScene().setRoot(view);
    }

    @Override
    public void showDetailsProduit(Produit produit, Map<String, Object> userData) {
        DetailsProduitView view = new DetailsProduitView(client, produit, userData, this);
        primaryStage.setTitle("ChriOnline — " + produit.getNom());
        primaryStage.getScene().setRoot(view);
    }

    @Override
    public void showCheckoutView(Map<String, Object> userData, List<PanierProduit> panierItems) {
        List<Map<String, Object>> lignes = panierItems.stream().map(item -> {
            Map<String, Object> ligne = new HashMap<>();
            ligne.put("id_produit",    item.getIdProduit());
            ligne.put("nom",           item.getNomProduit());
            ligne.put("quantite",      item.getQuantite());
            ligne.put("prix_unitaire", item.getPrix());
            return ligne;
        }).collect(Collectors.toList());

        CheckoutView view = new CheckoutView(
                client,
                lignes,
                userData,
                this,
                paiementData -> showConfirmationView(paiementData),
                () -> showPanierView(userData)
        );
        primaryStage.setTitle("ChriOnline — Paiement");
        primaryStage.getScene().setRoot(view);
    }

    @Override
    public void showConfirmationView(Map<String, Object> paiementData) {
        @SuppressWarnings("unchecked")
        Map<String, Object> userData = (Map<String, Object>) paiementData.get("userData");
        ConfirmationView view = new ConfirmationView(
                paiementData,
                () -> showHistoriqueCommandesView(userData),
                () -> showCatalogueView(userData)
        );
        primaryStage.setTitle("ChriOnline — Confirmation");
        primaryStage.getScene().setRoot(view);
    }

    @Override
    public void showConfirmationEchoueeView(Map<String, Object> userData, String messageErreur, Runnable onReessayer) {
        ConfirmationView view = ConfirmationView.echouee(
                messageErreur,
                onReessayer,
                () -> showCatalogueView(userData)
        );
        primaryStage.setTitle("ChriOnline — Paiement échoué");
        primaryStage.getScene().setRoot(view);
    }

    @Override
    public void showHistoriqueCommandesView(Map<String, Object> userData) {
        HistoriqueCommandesView view = new HistoriqueCommandesView(
                client, userData, () -> showCatalogueView(userData), this
        );
        primaryStage.setTitle("ChriOnline — Historique des Commandes");
        primaryStage.getScene().setRoot(view);
    }



    @Override
    public void stop() throws Exception {
        AppConfig.getLogger().info("Shutting down client application...");
        if (client != null && client.isConnected()) client.disconnect();
        super.stop();
    }


    private static void setupUdpServices() throws Exception {
        AppConfig.getLogger().info("--- Setting up UDP Services ---");
        // Client listener
        listenerHandlerExecutor = Executors.newFixedThreadPool(2);
        udpListener = new UDPNotificationListener();
        udpListener.setNotificationHandler(notification -> {
            AppConfig.getLogger().debug("Client received notification: {}", notification.getMessage());
            notifications.add(notification);
        }, listenerHandlerExecutor);
        udpListener.startListening();

        Thread.sleep(500);

    }


    public static void main(String[] args) {
        try {
            AppConfig.getLogger().info("Initializing TCP client...");
            client = new TCPClient();
            AppConfig.getLogger().info("TCP client initialized");
            AppConfig.getLogger().info("Initializing UDP client...");
            setupUdpServices();
            AppConfig.getLogger().info("UDP client initialized");
            if (!client.isConnected()) throw new RuntimeException("Failed to connect to server");
            AppConfig.getLogger().info("Successfully connected to server");
            launch(args);
        } catch (IOException e) {
            AppConfig.getLogger().error("Failed to initialize client", e);
            System.err.println("Could not connect to server: " + e.getMessage());
            System.err.println("Make sure the server is running on " +
                    AppConstants.SERVER_HOST + ":" + AppConstants.SERVER_PORT);
            System.exit(1);
        } catch (Exception e) {
            AppConfig.getLogger().error("Failed to initialize client", e);
            throw new RuntimeException(e);
        }
    }
}