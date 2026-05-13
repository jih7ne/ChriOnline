package com.chrionline.adminmodule.admin.ui.views;

import com.chrionline.adminmodule.admin.security.KeyPairManager;
import com.chrionline.adminmodule.admin.security.Signer;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.core.network.protocol.AppResponse;
import com.chrionline.core.theme.AppTheme;
import com.chrionline.core.utils.JsonUtils;
import com.chrionline.network.tcp.TCPClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.net.InetAddress;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Map;
import java.util.function.Consumer;

public class AdminLogin extends StackPane {

    private final TCPClient                         tcpClient;
    private final Consumer<Map<String, Object>>     onLoginSuccess;

    private final TextField                         emailField;
    private final Label                             errorLabel;
    private final Button                            loginButton;



    public AdminLogin(TCPClient tcpClient,
                      Consumer<Map<String, Object>> onLoginSuccess) {

        this.tcpClient      = tcpClient;
        this.onLoginSuccess = onLoginSuccess;

        this.emailField  = buildEmailField();
        this.errorLabel  = buildErrorLabel();
        this.loginButton = buildLoginButton();

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
                fieldLabel("Email"),
                wrapWithIcon("✉", emailField),
                errorLabel,
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
        Label subtitle = new Label("Administration");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        VBox box = new VBox(4, title, subtitle);
        box.setAlignment(Pos.CENTER);
        VBox.setMargin(box, new Insets(0, 0, 24, 0));
        return box;
    }



    private TextField buildEmailField() {
        TextField f = new TextField();
        f.setPromptText("votre@email.com");
        AppTheme.styleTextField(f);
        AppTheme.styleFocusedTextField(f);
        f.setOnAction(e -> handleLogin());
        VBox.setMargin(f, new Insets(0, 0, 14, 0));
        return f;
    }

    private Button buildLoginButton() {
        Button btn = new Button("Demander l'accès  →");
        btn.setPrefHeight(46);
        btn.setMaxWidth(Double.MAX_VALUE);
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
        String email = emailField.getText().trim();
        if (email.isEmpty())       { showError("Veuillez saisir votre email."); return; }
        if (!email.contains("@")) { showError("Adresse e-mail invalide.");      return; }

        setLoginPending(true);
        new Thread(() -> {
            try {
                AppResponse response = performAdminLogin(email);
                Platform.runLater(() -> onLoginResponse(response));
            } catch (Exception e) {
                Platform.runLater(() -> onLoginError(e.getMessage()));
            }
        }).start();
    }

    private AppResponse performAdminLogin(String email) throws Exception {

        AppResponse challengeResp = tcpClient.sendAndParse(new AppRequest.Builder()
                .controller("KeyAuth").action("requestLogin")
                .payload(JsonUtils.toJson(Map.of("email", email)))
                .build());

        if (!challengeResp.isSuccess()) return challengeResp;

        Map<String, Object> data = challengeResp.getDataAs(Map.class);
        String challengeId = (String)   data.get("challengeId");
        String challenge   = (String)   data.get("challenge");
        long   expiresAt   = ((Number)  data.get("expiresAt")).longValue();

        if (challengeId == null || challenge == null)
            throw new IllegalStateException("Incomplete challenge response from server.");
        if (expiresAt <= System.currentTimeMillis())
            throw new IllegalStateException("Challenge already expired.");


        String   deviceName = InetAddress.getLocalHost().getHostName();
        KeyPair  keyPair    = KeyPairManager.loadFromFile(deviceName);
        String   signature  = Base64.getEncoder().encodeToString(Signer.sign(challenge, keyPair.getPrivate()));
        String   fingerprint = KeyPairManager.computeFingerprint(keyPair.getPublic());

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

    private void onLoginResponse(AppResponse response) {
        setLoginPending(false);
        if (response != null && response.isSuccess()) {
            Map<String, Object> data = response.getDataAs(Map.class);
            if (data != null) onLoginSuccess.accept(data);
        } else {
            showError(response != null && response.getMessage() != null
                    ? response.getMessage()
                    : "Connexion échouée. Vérifiez vos identifiants.");
        }
    }

    private void onLoginError(String message) {
        setLoginPending(false);
        showError("Erreur réseau : " + message);
    }



    private void setLoginPending(boolean pending) {
        loginButton.setDisable(pending);
        loginButton.setText(pending ? "Connexion en cours..." : "Demander l'accès  →");
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