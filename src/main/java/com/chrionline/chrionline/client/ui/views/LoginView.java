package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.core.theme.AppTheme;
import com.chrionline.chrionline.core.utils.JsonUtils;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.tcp.TCPClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class LoginView extends StackPane {

    private final TextField emailField;
    private final PasswordField passwordField;
    private final Button loginButton;
    private final Label errorLabel;
    private final TCPClient tcpClient;
    private final Consumer<Map<String, Object>> onLoginSuccess; // passe toutes les données user
    private final Runnable onGoToRegister;

    public LoginView(TCPClient tcpClient,
                     Consumer<Map<String, Object>> onLoginSuccess,
                     Runnable onGoToRegister) {
        this.tcpClient = tcpClient;
        this.onLoginSuccess = onLoginSuccess;
        this.onGoToRegister = onGoToRegister;

        // ─── Fond beige ────────────────────────────────────────────────────
        this.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        // ─── Card centrale ─────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setMaxWidth(480);
        card.setMaxHeight(Double.MAX_VALUE);
        AppTheme.styleCard(card);
        card.setPadding(new Insets(40, 40, 40, 40));

        // ─── Icône sac ─────────────────────────────────────────────────────
        Label icon = new Label("🛍");
        icon.setStyle("-fx-font-size: 40px;");
        VBox iconBox = new VBox(icon);
        iconBox.setAlignment(Pos.CENTER);
        VBox.setMargin(iconBox, new Insets(0, 0, 8, 0));

        // ─── Titre + subtitle ──────────────────────────────────────────────
        Label title = new Label("ChriOnline");
        title.setStyle(
                "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";"
        );
        Label subtitle = new Label("Boutique artisanale");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        VBox titleBox = new VBox(4, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);
        VBox.setMargin(titleBox, new Insets(0, 0, 24, 0));

        // ─── Toggle Connexion / Inscription ────────────────────────────────
        Button btnConnexion = new Button("Connexion");
        Button btnInscription = new Button("Inscription");
        AppTheme.styleToggleActive(btnConnexion);
        AppTheme.styleToggleInactive(btnInscription);
        btnInscription.setOnAction(e -> onGoToRegister.run());
        btnConnexion.setMaxWidth(Double.MAX_VALUE);
        btnInscription.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnConnexion, Priority.ALWAYS);
        HBox.setHgrow(btnInscription, Priority.ALWAYS);

        HBox toggle = new HBox(0, btnConnexion, btnInscription);
        toggle.setStyle(
                "-fx-background-color: " + AppTheme.TOGGLE_INACTIVE + ";" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 4px;"
        );
        toggle.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(toggle, new Insets(0, 0, 24, 0));

        // ─── Champ Email ───────────────────────────────────────────────────
        emailField = new TextField();
        emailField.setPromptText("votre@email.com");
        AppTheme.styleTextField(emailField);
        AppTheme.styleFocusedTextField(emailField);
        StackPane emailPane = wrapWithIcon("✉", emailField);
        VBox.setMargin(emailPane, new Insets(0, 0, 14, 0));

        // ─── Champ Mot de passe ────────────────────────────────────────────
        passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        AppTheme.styleTextField(passwordField);
        AppTheme.styleFocusedTextField(passwordField);
        passwordField.setOnAction(e -> handleLogin());
        StackPane passPane = wrapWithIcon("🔒", passwordField);
        VBox.setMargin(passPane, new Insets(0, 0, 6, 0));

        // ─── Mot de passe oublié ───────────────────────────────────────────
        Hyperlink forgot = new Hyperlink("Mot de passe oublié?");
        forgot.setStyle(
                "-fx-text-fill: " + AppTheme.TEXT_MUTED + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-border-color: transparent;"
        );
        HBox forgotRow = new HBox(forgot);
        forgotRow.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(forgotRow, new Insets(0, 0, 18, 0));

        // ─── Erreur ────────────────────────────────────────────────────────
        errorLabel = new Label();
        errorLabel.setStyle(
                "-fx-text-fill: " + AppTheme.ERROR_COLOR + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 0 0 8 0;"
        );
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        // ─── Bouton Se connecter ───────────────────────────────────────────
        loginButton = new Button("Se connecter");
        AppTheme.stylePrimaryButton(loginButton);
        loginButton.setOnAction(e -> handleLogin());
        VBox.setMargin(loginButton, new Insets(0, 0, 0, 0));

        // ─── Assemblage card ───────────────────────────────────────────────
        card.getChildren().addAll(
                iconBox, titleBox, toggle,
                createFieldLabel("Email"), emailPane,
                createFieldLabel("Mot de passe"), passPane,
                forgotRow, errorLabel, loginButton
        );

        StackPane.setAlignment(card, Pos.CENTER);
        this.getChildren().add(card);
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
                        .controller("Auth").action("login")
                        .payload(JsonUtils.toJson(payload))
                        .build();

                AppResponse response = tcpClient.sendAndParse(request);

                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Se connecter");

                    if (response != null && response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = response.getDataAs(Map.class);
                        if (data != null) {
                            onLoginSuccess.accept(data); // passe role + token + tout
                        }
                    } else {
                        showError(response != null && response.getMessage() != null
                                ? response.getMessage()
                                : "Connexion échouée. Vérifiez vos identifiants.");
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

    // ─── Helpers UI ────────────────────────────────────────────────────────

    private Label createFieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + AppTheme.TEXT_MAIN + ";" +
                        "-fx-padding: 0 0 4 4;"
        );
        VBox.setMargin(lbl, new Insets(4, 0, 4, 0));
        return lbl;
    }

    private StackPane wrapWithIcon(String emoji, Control field) {
        Label iconLabel = new Label(emoji);
        iconLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        StackPane pane = new StackPane(field, iconLabel);
        StackPane.setAlignment(iconLabel, Pos.CENTER_LEFT);
        iconLabel.setTranslateX(14);
        field.setMaxWidth(Double.MAX_VALUE);
        return pane;
    }

    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
    private void hideError() { errorLabel.setVisible(false); }
}