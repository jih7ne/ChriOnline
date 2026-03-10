package com.chrionline.chrionline.client;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.constants.AppConstants;
import com.chrionline.chrionline.core.utils.JsonUtils;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.tcp.TCPClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ClientApplication extends Application {

    private static TCPClient client;

    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        Platform.setImplicitExit(true);
        showMainView();

        AppConfig.getLogger().info("JavaFX Application started successfully");
    }

    private void showMainView() throws IOException {
        Label label = new Label("Press the button!");
        Button button = new Button("Say Hello");

        button.setOnAction(e -> label.setText("Hello, JavaFX!"));

        VBox root = new VBox(20, label, button);
        root.setStyle("-fx-alignment: center; -fx-padding: 40;");
        primaryStage.setTitle("Hello!");
        primaryStage.setScene(new Scene(root, 320, 240));
        primaryStage.show();
    }

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

            // Test register
            Map<String, String> payload = new HashMap<>();
            payload.put("nom", "Test");
            payload.put("prenom", "User");
            payload.put("email", "test@chrionline.com");
            payload.put("password", "azerty");

            AppRequest req = new AppRequest.Builder()
                    .controller("Auth")
                    .action("register")
                    .payload(JsonUtils.toJson(payload))
                    .build();

            System.out.println(client.sendRequest(req));

            Map<String, String> map = new HashMap<>();
            map.put("password", "123");
            map.put("email", "1@11.com");
            map.put("username", "admin");

            AppRequest appRequest = new AppRequest.Builder()
                    .controller("Test")
                    .action("test")
                    .payload(JsonUtils.toJson(map))
                    .build();

            System.out.println(client.sendRequest(appRequest));


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
