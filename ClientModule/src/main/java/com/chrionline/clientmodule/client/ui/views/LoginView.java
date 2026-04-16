package com.chrionline.clientmodule.client.ui.views;

import com.chrionline.clientmodule.utils.CaptchaServer;
import com.chrionline.core.theme.AppTheme;
import com.chrionline.core.utils.JsonUtils;
import com.chrionline.network.protocol.AppResponse;
import com.chrionline.network.protocol.AppRequest;
import com.chrionline.network.tcp.TCPClient;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javafx.scene.web.WebEngine;
import javafx.concurrent.Worker;


public class LoginView extends StackPane {

    private final TextField                    emailField;
    private final PasswordField                passwordField;
    private final Button                       loginButton;
    private final Label                        errorLabel;
    private final TCPClient                    tcpClient;
    private final Consumer<Map<String, Object>> onLoginSuccess;
    private final Runnable                     onGoToRegister;
    private final Runnable                     onGoToForgotPassword;
    // ── Captcha ───────────────────────────────────────────
    private WebView captchaWebView;
    private String  captchaToken = null;
//on passe le tcp client, un consumer appele en cas de succes, runnables vers l inscription ou mot de passe oublié
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

        // Icon
        Label icon = new Label("🛍");
        icon.setStyle("-fx-font-size: 40px;");
        VBox iconBox = new VBox(icon);
        iconBox.setAlignment(Pos.CENTER);
        VBox.setMargin(iconBox, new Insets(0, 0, 8, 0));

        // Title
        Label title = new Label("ChriOnline");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");
        Label subtitle = new Label("Boutique artisanale");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        VBox titleBox = new VBox(4, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);
        VBox.setMargin(titleBox, new Insets(0, 0, 24, 0));

        // Toggle Connexion / Inscription
        Button btnConnexion   = new Button("Connexion");
        Button btnInscription = new Button("Inscription");
        AppTheme.styleToggleActive(btnConnexion);
        AppTheme.styleToggleInactive(btnInscription);
        btnInscription.setOnAction(e -> onGoToRegister.run());
        btnConnexion.setMaxWidth(Double.MAX_VALUE);
        btnInscription.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnConnexion,   Priority.ALWAYS);
        HBox.setHgrow(btnInscription, Priority.ALWAYS);

        HBox toggle = new HBox(0, btnConnexion, btnInscription);
        toggle.setStyle(
                "-fx-background-color: " + AppTheme.TOGGLE_INACTIVE + ";" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 4px;"
        );
        toggle.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(toggle, new Insets(0, 0, 24, 0));

        // Email
        emailField = new TextField();
        emailField.setPromptText("votre@email.com");
        AppTheme.styleTextField(emailField);
        AppTheme.styleFocusedTextField(emailField);
        StackPane emailPane = wrapWithIcon("✉", emailField);
        VBox.setMargin(emailPane, new Insets(0, 0, 14, 0));

        // Password
        passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        AppTheme.styleTextField(passwordField);
        AppTheme.styleFocusedTextField(passwordField);
        passwordField.setOnAction(e -> handleLogin());
        StackPane passPane = wrapWithIcon("🔒", passwordField);
        VBox.setMargin(passPane, new Insets(0, 0, 6, 0));

        // Forgot password — navigates via the Runnable callback (safe, no getScene() call)
        Hyperlink forgot = new Hyperlink("Mot de passe oublié ?");
        forgot.setStyle(
                "-fx-text-fill: " + AppTheme.TEXT_MUTED + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-border-color: transparent;"
        );
        forgot.setOnAction(e -> onGoToForgotPassword.run());

        HBox forgotRow = new HBox(forgot);
        forgotRow.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(forgotRow, new Insets(0, 0, 18, 0));

        // Error label
        errorLabel = new Label();
        errorLabel.setStyle(
                "-fx-text-fill: " + AppTheme.ERROR_COLOR + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 0 0 8 0;"
        );
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        // Login button
        loginButton = new Button("Se connecter");
        AppTheme.stylePrimaryButton(loginButton);
        loginButton.setOnAction(e -> handleLogin()); //what happens when we click on the log in button
        // ── WebView reCAPTCHA ─────────────────────────────────
        captchaWebView = buildCaptchaWebView();
        card.getChildren().addAll(
                iconBox, titleBox, toggle,
                createFieldLabel("Email"),        emailPane,
                createFieldLabel("Mot de passe"), passPane,
                forgotRow, errorLabel, captchaWebView, loginButton
        );

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + AppTheme.BG + "; -fx-background-color: " + AppTheme.BG + ";");

        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(24));
        wrapper.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        scroll.setContent(wrapper);
        StackPane.setAlignment(scroll, Pos.CENTER);
        this.getChildren().add(scroll);
    }
//methode appéle le moemnt on clique sur le button de connexion
private void handleLogin() {
    String email    = emailField.getText().trim();
    String password = passwordField.getText();

    if (email.isEmpty() || password.isEmpty()) { showError("Veuillez remplir tous les champs."); return; }
    if (!email.contains("@"))                  { showError("Adresse e-mail invalide."); return; }

    // ✅ NOUVEAU — vérification captcha côté client
    if (captchaToken == null) { showError("Veuillez valider le reCAPTCHA."); return; }

    loginButton.setDisable(true);
    loginButton.setText("Connexion...");
    hideError();

    new Thread(() -> {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("email",        email);
            payload.put("password",     password);
            payload.put("captchaToken", captchaToken); // ← NOUVEAU

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
                    if (data != null) onLoginSuccess.accept(data);
                } else {
                    showError(response != null && response.getMessage() != null
                            ? response.getMessage()
                            : "Connexion échouée. Vérifiez vos identifiants.");
                    resetCaptcha(); // ← NOUVEAU — reset après échec
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                loginButton.setDisable(false);
                loginButton.setText("Se connecter");
                showError("Erreur réseau : " + e.getMessage());
                resetCaptcha(); // ← NOUVEAU
            });
        }
    }).start();
}
//creation des labels
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
//tilisation des emojis
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
    private void hideError()           { errorLabel.setVisible(false); }
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

        // Polling toutes les 500 ms — contourne la perte de référence window.javabridge
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
                        loginButton.setDisable(false);
                    }
                } else {
                    if (captchaToken != null) {
                        captchaToken = null;
                        loginButton.setDisable(true);
                    }
                }
            } catch (Exception ignored) {}
        }));
        poller.setCycleCount(Animation.INDEFINITE);

        engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                poller.play();
            }
        });

        loginButton.setDisable(true);
        return wv;
    }

    private void resetCaptcha() {
        captchaToken = null;
        loginButton.setDisable(true);
        try {
            captchaWebView.getEngine().executeScript("grecaptcha.reset()");
        } catch (Exception ignored) {}
    }
}