package com.chrionline.clientmodule.client.ui.views;

import com.chrionline.clientmodule.client.security.KeyPairManager;
import com.chrionline.clientmodule.client.security.Signer;
import com.chrionline.clientmodule.utils.CaptchaServer;
import com.chrionline.core.theme.AppTheme;
import com.chrionline.core.utils.JsonUtils;
import com.chrionline.core.network.protocol.AppResponse;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.network.tcp.TCPClient;
import com.chrionline.shared.models.PanierProduit;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.security.KeyPair;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.scene.web.WebEngine;
import javafx.concurrent.Worker;


public class LoginView extends StackPane {

    // ── Mode courant ──────────────────────────────────────
    private enum Mode { CLIENT, ADMIN }
    private Mode currentMode = Mode.CLIENT;

    // ── Champs communs ────────────────────────────────────
    private final TextField                     emailField;
    private final PasswordField                 passwordField;
    private final Button                        loginButton;
    private final Label                         errorLabel;
    private final TCPClient                     tcpClient;
    private final Consumer<Map<String, Object>> onLoginSuccess;
    private final Runnable                      onGoToRegister;
    private final Runnable                      onGoToForgotPassword;

    // ── Sections conditionnelles ──────────────────────────
    private final StackPane  passPane;
    private final Label      passLabel;
    private final HBox       forgotRow;
    private final HBox       toggleClientMode;   // Connexion | Inscription — Client only
    private       WebView    captchaWebView;
    private       String     captchaToken = null;

    // ── Boutons du mode switcher ──────────────────────────
    private final Button btnModeClient;
    private final Button btnModeAdmin;

    public LoginView(TCPClient tcpClient,
                     Consumer<Map<String, Object>> onLoginSuccess,
                     Runnable onGoToRegister,
                     Runnable onGoToForgotPassword) {

        this.tcpClient            = tcpClient;
        this.onLoginSuccess       = onLoginSuccess;
        this.onGoToRegister       = onGoToRegister;
        this.onGoToForgotPassword = onGoToForgotPassword;

        this.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        VBox card = new VBox(0);
        card.setMaxWidth(480);
        card.setMaxHeight(Double.MAX_VALUE);
        AppTheme.styleCard(card);
        card.setPadding(new Insets(40, 40, 40, 40));

        // ── Icône ─────────────────────────────────────────
        Label icon = new Label("🛍");
        icon.setStyle("-fx-font-size: 44px;");
        VBox iconBox = new VBox(icon);
        iconBox.setAlignment(Pos.CENTER);
        VBox.setMargin(iconBox, new Insets(0, 0, 6, 0));

        // ── Titre ─────────────────────────────────────────
        Label title = new Label("ChriOnline");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");
        Label subtitle = new Label("Boutique artisanale");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        VBox titleBox = new VBox(4, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);
        VBox.setMargin(titleBox, new Insets(0, 0, 24, 0));

        // ══════════════════════════════════════════════════
        //  SWITCHER  Client | Admin
        // ══════════════════════════════════════════════════
        btnModeClient = new Button("Client");
        btnModeAdmin  = new Button("Admin");
        styleModeActive(btnModeClient);
        styleModeInactive(btnModeAdmin);

        btnModeClient.setMaxWidth(Double.MAX_VALUE);
        btnModeAdmin.setMaxWidth(Double.MAX_VALUE);
        btnModeClient.setPrefHeight(40);
        btnModeAdmin.setPrefHeight(40);
        HBox.setHgrow(btnModeClient, Priority.ALWAYS);
        HBox.setHgrow(btnModeAdmin,  Priority.ALWAYS);

        btnModeClient.setOnAction(e -> switchMode(Mode.CLIENT));
        btnModeAdmin .setOnAction(e -> switchMode(Mode.ADMIN));

        HBox modeSwitcher = new HBox(0, btnModeClient, btnModeAdmin);
        modeSwitcher.setStyle(
                "-fx-background-color: " + AppTheme.TOGGLE_INACTIVE + ";" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 4px;"
        );
        modeSwitcher.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(modeSwitcher, new Insets(0, 0, 20, 0));

        // ══════════════════════════════════════════════════
        //  SWITCHER  Connexion | Inscription  (CLIENT only)
        // ══════════════════════════════════════════════════
        Button btnConnexion   = new Button("Connexion");
        Button btnInscription = new Button("Inscription");
        AppTheme.styleToggleActive(btnConnexion);
        AppTheme.styleToggleInactive(btnInscription);
        btnInscription.setOnAction(e -> onGoToRegister.run());
        btnConnexion.setMaxWidth(Double.MAX_VALUE);
        btnInscription.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnConnexion,   Priority.ALWAYS);
        HBox.setHgrow(btnInscription, Priority.ALWAYS);

        toggleClientMode = new HBox(0, btnConnexion, btnInscription);
        toggleClientMode.setStyle(
                "-fx-background-color: " + AppTheme.TOGGLE_INACTIVE + ";" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 4px;"
        );
        toggleClientMode.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(toggleClientMode, new Insets(0, 0, 24, 0));



        // ── Email ─────────────────────────────────────────
        emailField = new TextField();
        emailField.setPromptText("votre@email.com");
        AppTheme.styleTextField(emailField);
        AppTheme.styleFocusedTextField(emailField);
        StackPane emailPane = wrapWithIcon("✉", emailField);
        VBox.setMargin(emailPane, new Insets(0, 0, 14, 0));

        // ── Password (CLIENT only) ────────────────────────
        passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        AppTheme.styleTextField(passwordField);
        AppTheme.styleFocusedTextField(passwordField);
        passwordField.setOnAction(e -> handleLogin());
        passPane  = wrapWithIcon("🔒", passwordField);
        VBox.setMargin(passPane, new Insets(0, 0, 6, 0));

        // ── Mot de passe oublié (CLIENT only) ─────────────
        Hyperlink forgot = new Hyperlink("Mot de passe oublié ?");
        forgot.setStyle(
                "-fx-text-fill: " + AppTheme.TEXT_MUTED + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-border-color: transparent;"
        );
        forgot.setOnAction(e -> onGoToForgotPassword.run());
        forgotRow = new HBox(forgot);
        forgotRow.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(forgotRow, new Insets(0, 0, 18, 0));

        // ── Labels de section ─────────────────────────────
        passLabel = createFieldLabel("Mot de passe");

        // ── Message d'erreur ──────────────────────────────
        errorLabel = new Label();
        errorLabel.setStyle(
                "-fx-background-color: #FFF0F0;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-text-fill: " + AppTheme.ERROR_COLOR + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 8 12 8 12;" +
                        "-fx-border-color: " + AppTheme.ERROR_COLOR + "44;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-border-width: 1px;"
        );
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(errorLabel, new Insets(0, 0, 10, 0));

        // ── Séparateur ────────────────────────────────────
        Separator sep = new Separator();
        sep.setOpacity(0.3);
        VBox.setMargin(sep, new Insets(4, 0, 18, 0));

        // ── Bouton de connexion ───────────────────────────
        loginButton = new Button("Se connecter");
        loginButton.setPrefHeight(46);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        AppTheme.stylePrimaryButton(loginButton);
        loginButton.setOnAction(e -> handleLogin());

        // ── reCAPTCHA WebView ─────────────────────────────
        captchaWebView = buildCaptchaWebView();

        // ── Assemblage ────────────────────────────────────
        card.getChildren().addAll(
                iconBox,
                titleBox,
                modeSwitcher,
                toggleClientMode,          // masqué en mode Admin
                createFieldLabel("Email"),
                emailPane,
                passLabel,                 // masqué en mode Admin
                passPane,                  // masqué en mode Admin
                forgotRow,                 // masqué en mode Admin
                errorLabel,
                captchaWebView,            // masqué en mode Admin
                sep,
                loginButton
        );

        // ── ScrollPane ────────────────────────────────────
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: " + AppTheme.BG + "; -fx-background-color: " + AppTheme.BG + ";");

        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(24));
        wrapper.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        scroll.setContent(wrapper);
        StackPane.setAlignment(scroll, Pos.CENTER);
        this.getChildren().add(scroll);

        // État initial
        applyModeUI(Mode.CLIENT);
    }

    // ══════════════════════════════════════════════════════
    //  Logique de changement de mode
    // ══════════════════════════════════════════════════════

    private void switchMode(Mode mode) {
        if (currentMode == mode) return;
        currentMode = mode;
        hideError();
        resetCaptcha();
        emailField.clear();
        passwordField.clear();
        applyModeUI(mode);
    }

    private void applyModeUI(Mode mode) {
        boolean isClient = (mode == Mode.CLIENT);

        // Switcher style
        if (isClient) { styleModeActive(btnModeClient); styleModeInactive(btnModeAdmin); }
        else          { styleModeInactive(btnModeClient); styleModeActive(btnModeAdmin); }

        // Toggle Connexion/Inscription — Client only
        toggleClientMode.setVisible(isClient);
        toggleClientMode.setManaged(isClient);



        // Password, forgot, captcha — Client only
        passLabel.setVisible(isClient);
        passLabel.setManaged(isClient);
        passPane.setVisible(isClient);
        passPane.setManaged(isClient);
        forgotRow.setVisible(isClient);
        forgotRow.setManaged(isClient);
        captchaWebView.setVisible(isClient);
        captchaWebView.setManaged(isClient);

        // Bouton
        loginButton.setDisable(isClient && captchaToken == null);
        loginButton.setText(isClient ? "Se connecter" : "Demander l'accès  →");

        // Prompt email
        emailField.setPromptText(isClient ? "votre@email.com" : "admin@chrionline.com");

        // Fondu
        FadeTransition ft = new FadeTransition(Duration.millis(220), this);
        ft.setFromValue(0.75);
        ft.setToValue(1.0);
        ft.play();
    }

    // ══════════════════════════════════════════════════════
    //  Gestion du login
    // ══════════════════════════════════════════════════════

    private AppResponse handleAdminLogin(String email) throws Exception {
        AppRequest requestLogin = new AppRequest.Builder()
                .controller("KeyAuth").action("requestLogin")
                .payload(JsonUtils.toJson(Map.of(
                        "email", email,
                        "mode",      currentMode.name().toLowerCase()
                )))
                .build();

        AppResponse resp = tcpClient.sendAndParse(requestLogin);
        if (!resp.isSuccess()) return resp;

        System.out.println("Reached here 1");
        System.out.println(resp.getDataAs(Map.class));
        Map<String, Object> data = resp.getDataAs(Map.class);
        String challengeId = (String) data.get("challengeId");
        String challenge   = (String) data.get("challenge");
        long expiresAt = ((Number) data.get("expiresAt")).longValue();

        System.out.println("Reached here 2");

        if (challengeId == null || challenge == null) {
            throw new IllegalStateException("Incomplete challenge response from server.");
        }
        if (expiresAt <= System.currentTimeMillis()) {
            throw new IllegalStateException("Challenge already expired.");
        }

        String deviceName = InetAddress.getLocalHost().getHostName();

        KeyPair keyPair      = KeyPairManager.loadFromFile(deviceName);
        byte[]  sigBytes     = Signer.sign(challenge, keyPair.getPrivate());
        String  signature    = Base64.getEncoder().encodeToString(sigBytes);
        String  fingerprint  = KeyPairManager.computeFingerprint(keyPair.getPublic());

        AppRequest loginRequest = new AppRequest.Builder()
                .controller("KeyAuth").action("login")
                .payload(JsonUtils.toJson(Map.of(
                        "email",   email,
                        "challengeId", challengeId,
                        "signature",   signature,
                        "fingerprint", fingerprint
                )))
                .build();

        return tcpClient.sendAndParse(loginRequest);
    }


    private AppResponse handleClientLogin(String email, String password) throws Exception {
        AppRequest request = new AppRequest.Builder()
                .controller("Auth").action("login")
                .payload(JsonUtils.toJson(Map.of(
                        "email",        email,
                        "mode",         currentMode.name().toLowerCase(),
                        "password",     password,
                        "captchaToken", captchaToken
                )))
                .build();

        return tcpClient.sendAndParse(request);
    }


    private void onLoginResponse(AppResponse response) {
        resetLoginButton();
        if (response != null && response.isSuccess()) {
            Map<String, Object> data = response.getDataAs(Map.class);
            if (data != null) onLoginSuccess.accept(data);
        } else {
            showError(response != null && response.getMessage() != null
                    ? response.getMessage()
                    : "Connexion échouée. Vérifiez vos identifiants.");
            if (currentMode == Mode.CLIENT) resetCaptcha();
        }
    }

    private void onLoginError(String message) {
        resetLoginButton();
        showError("Erreur réseau : " + message);
        if (currentMode == Mode.CLIENT) resetCaptcha();
    }

    private void resetLoginButton() {
        loginButton.setDisable(false);
        loginButton.setText(currentMode == Mode.ADMIN
                ? "Demander l'accès  →"
                : "Se connecter");
    }

    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = currentMode == Mode.CLIENT ? passwordField.getText() : null;

        if (email.isEmpty())       { showError("Veuillez saisir votre email."); return; }
        if (!email.contains("@")) { showError("Adresse e-mail invalide."); return; }

        if (currentMode == Mode.CLIENT) {
            if (password == null || password.isEmpty()) { showError("Veuillez saisir votre mot de passe."); return; }
            if (captchaToken == null) { showError("Veuillez valider le reCAPTCHA."); return; }
        }

        loginButton.setDisable(true);
        loginButton.setText("Connexion en cours...");
        hideError();

        new Thread(() -> {
            try {
                AppResponse response = currentMode == Mode.ADMIN
                        ? handleAdminLogin(email)
                        : handleClientLogin(email, password);

                Platform.runLater(() -> onLoginResponse(response));

            } catch (Exception e) {
                Platform.runLater(() -> onLoginError(e.getMessage()));
            }
        }).start();
    }

    // ══════════════════════════════════════════════════════
    //  Helpers UI
    // ══════════════════════════════════════════════════════

    private void styleModeActive(Button btn) {
        btn.setStyle(
                "-fx-background-color: " + AppTheme.PRIMARY + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-background-radius: 26px;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 6, 0, 0, 2);"
        );
    }

    private void styleModeInactive(Button btn) {
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + AppTheme.TEXT_MUTED + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-background-radius: 26px;" +
                        "-fx-cursor: hand;"
        );
    }

    private Label createFieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + AppTheme.TEXT_MUTED + ";" +
                        "-fx-padding: 0 0 4 2;"
        );
        VBox.setMargin(lbl, new Insets(6, 0, 4, 0));
        return lbl;
    }

    private StackPane wrapWithIcon(String emoji, Control field) {
        Label iconLabel = new Label(emoji);
        iconLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        StackPane pane = new StackPane(field, iconLabel);
        StackPane.setAlignment(iconLabel, Pos.CENTER_LEFT);
        iconLabel.setTranslateX(14);
        field.setMaxWidth(Double.MAX_VALUE);
        return pane;
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    // ══════════════════════════════════════════════════════
    //  reCAPTCHA
    // ══════════════════════════════════════════════════════

    private WebView buildCaptchaWebView() {
        WebView wv = new WebView();
        wv.setPrefSize(400, 560);
        wv.setMinSize(400, 560);
        wv.setMaxSize(400, 560);
        VBox.setMargin(wv, new Insets(10, 0, 16, 0));

        WebEngine engine = wv.getEngine();

        try {
            int port = CaptchaServer.start();
            engine.load("http://localhost:" + port + "/recaptcha");
        } catch (Exception e) {
            System.err.println("Erreur démarrage serveur captcha : " + e.getMessage());
        }

        Timeline poller = new Timeline(new KeyFrame(Duration.millis(500), ev -> {
            try {
                Object result = engine.executeScript(
                        "(typeof grecaptcha !== 'undefined' && typeof grecaptcha.getResponse === 'function')"
                                + " ? grecaptcha.getResponse() : ''"
                );
                String token = (result instanceof String) ? (String) result : "";
                if (!token.isEmpty()) {
                    if (captchaToken == null || !captchaToken.equals(token)) {
                        captchaToken = token;
                        if (currentMode == Mode.CLIENT) loginButton.setDisable(false);
                    }
                } else {
                    if (captchaToken != null) {
                        captchaToken = null;
                        if (currentMode == Mode.CLIENT) loginButton.setDisable(true);
                    }
                }
            } catch (Exception ignored) {}
        }));
        poller.setCycleCount(Animation.INDEFINITE);

        engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) poller.play();
        });

        loginButton.setDisable(true);
        return wv;
    }

    private void resetCaptcha() {
        captchaToken = null;
        if (currentMode == Mode.CLIENT) loginButton.setDisable(true);
        try {
            captchaWebView.getEngine().executeScript("grecaptcha.reset()");
        } catch (Exception ignored) {}
    }
}
