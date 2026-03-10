package com.chrionline.chrionline.client;

import com.chrionline.chrionline.core.config.AppConfig;
import com.chrionline.chrionline.core.config.ClientConfig;
import com.chrionline.chrionline.core.interfaces.ConfigAware;
import com.chrionline.chrionline.core.interfaces.ViewManager;
import javafx.application.Application;
import javafx.application.Platform;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;


public class ClientApplication extends Application implements ViewManager {

    private ClientConfig clientConfig;

    private Stage primaryStage;

    @Override
    public  void init() throws Exception {
        clientConfig = ClientConfig.getInstance();
        clientConfig.initialize();
        AppConfig.getLogger().info("Client application initialized");
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        Platform.setImplicitExit(true);
        showView("hello-view.fxml", "Main");

        AppConfig.getLogger().info("JavaFX Application started successfully");
    }



    public void showView(String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

        loader.setControllerFactory(controllerClass -> {
            try {
                Object controller = controllerClass.getDeclaredConstructor().newInstance();
                if (controller instanceof ConfigAware aware) {
                    aware.setClientConfig(clientConfig);
                    aware.setViewManager(this);
                }
                return controller;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create controller: " + controllerClass, e);
            }
        });

        Scene scene = new Scene(loader.load());
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        AppConfig.getLogger().info("Shutting down client application...");

        if (clientConfig != null) {
            clientConfig.shutdown();
        }

        super.stop();
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
