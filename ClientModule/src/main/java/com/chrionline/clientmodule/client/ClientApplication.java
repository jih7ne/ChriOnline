package com.chrionline.clientmodule.client;

import com.chrionline.clientmodule.client.ui.components.NotificationToast;
import com.chrionline.clientmodule.client.ui.views.*;
import com.chrionline.clientmodule.core.ClientViewManager;
import com.chrionline.core.constants.AppConstants;
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

public class ClientApplication extends Application implements ClientViewManager {

    private static final Logger                 logger = LoggerFactory.getLogger(ClientApplication.class);
    private static TCPClient                    client;
    private static UDPNotificationListener      udpListener;
    private static ExecutorService              listenerHandlerExecutor;
    private static volatile StackPane           rootStack;
    private Stage                               primaryStage;



    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        Platform.setImplicitExit(true);
        rootStack = new StackPane();

        rootStack.getChildren().add(new LoginView(
                client,
                this::onLoginSuccess,
                this::showRegisterView,
                this::showForgotPasswordView
        ));

        primaryStage.setTitle("ChriOnline — Connexion");
        primaryStage.setScene(new Scene(rootStack, 900, 700));
        primaryStage.show();
        logger.info("ClientApplication started");
    }



    private void onLoginSuccess(Map<String, Object> userData) {
        if (Boolean.TRUE.equals(userData.get("requires2FA"))) {
            String tempToken = (String) userData.get("tempToken");
            Platform.runLater(() -> showTwoFactorView(tempToken));
            return;
        }
        handleAuthSuccess(userData);
    }

    private void handleAuthSuccess(Map<String, Object> userData) {
        String token = (String) userData.get("token");
        client.setAuthToken(token);

        if (udpListener != null && userData.get("id") != null) {
            int userId = ((Number) userData.get("id")).intValue();
            new Thread(() -> udpListener.registerWithServer(userId)).start();
        }

        showCatalogueView(userData);
    }


    private void setView(javafx.scene.Node view) {
        if (rootStack == null) return;
        if (!rootStack.getChildren().isEmpty()) rootStack.getChildren().set(0, view);
        else                                    rootStack.getChildren().add(0, view);
    }

    @Override
    public void showLoginView() {
        primaryStage.setTitle("ChriOnline — Connexion");
        setView(new LoginView(client, this::onLoginSuccess, this::showRegisterView, this::showForgotPasswordView));
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
    public void showProfileView(Map<String, Object> userData) {
        primaryStage.setTitle("ChriOnline — Mon Profil");
        setView(new ProfileView(client, userData, this));
    }

    @Override
    public void showDetailsProduit(Produit produit, Map<String, Object> userData) {
        primaryStage.setTitle("ChriOnline — " + produit.getNom());
        setView(new DetailsProduitView(client, produit, userData, this));
    }

    @Override
    public void showCheckoutView(Map<String, Object> userData, List<PanierProduit> panierItems) {
        primaryStage.setTitle("ChriOnline — Paiement");
        setView(new CheckoutView(
                client, tolignes(panierItems), userData, this,
                this::showConfirmationView,
                () -> showPanierView(userData)
        ));
    }

    @Override
    public void showCheckoutViewForExisting(Map<String, Object> userData, List<PanierProduit> panierItems,
                                            int idCommande, String uuidCommande) {
        primaryStage.setTitle("ChriOnline — Paiement");
        setView(new CheckoutView(
                client, tolignes(panierItems), userData, this,
                this::showConfirmationView,
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
        setView(ConfirmationView.echouee(messageErreur, onReessayer, () -> showCatalogueView(userData)));
    }

    @Override
    public void showHistoriqueCommandesView(Map<String, Object> userData) {
        primaryStage.setTitle("ChriOnline — Historique des Commandes");
        setView(new HistoriqueCommandesView(client, userData, () -> showCatalogueView(userData), this));
    }



    private void showTwoFactorView(String tempToken) {
        primaryStage.setTitle("ChriOnline — Vérification 2FA");
        setView(new TwoFactorView(
                client,
                tempToken,
                data -> Platform.runLater(() -> handleAuthSuccess(data)),
                ()   -> Platform.runLater(this::showLoginView)
        ));
    }



    @Override
    public void stop() throws Exception {
        logger.info("Shutting down ClientApplication...");
        if (udpListener             != null) udpListener.close();
        if (listenerHandlerExecutor != null) listenerHandlerExecutor.shutdown();
        if (client != null && client.isConnected()) client.disconnect();
        super.stop();
    }



    private static void setupUdpServices() throws Exception {
        logger.info("Setting up UDP services...");
        listenerHandlerExecutor = Executors.newFixedThreadPool(2);
        udpListener = new UDPNotificationListener();

        udpListener.setNotificationHandler(notification -> {
            logger.info("UDP notification received: {}", notification.getMessage());
            Platform.runLater(() -> {
                if (rootStack != null) NotificationToast.show(rootStack, notification);
            });
        }, listenerHandlerExecutor);

        udpListener.startListening();
        Thread.sleep(500);
        logger.info("UDP services ready");
    }



    private static List<Map<String, Object>> tolignes(List<PanierProduit> items) {
        return items.stream().map(item -> {
            Map<String, Object> ligne = new HashMap<>();
            ligne.put("id_produit",    item.getIdProduit());
            ligne.put("nom",           item.getNomProduit());
            ligne.put("quantite",      item.getQuantite());
            ligne.put("prix_unitaire", item.getPrix());
            return ligne;
        }).collect(Collectors.toList());
    }



    public static void main(String[] args) {
        try {
            logger.info("Initializing TCP client...");
            client = new TCPClient();
            logger.info("TCP client initialized");

            setupUdpServices();

            if (!client.isConnected())
                throw new RuntimeException("Failed to connect to server");

            launch(args);

        } catch (IOException e) {
            logger.error("Failed to connect to server", e);
            System.err.println("Could not connect to server: " + e.getMessage());
            System.err.println("Make sure the server is running on " +
                    AppConstants.SERVER_HOST + ":" + AppConstants.SERVER_PORT);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Unexpected startup failure", e);
            throw new RuntimeException(e);
        }
    }
}