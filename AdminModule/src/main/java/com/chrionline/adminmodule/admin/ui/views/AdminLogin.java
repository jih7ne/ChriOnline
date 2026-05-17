package com.chrionline.adminmodule.admin.ui.views;

import com.chrionline.adminmodule.admin.security.KeyPairManager;
import com.chrionline.adminmodule.admin.security.Signer;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.core.network.protocol.AppResponse;
import com.chrionline.core.theme.AppTheme;
import com.chrionline.core.utils.JsonUtils;
import com.chrionline.network.tcp.TCPClient;
import com.chrionline.utils.CaptchaServer;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.net.InetAddress;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Map;
import java.util.function.Consumer;

public class AdminLogin extends StackPane {

    private final TCPClient                         tcpClient;
    private final Consumer<Map<String, Object>>     onLoginSuccess;

    // Shared
    private final Label                             errorLabel;

    // Key Auth tab
    private final TextField                         emailField;
    private final Button                            keyLoginButton;

    // Password tab
    private final TextField                         pwEmailField;
    private final PasswordField                     passwordField;
    private final Button                            pwLoginButton;
    private final WebView                           captchaWebView;
    private       String                            captchaToken = null;

    // Tab state
    private boolean keyTabActive = true;
    private Button  keyTabBtn;
    private Button  pwTabBtn;
    private VBox    keyTabContent;
    private VBox    pwTabContent;



    public AdminLogin(TCPClient tcpClient,
                      Consumer<Map<String, Object>> onLoginSuccess) {

        this.tcpClient      = tcpClient;
        this.onLoginSuccess = onLoginSuccess;

        this.emailField     = buildTextField("votre@email.com");
        this.pwEmailField   = buildTextField("votre@email.com");
        this.passwordField  = buildPasswordField();
        this.errorLabel     = buildErrorLabel();
        this.keyLoginButton = buildButton("Demander l'accès  →", e -> handleKeyLogin());

        // Password button starts DISABLED — captcha must resolve first
        this.pwLoginButton  = buildButton("Se connecter  →", e -> handlePasswordLogin());
        this.pwLoginButton.setDisable(true);

        this.captchaWebView = buildCaptchaWebView();

        this.setStyle("-fx-background-color: " + AppTheme.BG + ";");
        this.getChildren().add(buildScrollWrapper(buildCard()));
    }



    private VBox buildCard() {
        keyTabContent = buildKeyTabContent();
        pwTabContent  = buildPwTabContent();   // captchaWebView lives inside pwTabContent

        // Password tab hidden initially
        pwTabContent.setVisible(false);
        pwTabContent.setManaged(false);

        VBox card = new VBox(0);
        card.setMaxWidth(480);
        card.setMaxHeight(Double.MAX_VALUE);
        AppTheme.styleCard(card);
        card.setPadding(new Insets(40));
        card.getChildren().addAll(
                buildIconBox(),
                buildTitleBox(),
                buildTabBar(),
                buildTabDivider(),
                keyTabContent,
                pwTabContent,
                errorLabel,
                buildSeparator(),
                keyLoginButton,
                pwLoginButton
        );

        // Password button hidden initially
        pwLoginButton.setVisible(false);
        pwLoginButton.setManaged(false);

        return card;
    }

    private ScrollPane buildScrollWrapper(VBox card) {
        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(24));
        wrapper.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: " + AppTheme.BG + "; -fx-background-color: " + AppTheme.BG + ";");
        return scroll;
    }



    private HBox buildTabBar() {
        keyTabBtn = buildTabButton("🔑  Clé publique", true);
        pwTabBtn  = buildTabButton("🔒  Mot de passe", false);

        keyTabBtn.setOnAction(e -> switchTab(true));
        pwTabBtn .setOnAction(e -> switchTab(false));

        HBox bar = new HBox(0, keyTabBtn, pwTabBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private Button buildTabButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setPrefHeight(36);
        btn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btn, Priority.ALWAYS);
        applyTabStyle(btn, active);
        return btn;
    }

    private void applyTabStyle(Button btn, boolean active) {
        if (active) {
            btn.setStyle(
                    "-fx-background-color: " + AppTheme.BG + "22;" +
                            "-fx-text-fill: " + AppTheme.TEXT_MAIN + ";" +
                            "-fx-font-size: 12px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-border-color: " + AppTheme.TEXT_MAIN + "66;" +
                            "-fx-border-width: 0 0 2 0;" +
                            "-fx-background-radius: 0;" +
                            "-fx-border-radius: 0;" +
                            "-fx-cursor: default;" +
                            "-fx-padding: 8 16 8 16;"
            );
        } else {
            btn.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-text-fill: " + AppTheme.TEXT_MUTED + ";" +
                            "-fx-font-size: 12px;" +
                            "-fx-font-weight: normal;" +
                            "-fx-border-color: transparent;" +
                            "-fx-border-width: 0 0 2 0;" +
                            "-fx-background-radius: 0;" +
                            "-fx-border-radius: 0;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 8 16 8 16;"
            );
        }
    }

    private Region buildTabDivider() {
        Region line = new Region();
        line.setPrefHeight(1);
        line.setMaxWidth(Double.MAX_VALUE);
        line.setStyle("-fx-background-color: " + AppTheme.TEXT_MUTED + "33;");
        VBox.setMargin(line, new Insets(0, 0, 20, 0));
        return line;
    }

    private void switchTab(boolean toKeyTab) {
        if (keyTabActive == toKeyTab) return;
        keyTabActive = toKeyTab;
        hideError();

        applyTabStyle(keyTabBtn, toKeyTab);
        applyTabStyle(pwTabBtn,  !toKeyTab);

        keyTabContent.setVisible(toKeyTab);
        keyTabContent.setManaged(toKeyTab);
        pwTabContent .setVisible(!toKeyTab);
        pwTabContent .setManaged(!toKeyTab);

        keyLoginButton.setVisible(toKeyTab);
        keyLoginButton.setManaged(toKeyTab);
        pwLoginButton .setVisible(!toKeyTab);
        pwLoginButton .setManaged(!toKeyTab);
    }



    private VBox buildKeyTabContent() {
        VBox box = new VBox(0);
        box.getChildren().addAll(
                fieldLabel("Email"),
                wrapWithIcon("✉", emailField)
        );
        return box;
    }

    /**
     * Password tab content owns the captchaWebView so it is only
     * laid out when this tab is active. When pwTabContent.setManaged(false),
     * JavaFX excludes the entire subtree — including the WebView — from layout.
     */
    private VBox buildPwTabContent() {
        // reCAPTCHA widget itself is ~78px tall. The challenge popup floats
        // outside the WebView so we only need enough room for the checkbox widget.
        captchaWebView.setPrefSize(400, 560);
        captchaWebView.setMinSize(400, 560);
        captchaWebView.setMaxSize(400, 560);
        VBox.setMargin(captchaWebView, new Insets(6, 0, 10, 0));

        VBox box = new VBox(0);
        box.getChildren().addAll(
                fieldLabel("Email"),
                wrapWithIcon("✉", pwEmailField),
                fieldLabel("Mot de passe"),
                wrapWithIcon("🔒", passwordField),
                captchaWebView
        );
        return box;
    }



    private VBox buildIconBox() {
        Label icon = new Label("🛍");
        icon.setStyle("-fx-font-size: 44px;");
        VBox box = new VBox(icon);
        box.setAlignment(Pos.CENTER);
        VBox.setMargin(box, new Insets(0, 0, 6, 0));
        return box;
    }

    private VBox buildTitleBox() {
        Label title = new Label("ChriOnline");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");
        Label subtitle = new Label("Administration");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        VBox box = new VBox(4, title, subtitle);
        box.setAlignment(Pos.CENTER);
        VBox.setMargin(box, new Insets(0, 0, 24, 0));
        return box;
    }



    private TextField buildTextField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        AppTheme.styleTextField(f);
        AppTheme.styleFocusedTextField(f);
        VBox.setMargin(f, new Insets(0, 0, 14, 0));
        return f;
    }

    private PasswordField buildPasswordField() {
        PasswordField f = new PasswordField();
        f.setPromptText("••••••••");
        AppTheme.styleTextField(f);
        AppTheme.styleFocusedTextField(f);
        f.setOnAction(e -> handlePasswordLogin());
        VBox.setMargin(f, new Insets(0, 0, 14, 0));
        return f;
    }

    private Button buildButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setPrefHeight(46);
        btn.setMaxWidth(Double.MAX_VALUE);
        AppTheme.stylePrimaryButton(btn);
        btn.setOnAction(handler);
        return btn;
    }

    private Label buildErrorLabel() {
        Label lbl = new Label();
        lbl.setStyle(
                "-fx-background-color: #FFF0F0;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-text-fill: " + AppTheme.ERROR_COLOR + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 8 12 8 12;" +
                        "-fx-border-color: " + AppTheme.ERROR_COLOR + "44;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-border-width: 1px;"
        );
        lbl.setVisible(false);
        lbl.setManaged(false);
        lbl.setWrapText(true);
        lbl.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(lbl, new Insets(0, 0, 10, 0));
        return lbl;
    }

    private Separator buildSeparator() {
        Separator sep = new Separator();
        sep.setOpacity(0.3);
        VBox.setMargin(sep, new Insets(4, 0, 18, 0));
        return sep;
    }



    private void handleKeyLogin() {
        String email = emailField.getText().trim();
        if (email.isEmpty())       { showError("Veuillez saisir votre email."); return; }
        if (!email.contains("@")) { showError("Adresse e-mail invalide.");      return; }

        setPending(keyLoginButton, true, "Connexion en cours...", "Demander l'accès  →");
        new Thread(() -> {
            try {
                AppResponse response = performKeyLogin(email);
                Platform.runLater(() -> onLoginResponse(response, keyLoginButton, "Demander l'accès  →"));
            } catch (Exception e) {
                Platform.runLater(() -> onLoginError(e.getMessage(), keyLoginButton, "Demander l'accès  →"));
            }
        }).start();
    }

    private void handlePasswordLogin() {
        String email    = pwEmailField.getText().trim();
        String password = passwordField.getText();
        if (email.isEmpty())       { showError("Veuillez saisir votre email.");           return; }
        if (!email.contains("@")) { showError("Adresse e-mail invalide.");                return; }
        if (password.isEmpty())    { showError("Veuillez saisir votre mot de passe.");    return; }
        if (captchaToken == null)  { showError("Veuillez valider le reCAPTCHA.");         return; }

        setPending(pwLoginButton, true, "Connexion en cours...", "Se connecter  →");
        new Thread(() -> {
            try {
                AppResponse response = performPasswordLogin(email, password);
                Platform.runLater(() -> onLoginResponse(response, pwLoginButton, "Se connecter  →"));
            } catch (Exception e) {
                Platform.runLater(() -> onLoginError(e.getMessage(), pwLoginButton, "Se connecter  →"));
            }
        }).start();
    }

    private AppResponse performKeyLogin(String email) throws Exception {

        AppResponse challengeResp = tcpClient.sendAndParse(new AppRequest.Builder()
                .controller("KeyAuth").action("requestLogin")
                .payload(JsonUtils.toJson(Map.of("email", email)))
                .build());

        if (!challengeResp.isSuccess()) return challengeResp;

        Map<String, Object> data = challengeResp.getDataAs(Map.class);
        String challengeId = (String)  data.get("challengeId");
        String challenge   = (String)  data.get("challenge");
        long   expiresAt   = ((Number) data.get("expiresAt")).longValue();

        if (challengeId == null || challenge == null)
            throw new IllegalStateException("Incomplete challenge response from server.");
        if (expiresAt <= System.currentTimeMillis())
            throw new IllegalStateException("Challenge already expired.");

        String  deviceName  = InetAddress.getLocalHost().getHostName();
        KeyPair keyPair     = KeyPairManager.loadFromFile(deviceName);
        String  signature   = Base64.getEncoder().encodeToString(Signer.sign(challenge, keyPair.getPrivate()));
        String  fingerprint = KeyPairManager.computeFingerprint(keyPair.getPublic());

        return tcpClient.sendAndParse(new AppRequest.Builder()
                .controller("KeyAuth").action("login")
                .payload(JsonUtils.toJson(Map.of(
                        "email",       email,
                        "challengeId", challengeId,
                        "signature",   signature,
                        "fingerprint", fingerprint
                )))
                .build());
    }

    private AppResponse performPasswordLogin(String email, String password) throws Exception {
        return tcpClient.sendAndParse(new AppRequest.Builder()
                .controller("Auth").action("login")
                .payload(JsonUtils.toJson(Map.of(
                        "email",        email,
                        "password",     password,
                        "captchaToken", captchaToken
                )))
                .build());
    }



    private WebView buildCaptchaWebView() {
        WebView wv = new WebView();
        wv.setPrefSize(400, 560);
        wv.setMinSize(400, 560);
        wv.setMaxSize(400, 560);

        WebEngine engine = wv.getEngine();
        try {
            int port = CaptchaServer.start();
            engine.load("http://localhost:" + port + "/recaptcha");
        } catch (Exception e) {
            System.err.println("Erreur démarrage serveur captcha : " + e.getMessage());
        }

        Timeline poller = buildCaptchaPoller(engine);
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) poller.play();
        });

        return wv;
    }

    private Timeline buildCaptchaPoller(WebEngine engine) {
        Timeline poller = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            try {
                Object result = engine.executeScript(
                        "(typeof grecaptcha !== 'undefined' && typeof grecaptcha.getResponse === 'function')"
                                + " ? grecaptcha.getResponse() : ''"
                );
                String token = (result instanceof String s) ? s : "";
                if (!token.isEmpty()) {
                    if (!token.equals(captchaToken)) {
                        captchaToken = token;
                        pwLoginButton.setDisable(false);
                    }
                } else if (captchaToken != null) {
                    captchaToken = null;
                    pwLoginButton.setDisable(true);
                }
            } catch (Exception ignored) {}
        }));
        poller.setCycleCount(Animation.INDEFINITE);
        return poller;
    }

    private void resetCaptcha() {
        captchaToken = null;
        pwLoginButton.setDisable(true);
        try { captchaWebView.getEngine().executeScript("grecaptcha.reset()"); }
        catch (Exception ignored) {}
    }



    private void onLoginResponse(AppResponse response, Button btn, String defaultLabel) {
        setPending(btn, false, null, defaultLabel);
        if (response != null && response.isSuccess()) {
            Map<String, Object> data = response.getDataAs(Map.class);
            if (data != null) onLoginSuccess.accept(data);
        } else {
            showError(response != null && response.getMessage() != null
                    ? response.getMessage()
                    : "Connexion échouée. Vérifiez vos identifiants.");
            resetCaptcha();
        }
    }

    private void onLoginError(String message, Button btn, String defaultLabel) {
        setPending(btn, false, null, defaultLabel);
        showError("Erreur réseau : " + message);
        resetCaptcha();
    }



    private void setPending(Button btn, boolean pending, String pendingLabel, String defaultLabel) {
        btn.setDisable(pending);
        btn.setText(pending ? pendingLabel : defaultLabel);
        if (pending) hideError();
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



    private Label fieldLabel(String text) {
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
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        icon.setTranslateX(14);
        StackPane pane = new StackPane(field, icon);
        StackPane.setAlignment(icon, Pos.CENTER_LEFT);
        field.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(pane, new Insets(0, 0, 14, 0));
        return pane;
    }
}