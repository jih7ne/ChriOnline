package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.core.theme.AppTheme;
import com.chrionline.chrionline.core.utils.JsonUtils;
import com.chrionline.chrionline.network.protocol.ApiResponse;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.tcp.TCPClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class LoginView extends VBox {

    private final TextField emailField;
    private final PasswordField passwordField;
    private final Button loginButton;
    private final Label errorLabel;
    private final TCPClient tcpClient;
    private final Consumer<String> onLoginSuccess; // ← String = token
    private final Runnable onGoToRegister;


    public LoginView(TCPClient tcpClient, Consumer<String> onLoginSuccess, Runnable onGoToRegister) {
        this.tcpClient = tcpClient;
        this.onLoginSuccess = onLoginSuccess;
        this.onGoToRegister = onGoToRegister;

        this.setAlignment(Pos.CENTER);
        this.setStyle("-fx-background-color: #FFFFFF;");
        this.setPadding(new Insets(60, 40, 60, 40));

        Text logo = new Text("ChriOnline");
        logo.setFont(Font.font("System", FontWeight.BOLD, 32));
        logo.setFill(Color.web("#FF385C"));
        Text subtitle = new Text("Connectez-vous à votre compte");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitle.setFill(Color.web("#717171"));
        VBox header = new VBox(8, logo, subtitle);
        header.setAlignment(Pos.CENTER);
        VBox.setMargin(header, new Insets(0, 0, 32, 0));

        emailField = new TextField();
        emailField.setPromptText("votre@email.com");
        AppTheme.styleTextField(emailField);
        AppTheme.styleFocusedTextField(emailField);

        passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        AppTheme.styleTextField(passwordField);
        passwordField.setOnAction(e -> handleLogin());

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #FF385C; -fx-font-size: 13px;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        loginButton = new Button("Se connecter");
        AppTheme.stylePrimaryButton(loginButton);
        loginButton.setOnAction(e -> handleLogin());
        VBox.setMargin(loginButton, new Insets(8, 0, 0, 0));

        Hyperlink registerLink = new Hyperlink("Pas de compte ? S'inscrire");
        registerLink.setStyle("-fx-text-fill: #FF385C; -fx-font-size: 13px; -fx-border-color: transparent;");
        registerLink.setOnAction(e -> onGoToRegister.run());
        HBox registerRow = new HBox(registerLink);
        registerRow.setAlignment(Pos.CENTER);
        VBox.setMargin(registerRow, new Insets(16, 0, 0, 0));

        Separator sep = new Separator();
        VBox.setMargin(sep, new Insets(16, 0, 0, 0));

        VBox card = new VBox(12,
                createLabel("Adresse e-mail"), emailField,
                createLabel("Mot de passe"), passwordField,
                errorLabel, loginButton, sep, registerRow
        );
        card.setMaxWidth(420);
        card.setPadding(new Insets(32));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px;" +
                "-fx-border-color: #EBEBEB; -fx-border-radius: 16px;");
        card.setEffect(new DropShadow(20, 0, 4, Color.rgb(0, 0, 0, 0.08)));

        this.getChildren().addAll(header, card);
    }

    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) { showError("Veuillez remplir tous les champs."); return; }
        if (!email.contains("@")) { showError("Adresse e-mail invalide."); return; }

        loginButton.setDisable(true);
        loginButton.setText("Connexion...");
        hideError();

        new Thread(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("email", email);
                payload.put("password", password);

                AppRequest request = new AppRequest.Builder()
                        .controller("Auth")
                        .action("login")
                        .payload(JsonUtils.toJson(payload))
                        .build();

                ApiResponse response = tcpClient.sendAndParse(request);

                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Se connecter");

                    if (response != null && response.isSuccess()) {
                        // ← AJOUT : extraire le token depuis data et le passer au callback
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = response.getDataAs(Map.class);
                        String token = data != null ? (String) data.get("token") : null;
                        onLoginSuccess.accept(token); // ← passe le token
                    } else {
                        String msg = response != null ? response.getMessage() : "Connexion échouée.";
                        showError(msg != null ? msg : "Connexion échouée. Vérifiez vos identifiants.");
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

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #222222;");
        return lbl;
    }

    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
    private void hideError() { errorLabel.setVisible(false); }
}