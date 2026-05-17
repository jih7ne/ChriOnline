package com.chrionline.adminmodule.admin;

import com.chrionline.adminmodule.admin.ui.views.AdminDashboardView;
import com.chrionline.adminmodule.admin.ui.views.AdminLogin;
import com.chrionline.adminmodule.admin.ui.views.AdminView;
import com.chrionline.adminmodule.core.AdminViewManager;
import com.chrionline.core.constants.AppConstants;
import com.chrionline.network.tcp.TCPClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.security.Security;
import java.util.Map;

public class AdminApplication extends Application implements AdminViewManager {

    private static final Logger         logger = LoggerFactory.getLogger(AdminApplication.class);
    private static TCPClient            client;
    private Stage                       primaryStage;
    private StackPane                   rootStack;


    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        Platform.setImplicitExit(true);
        rootStack = new StackPane();

        rootStack.getChildren().add(new AdminLogin(client, this::onLoginSuccess));

        primaryStage.setTitle("ChriOnline — Administration");
        primaryStage.setScene(new Scene(rootStack, 900, 700));
        primaryStage.show();
        logger.info("AdminApplication started");
    }



    private void onLoginSuccess(Map<String, Object> userData) {
        String token = (String) userData.get("token");
        String role  = (String) userData.get("role");
        client.setAuthToken(token);
        if ("admin".equals(role)) showAdminView(userData);
    }



    private void setView(javafx.scene.Node view) {
        if (!rootStack.getChildren().isEmpty()) rootStack.getChildren().set(0, view);
        else                                    rootStack.getChildren().add(0, view);
    }

    public void showLoginView() {
        primaryStage.setTitle("ChriOnline — Administration");
        setView(new AdminLogin(client, this::onLoginSuccess));
    }

    @Override
    public void showAdminLoginView() {
        showLoginView();
    }

    private void showDashboard() {
        primaryStage.setTitle("ChriOnline — Dashboard Admin");
        setView(new AdminDashboardView(client, this));
    }



    @Override
    public void stop() throws Exception {
        logger.info("Shutting down AdminApplication...");
        if (client != null && client.isConnected()) client.disconnect();
        super.stop();
    }



    public static void main(String[] args) {
        try {
            logger.info("Initializing TCP client...");
            client = new TCPClient();
            logger.info("TCP client initialized");

            if (!client.isConnected())
                throw new RuntimeException("Failed to connect to server");

            Security.addProvider(new BouncyCastleProvider());
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



    @Override
    public void showAdminView(Map<String, Object> userData) {
        primaryStage.setTitle("ChriOnline — Administration");
        setView(new AdminView(client, userData, this));
    }

    @Override
    public void showAdminDashboard() {

    }
}
