package com.chrionline.clientmodule.client;

import com.chrionline.clientmodule.client.ui.components.NotificationToast;
import com.chrionline.clientmodule.client.ui.views.*;
import com.chrionline.core.constants.AppConstants;
import com.chrionline.core.interfaces.ViewManager;
import com.chrionline.network.tcp.TCPClient;
import com.chrionline.network.udp.UDPNotificationListener;
import com.chrionline.shared.models.PanierProduit;
import com.chrionline.shared.models.Produit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ClientApplication extends Application implements ViewManager {

    private static final Logger logger = LoggerFactory.getLogger(ClientApplication.class);

    // ── Static — shared between main() thread and the JavaFX instance ─────────
    private static TCPClient               client;
    private static UDPNotificationListener udpListener;
    private static ExecutorService         listenerHandlerExecutor;

    /**
     * Static volatile so the UDP handler (background thread, set up in main())
     * can reach the StackPane created by the JavaFX instance in start().
     */
    private static volatile StackPane rootStack;

    // ── Instance ───────────────────────────────────────────────────────────────
    private Stage primaryStage;

    // ─── JavaFX entry point ───────────────────────────────────────────────────

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        Platform.setImplicitExit(true);

        rootStack = new StackPane();

        LoginView loginView = new LoginView(
                client,
                userData -> onLoginSuccess(userData),
                this::showRegisterView,
                this::showForgotPasswordView
        );
        rootStack.getChildren().add(loginView);

        primaryStage.setTitle("ChriOnline — Connexion");
        primaryStage.setScene(new Scene(rootStack, 900, 700));
        primaryStage.show();

        logger.info("JavaFX Application started successfully");
    }

    // ─── Login success handler (shared by Login and Register) ─────────────────

    /**
     * Called whenever a user successfully logs in.
     * Sets the auth token, re-registers UDP with the real userId so the server
     * can route targeted notifications to this client only.
     */
    private void onLoginSuccess(Map<String, Object> userData) {
        String token = (String) userData.get("token");
        String role  = (String) userData.get("role");
        client.setAuthToken(token);

        // Re-register UDP with the actual userId so the server knows
        // which UDP port belongs to this user
        if (udpListener != null && userData.get("id") != null) {
            int userId = ((Number) userData.get("id")).intValue();
            new Thread(() -> udpListener.registerWithServer(userId)).start();
        }

        if ("admin".equals(role)) showAdminView(userData);
        else showCatalogueView(userData);
    }

    // ─── View swapping ────────────────────────────────────────────────────────

    private void setView(javafx.scene.Node view) {
        if (rootStack == null) return;
        if (!rootStack.getChildren().isEmpty()) {
            rootStack.getChildren().set(0, view);
        } else {
            rootStack.getChildren().add(0, view);
        }
    }

    // ─── ViewManager ──────────────────────────────────────────────────────────

    @Override
    public void showLoginView() {
        primaryStage.setTitle("ChriOnline — Connexion");
        setView(new LoginView(
                client,
                userData -> onLoginSuccess(userData),
                this::showRegisterView,
                this::showForgotPasswordView
        ));
    }

    @Override
    public void showRegisterView() {
        primaryStage.setTitle("ChriOnline — Inscription");
        setView(new RegisterView(client, this::showLoginView, this::showLoginView));
    }

    @Override
    public void showForgotPasswordView() {
        primaryStage.setTitle("ChriOnline — Mot de passe oublié");
        setView(new ForgotPasswordView(client, this::showLoginView));
    }

    @Override
    public void showAdminDashboard(){
        primaryStage.setTitle("Admin Dashboard");
        setView(new AdminDashboardView(client, this));
    }

    @Override
    public void showProfileView(Map<String, Object> userData) {
        primaryStage.setTitle("ChriOnline — Mon Profil");
        setView(new ProfileView(client, userData, this));
    }

    @Override
    public void showCatalogueView(Map<String, Object> userData) {
        primaryStage.setTitle("ChriOnline — Catalogue");
        setView(new CatalogueView(client, userData, this));
    }

    @Override
    public void showPanierView(Map<String, Object> userData) {
        primaryStage.setTitle("ChriOnline — Mon Panier");
        setView(new PanierView(client, userData, this));
    }

    @Override
    public void showAdminView(Map<String, Object> userData) {
        primaryStage.setTitle("ChriOnline — Administration");
        setView(new AdminView(client, userData, this));
    }

    @Override
    public void showDetailsProduit(Produit produit, Map<String, Object> userData) {
        primaryStage.setTitle("ChriOnline — " + produit.getNom());
        setView(new DetailsProduitView(client, produit, userData, this));
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

        primaryStage.setTitle("ChriOnline — Paiement");
        setView(new CheckoutView(
                client, lignes, userData, (ViewManager) this,
                paiementData -> showConfirmationView(paiementData),
                () -> showPanierView(userData)
        ));
    }

    @Override
    public void showCheckoutViewForExisting(Map<String, Object> userData, List<PanierProduit> panierItems, int idCommande, String uuidCommande) {
        List<Map<String, Object>> lignes = panierItems.stream().map(item -> {
            Map<String, Object> ligne = new HashMap<>();
            ligne.put("id_produit",    item.getIdProduit());
            ligne.put("nom",           item.getNomProduit());
            ligne.put("quantite",      item.getQuantite());
            ligne.put("prix_unitaire", item.getPrix());
            return ligne;
        }).collect(Collectors.toList());

        primaryStage.setTitle("ChriOnline — Paiement");
        setView(new CheckoutView(
                client, lignes, userData, (ViewManager) this,
                paiementData -> showConfirmationView(paiementData),
                () -> showHistoriqueCommandesView(userData),
                idCommande, uuidCommande
        ));
    }

    @Override
    public void showConfirmationView(Map<String, Object> paiementData) {
        @SuppressWarnings("unchecked")
        Map<String, Object> userData = (Map<String, Object>) paiementData.get("userData");
        primaryStage.setTitle("ChriOnline — Confirmation");
        setView(new ConfirmationView(
                paiementData,
                () -> showHistoriqueCommandesView(userData),
                () -> showCatalogueView(userData)
        ));
    }

    @Override
    public void showConfirmationEchoueeView(Map<String, Object> userData,
                                            String messageErreur, Runnable onReessayer) {
        primaryStage.setTitle("ChriOnline — Paiement échoué");
        setView(ConfirmationView.echouee(
                messageErreur, onReessayer, () -> showCatalogueView(userData)));
    }

    @Override
    public void showHistoriqueCommandesView(Map<String, Object> userData) {
        primaryStage.setTitle("ChriOnline — Historique des Commandes");
        setView(new HistoriqueCommandesView(
                client, userData, () -> showCatalogueView(userData), this));
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void stop() throws Exception {
        logger.info("Shutting down client application...");
        if (udpListener             != null) udpListener.close();
        if (listenerHandlerExecutor != null) listenerHandlerExecutor.shutdown();
        if (client != null && client.isConnected()) client.disconnect();
        super.stop();
    }

    // ─── UDP setup ────────────────────────────────────────────────────────────

    private static void setupUdpServices() throws Exception {
        logger.info("--- Setting up UDP Services ---");
        listenerHandlerExecutor = Executors.newFixedThreadPool(2);
        udpListener = new UDPNotificationListener();

        udpListener.setNotificationHandler(notification -> {
            logger.info("UDP notification received: {}", notification.getMessage());
            Platform.runLater(() -> {
                if (rootStack != null) {
                    NotificationToast.show(rootStack, notification);
                }
            });
        }, listenerHandlerExecutor);

        udpListener.startListening();
        Thread.sleep(500);
        logger.info("UDP client initialized");
    }

    // ─── Entry point ─────────────────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            logger.info("Initializing TCP client...");
            client = new TCPClient();
            logger.info("TCP client initialized");

            setupUdpServices();

            if (!client.isConnected())
                throw new RuntimeException("Failed to connect to server");
            logger.info("Successfully connected to server");

            launch(args);

        } catch (IOException e) {
            logger.error("Failed to initialize client", e);
            System.err.println("Could not connect to server: " + e.getMessage());
            System.err.println("Make sure the server is running on " +
                    AppConstants.SERVER_HOST + ":" + AppConstants.SERVER_PORT);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Failed to initialize client", e);
            throw new RuntimeException(e);
        }
    }
}