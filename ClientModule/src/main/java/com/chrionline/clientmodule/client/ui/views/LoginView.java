package com.chrionline.clientmodule.client.ui.views;


import com.chrionline.core.theme.AppTheme;
import com.chrionline.core.utils.JsonUtils;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.core.network.protocol.AppResponse;
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
import java.util.Map;
import java.util.function.Consumer;

public class LoginView extends StackPane {

    private final TCPClient                         tcpClient;
    private final Consumer<Map<String, Object>>     onLoginSuccess;
    private final Runnable                          onGoToRegister;
    private final Runnable                          onGoToForgotPassword;

    private final TextField                         emailField;
    private final PasswordField                     passwordField;
    private final Label                             errorLabel;
    private final Button                            loginButton;
    private final WebView                           captchaWebView;
    private       String                            captchaToken = null;



    public LoginView(TCPClient tcpClient,
                     Consumer<Map<String, Object>> onLoginSuccess,
                     Runnable onGoToRegister,
                     Runnable onGoToForgotPassword) {

        this.tcpClient            = tcpClient;
        this.onLoginSuccess       = onLoginSuccess;
        this.onGoToRegister       = onGoToRegister;
        this.onGoToForgotPassword = onGoToForgotPassword;

        this.emailField     = buildEmailField();
        this.passwordField  = buildPasswordField();
        this.errorLabel     = buildErrorLabel();
        this.loginButton    = buildLoginButton();
        this.captchaWebView = buildCaptchaWebView();

        this.setStyle("-fx-background-color: " + AppTheme.BG + ";");
        this.getChildren().add(buildScrollWrapper(buildCard()));
    }



    private VBox buildCard() {
        VBox card = new VBox(0);
        card.setMaxWidth(480);
        card.setMaxHeight(Double.MAX_VALUE);
        AppTheme.styleCard(card);
        card.setPadding(new Insets(40));
        card.getChildren().addAll(
                buildIconBox(),
                buildTitleBox(),
                buildToggleBar(),
                fieldLabel("Email"),
                wrapWithIcon("✉", emailField),
                fieldLabel("Mot de passe"),
                wrapWithIcon("🔒", passwordField),
                buildForgotRow(),
                errorLabel,
                captchaWebView,
                buildSeparator(),
                loginButton
        );
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
        Label subtitle = new Label("Boutique artisanale");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        VBox box = new VBox(4, title, subtitle);
        box.setAlignment(Pos.CENTER);
        VBox.setMargin(box, new Insets(0, 0, 24, 0));
        return box;
    }

    private HBox buildToggleBar() {
        Button btnConnexion   = new Button("Connexion");
        Button btnInscription = new Button("Inscription");
        AppTheme.styleToggleActive(btnConnexion);
        AppTheme.styleToggleInactive(btnInscription);
        btnConnexion.setMaxWidth(Double.MAX_VALUE);
        btnInscription.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnConnexion,   Priority.ALWAYS);
        HBox.setHgrow(btnInscription, Priority.ALWAYS);
        btnInscription.setOnAction(e -> onGoToRegister.run());

        HBox bar = new HBox(0, btnConnexion, btnInscription);
        bar.setStyle(
                "-fx-background-color: " + AppTheme.TOGGLE_INACTIVE + ";" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 4px;"
        );
        bar.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(bar, new Insets(0, 0, 24, 0));
        return bar;
    }




    private TextField buildEmailField() {
        TextField f = new TextField();
        f.setPromptText("votre@email.com");
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
        f.setOnAction(e -> handleLogin());
        VBox.setMargin(f, new Insets(0, 0, 6, 0));
        return f;
    }

    private HBox buildForgotRow() {
        Hyperlink forgot = new Hyperlink("Mot de passe oublié ?");
        forgot.setStyle(
                "-fx-text-fill: " + AppTheme.TEXT_MUTED + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-border-color: transparent;"
        );
        forgot.setOnAction(e -> onGoToForgotPassword.run());
        HBox row = new HBox(forgot);
        row.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(row, new Insets(0, 0, 18, 0));
        return row;
    }

    private Button buildLoginButton() {
        Button btn = new Button("Se connecter");
        btn.setPrefHeight(46);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setDisable(true); // enabled once captcha resolves
        AppTheme.stylePrimaryButton(btn);
        btn.setOnAction(e -> handleLogin());
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



    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty())                        { showError("Veuillez saisir votre email.");          return; }
        if (!email.contains("@"))                   { showError("Adresse e-mail invalide.");              return; }
        if (password == null || password.isEmpty()) { showError("Veuillez saisir votre mot de passe.");  return; }
        if (captchaToken == null)                   { showError("Veuillez valider le reCAPTCHA.");        return; }

        setLoginPending(true);
        new Thread(() -> {
            try {
                AppResponse response = performClientLogin(email, password);
                Platform.runLater(() -> onLoginResponse(response));
            } catch (Exception e) {
                Platform.runLater(() -> onLoginError(e.getMessage()));
            }
        }).start();
    }

    private AppResponse performClientLogin(String email, String password) throws Exception {
        return tcpClient.sendAndParse(new AppRequest.Builder()
                .controller("Auth").action("login")
                .payload(JsonUtils.toJson(Map.of(
                        "email",        email,
                        "password",     password,
                        "captchaToken", captchaToken
                )))
                .build());
    }

    private void onLoginResponse(AppResponse response) {
        setLoginPending(false);
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

    private void onLoginError(String message) {
        setLoginPending(false);
        showError("Erreur réseau : " + message);
        resetCaptcha();
    }



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
                        loginButton.setDisable(false);
                    }
                } else if (captchaToken != null) {
                    captchaToken = null;
                    loginButton.setDisable(true);
                }
            } catch (Exception ignored) {}
        }));
        poller.setCycleCount(Animation.INDEFINITE);
        return poller;
    }

    private void resetCaptcha() {
        captchaToken = null;
        loginButton.setDisable(true);
        try { captchaWebView.getEngine().executeScript("grecaptcha.reset()"); }
        catch (Exception ignored) {}
    }




    private void setLoginPending(boolean pending) {
        loginButton.setDisable(pending);
        loginButton.setText(pending ? "Connexion en cours..." : "Se connecter");
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