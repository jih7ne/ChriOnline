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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

public class RegisterView extends StackPane {

    private final TextField prenomField, nomField, emailField;
    // ── C01 : nouveaux champs adresse ────────────────────────────────────────
    private final TextField rueField, villeField, codePostalField;
    // ─────────────────────────────────────────────────────────────────────────
    private final PasswordField passwordField;
    private final Button registerButton;
    private final Label errorLabel;
    private final TCPClient tcpClient;
    private final Runnable onRegisterSuccess;
    private final Runnable onGoToLogin;

    public RegisterView(TCPClient tcpClient, Runnable onRegisterSuccess, Runnable onGoToLogin) {
        this.tcpClient = tcpClient;
        this.onRegisterSuccess = onRegisterSuccess;
        this.onGoToLogin = onGoToLogin;

        this.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        // ── ScrollPane pour éviter le débordement avec les champs supplémentaires ──
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background: " + AppTheme.BG + ";" +
                        "-fx-background-color: " + AppTheme.BG + ";"
        );

        VBox card = new VBox(0);
        card.setMaxWidth(480);
        AppTheme.styleCard(card);
        card.setPadding(new Insets(40));

        // ── Icône ──────────────────────────────────────────────────────────────
        Label icon = new Label("🛍");
        icon.setStyle("-fx-font-size: 40px;");
        VBox iconBox = new VBox(icon);
        iconBox.setAlignment(Pos.CENTER);
        VBox.setMargin(iconBox, new Insets(0, 0, 8, 0));

        // ── Titre ──────────────────────────────────────────────────────────────
        Label title = new Label("ChriOnline");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");
        Label subtitle = new Label("Boutique artisanale");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        VBox titleBox = new VBox(4, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);
        VBox.setMargin(titleBox, new Insets(0, 0, 24, 0));

        // ── Toggle ─────────────────────────────────────────────────────────────
        Button btnConnexion = new Button("Connexion");
        Button btnInscription = new Button("Inscription");
        AppTheme.styleToggleInactive(btnConnexion);
        AppTheme.styleToggleActive(btnInscription);
        btnConnexion.setOnAction(e -> onGoToLogin.run());
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
        VBox.setMargin(toggle, new Insets(0, 0, 20, 0));

        // ── Champs identité ────────────────────────────────────────────────────
        prenomField = new TextField();
        prenomField.setPromptText("Prénom");
        AppTheme.styleTextField(prenomField);
        StackPane prenomPane = wrapWithIcon("👤", prenomField);

        nomField = new TextField();
        nomField.setPromptText("Nom");
        AppTheme.styleTextField(nomField);
        StackPane nomPane = wrapWithIcon("👤", nomField);

        emailField = new TextField();
        emailField.setPromptText("votre@email.com");
        AppTheme.styleTextField(emailField);
        StackPane emailPane = wrapWithIcon("✉", emailField);

        passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        AppTheme.styleTextField(passwordField);
        passwordField.setOnAction(e -> handleRegister());
        StackPane passPane = wrapWithIcon("🔒", passwordField);

        Separator sepAdresse = new Separator();
        sepAdresse.setStyle("-fx-background-color: " + AppTheme.FIELD_BORDER + ";");
        VBox.setMargin(sepAdresse, new Insets(16, 0, 8, 0));

        Label adresseTitre = new Label("Adresse de livraison");
        adresseTitre.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + AppTheme.PRIMARY + ";"
        );
        VBox.setMargin(adresseTitre, new Insets(0, 0, 8, 0));

        // Rue
        rueField = new TextField();
        rueField.setPromptText("Numéro et nom de rue");
        AppTheme.styleTextField(rueField);
        StackPane ruePane = wrapWithIcon("📍", rueField);

        // Ville + Code postal côte à côte
        codePostalField = new TextField();
        codePostalField.setPromptText("Code postal");
        AppTheme.styleTextField(codePostalField);
        codePostalField.setMaxWidth(Double.MAX_VALUE);

        villeField = new TextField();
        villeField.setPromptText("Ville");
        AppTheme.styleTextField(villeField);
        villeField.setMaxWidth(Double.MAX_VALUE);

        HBox cpVilleRow = new HBox(10, codePostalField, villeField);
        HBox.setHgrow(codePostalField, Priority.NEVER);
        HBox.setHgrow(villeField, Priority.ALWAYS);
        codePostalField.setPrefWidth(130);
        codePostalField.setMinWidth(110);
        codePostalField.setMaxWidth(150);
        cpVilleRow.setMaxWidth(Double.MAX_VALUE);

        errorLabel = new Label();
        errorLabel.setStyle(
                "-fx-text-fill: " + AppTheme.ERROR_COLOR + ";" +
                        "-fx-font-size: 13px;"
        );
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        // ── Bouton ─────────────────────────────────────────────────────────────
        registerButton = new Button("S'inscrire");
        AppTheme.stylePrimaryButton(registerButton);
        registerButton.setOnAction(e -> handleRegister());
        VBox.setMargin(registerButton, new Insets(8, 0, 0, 0));

        // ── Assemblage ─────────────────────────────────────────────────────────
        card.getChildren().addAll(
                iconBox, titleBox, toggle,
                createFieldLabel("Prénom"), prenomPane,
                createFieldLabel("Nom"), nomPane,
                createFieldLabel("Email"), emailPane,
                createFieldLabel("Mot de passe"), passPane,
                // C01 : section adresse
                sepAdresse,
                adresseTitre,
                createFieldLabel("Rue *"), ruePane,
                createFieldLabel("Code postal et ville *"), cpVilleRow,
                // fin C01
                errorLabel, registerButton
        );

        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(24));
        wrapper.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        scroll.setContent(wrapper);
        StackPane.setAlignment(scroll, Pos.CENTER);
        this.getChildren().add(scroll);
    }

    private void handleRegister() {
        String prenom = prenomField.getText().trim();
        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Remplissez tous les champs."); return;
        }
        if (!email.contains("@")) { showError("Email invalide."); return; }
        if (password.length() < 6) { showError("Mot de passe trop court (min. 6 caractères)."); return; }

        registerButton.setDisable(true);
        registerButton.setText("Inscription...");
        hideError();

        new Thread(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("nom", nom);
                payload.put("prenom", prenom);
                payload.put("email", email);
                payload.put("password", password);

                AppRequest request = new AppRequest.Builder()
                        .controller("Auth").action("register")
                        .payload(JsonUtils.toJson(payload))
                        .build();

                AppResponse response = tcpClient.sendAndParse(request);

                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    registerButton.setText("S'inscrire");
                    if (response != null && response.isSuccess()) {
                        onRegisterSuccess.run();
                    } else {
                        showError(response != null && response.getMessage() != null
                                ? response.getMessage()
                                : "Inscription échouée. Email déjà utilisé ?");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    registerButton.setText("S'inscrire");
                    showError("Erreur réseau : " + e.getMessage());
                });
            }
        }).start();
    }

    private Label createFieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + AppTheme.TEXT_MAIN + ";" +
                        "-fx-padding: 0 0 4 4;"
        );
        VBox.setMargin(lbl, new Insets(8, 0, 4, 0));
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