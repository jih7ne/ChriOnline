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

/**
 * Écran d'inscription — adapté au protocole AppRequest/ApiResponse avec Gson.
 */
public class RegisterView extends VBox {

    private final TextField nomField, prenomField, emailField;
    private final PasswordField passwordField, confirmField;
    private final Button registerButton;
    private final Label errorLabel;
    private final TCPClient tcpClient;
    private final Runnable onRegisterSuccess;
    private final Runnable onGoToLogin;

    public RegisterView(TCPClient tcpClient, Runnable onRegisterSuccess, Runnable onGoToLogin) {
        this.tcpClient = tcpClient;
        this.onRegisterSuccess = onRegisterSuccess;
        this.onGoToLogin = onGoToLogin;

        this.setAlignment(Pos.CENTER);
        this.setStyle("-fx-background-color: #FFFFFF;");
        this.setPadding(new Insets(40));


        Text logo = new Text("ChriOnline");
        logo.setFont(Font.font("System", FontWeight.BOLD, 28));
        logo.setFill(Color.web("#FF385C"));
        Text subtitle = new Text("Créez votre compte");
        subtitle.setFont(Font.font("System", FontWeight.NORMAL, 14));
        subtitle.setFill(Color.web("#717171"));
        VBox header = new VBox(6, logo, subtitle);
        header.setAlignment(Pos.CENTER);
        VBox.setMargin(header, new Insets(0, 0, 24, 0));


        nomField = new TextField(); nomField.setPromptText("Dupont"); AppTheme.styleTextField(nomField);
        prenomField = new TextField(); prenomField.setPromptText("Jean"); AppTheme.styleTextField(prenomField);
        VBox nomBox = new VBox(4, createLabel("Nom"), nomField);
        VBox prenomBox = new VBox(4, createLabel("Prénom"), prenomField);
        HBox.setHgrow(nomBox, Priority.ALWAYS); HBox.setHgrow(prenomBox, Priority.ALWAYS);
        HBox nameRow = new HBox(12, nomBox, prenomBox);

        emailField = new TextField(); emailField.setPromptText("votre@email.com"); AppTheme.styleTextField(emailField);
        passwordField = new PasswordField(); passwordField.setPromptText("Min. 6 caractères"); AppTheme.styleTextField(passwordField);
        confirmField = new PasswordField(); confirmField.setPromptText("Répétez le mot de passe"); AppTheme.styleTextField(confirmField);

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #FF385C; -fx-font-size: 13px;");
        errorLabel.setVisible(false); errorLabel.setWrapText(true);

        registerButton = new Button("Créer mon compte");
        AppTheme.stylePrimaryButton(registerButton);
        registerButton.setOnAction(e -> handleRegister());

        Hyperlink loginLink = new Hyperlink("Déjà un compte ? Se connecter");
        loginLink.setStyle("-fx-text-fill: #FF385C; -fx-font-size: 13px; -fx-border-color: transparent;");
        loginLink.setOnAction(e -> onGoToLogin.run());
        HBox loginRow = new HBox(loginLink); loginRow.setAlignment(Pos.CENTER);
        Separator sep = new Separator(); VBox.setMargin(sep, new Insets(16, 0, 0, 0));

        VBox card = new VBox(10,
                nameRow,
                createLabel("Adresse e-mail"), emailField,
                createLabel("Mot de passe"), passwordField,
                createLabel("Confirmer le mot de passe"), confirmField,
                errorLabel, registerButton, sep, loginRow
        );
        card.setMaxWidth(480); card.setPadding(new Insets(32));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px;" +
                "-fx-border-color: #EBEBEB; -fx-border-radius: 16px;");
        card.setEffect(new DropShadow(20, 0, 4, Color.rgb(0, 0, 0, 0.08)));
        this.getChildren().addAll(header, card);
    }

    private void handleRegister() {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmField.getText();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()) { showError("Remplissez tous les champs."); return; }
        if (!email.contains("@")) { showError("Email invalide."); return; }
        if (password.length() < 6) { showError("Mot de passe trop court (min. 6 caractères)."); return; }
        if (!password.equals(confirm)) { showError("Les mots de passe ne correspondent pas."); return; }

        registerButton.setDisable(true); registerButton.setText("Création..."); hideError();

        new Thread(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("nom", nom); payload.put("prenom", prenom);
                payload.put("email", email); payload.put("password", password);

                AppRequest request = new AppRequest.Builder()
                        .controller("Auth").action("register")
                        .payload(JsonUtils.toJson(payload))
                        .build();

                ApiResponse response = tcpClient.sendAndParse(request);

                Platform.runLater(() -> {
                    registerButton.setDisable(false); registerButton.setText("Créer mon compte");
                    if (response != null && response.isSuccess()) {
                        onRegisterSuccess.run();
                    } else {
                        String msg = response != null ? response.getMessage() : null;
                        showError(msg != null ? msg : "Inscription échouée. Email déjà utilisé ?");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    registerButton.setDisable(false); registerButton.setText("Créer mon compte");
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