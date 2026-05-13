package com.chrionline.clientmodule.client.ui.views;

import com.chrionline.core.theme.AppTheme;
import com.chrionline.core.utils.JsonUtils;
import com.chrionline.network.protocol.AppRequest;
import com.chrionline.network.protocol.AppResponse;
import com.chrionline.network.tcp.TCPClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TestAdminLoginView extends StackPane {

    private final TextField     emailField;
    private final PasswordField passwordField;
    private final Button        loginButton;
    private final Label         errorLabel;
    private final TCPClient     tcpClient;
    private final Consumer<Map<String, Object>> onLoginSuccess;

    public TestAdminLoginView(TCPClient tcpClient,
                              Consumer<Map<String, Object>> onLoginSuccess) {
        this.tcpClient      = tcpClient;
        this.onLoginSuccess = onLoginSuccess;

        setStyle("-fx-background-color: " + AppTheme.BG + ";");

        // ── Card ─────────────────────────────────────────────────────────
        VBox card = new VBox(16);
        card.setMaxWidth(420);
        AppTheme.styleCard(card);
        card.setPadding(new Insets(40));

        Label title = new Label("🔐  Admin — KeyAuth Test");
        title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:" + AppTheme.PRIMARY + ";");

        Label subtitle = new Label("Test de l'authentification par clé RSA");
        subtitle.setStyle("-fx-font-size:13px;-fx-text-fill:" + AppTheme.TEXT_MUTED + ";");

        // Email
        emailField = new TextField();
        emailField.setPromptText("admin@chrionline.com");
        AppTheme.styleTextField(emailField);
        emailField.setMaxWidth(Double.MAX_VALUE);

        // Password
        passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        AppTheme.styleTextField(passwordField);
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setOnAction(e -> handleLogin());

        // Error label
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill:" + AppTheme.ERROR_COLOR + ";-fx-font-size:13px;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        // Login button
        loginButton = new Button("Se connecter");
        AppTheme.stylePrimaryButton(loginButton);
        loginButton.setOnAction(e -> handleLogin());

        card.getChildren().addAll(
                title, subtitle,
                fieldBox("Email", emailField),
                fieldBox("Mot de passe", passwordField),
                errorLabel,
                loginButton
        );

        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(24));
        wrapper.setStyle("-fx-background-color:" + AppTheme.BG + ";");

        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:" + AppTheme.BG + ";-fx-background-color:" + AppTheme.BG + ";");
        getChildren().add(scroll);
    }

    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Connexion...");
        errorLabel.setVisible(false);

        new Thread(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("email",    email);
                payload.put("password", password);

                AppRequest request = new AppRequest.Builder()
                        .controller("Auth")
                        .action("testadminlogin")
                        .payload(JsonUtils.toJson(payload))
                        .build();

                AppResponse response = tcpClient.sendAndParse(request);

                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Se connecter");

                    if (response != null && response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = response.getDataAs(Map.class);
                        if (data != null) onLoginSuccess.accept(data);
                    } else {
                        showError(response != null ? response.getMessage()
                                : "Erreur de connexion.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Se connecter");
                    showError("Erreur réseau : " + e.getMessage());
                });
            }
        }).start();
    }

    private VBox fieldBox(String labelText, javafx.scene.Node field) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + AppTheme.TEXT_MAIN + ";");
        return new VBox(6, lbl, field);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}