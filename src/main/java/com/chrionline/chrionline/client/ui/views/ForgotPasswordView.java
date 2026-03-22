package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.core.theme.AppTheme;
import com.chrionline.chrionline.core.utils.JsonUtils;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.network.tcp.TCPClient;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

/**
 * Forgot-password flow — no email service required.
 *
 * Step 1 → email → server returns the security question  (action: "getquestion")
 * Step 2 → answer the question                           (action: "verifyanswer")
 * Step 3 → enter new password                            (action: "resetpassword")
 *
 * All action names are lowercase to match RequestDispatcher's reflection lookup.
 */
public class ForgotPasswordView extends StackPane {

    // ── Step 1 ─────────────────────────────────────────────────────────────
    private final TextField emailField;

    // ── Step 2 ─────────────────────────────────────────────────────────────
    private final Label     questionLabel;
    private final TextField reponseField;

    // ── Step 3 ─────────────────────────────────────────────────────────────
    private final PasswordField nouveauMdpField;
    private final PasswordField confirmerMdpField;

    // ── Shared ─────────────────────────────────────────────────────────────
    private final Label     errorLabel;
    private final Label     successLabel;
    private final Button    actionButton;
    private final TCPClient tcpClient;
    private final Runnable  onGoToLogin;

    private final VBox step1Container;
    private final VBox step2Container;
    private final VBox step3Container;

    private HBox miniStepBar;
    private int  currentStep    = 1;
    private String confirmedEmail = null;

    public ForgotPasswordView(TCPClient tcpClient, Runnable onGoToLogin) {
        this.tcpClient  = tcpClient;
        this.onGoToLogin = onGoToLogin;

        this.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        // ── Fields ─────────────────────────────────────────────────────────
        emailField        = textField("votre@email.com");
        questionLabel     = new Label("—");
        questionLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + AppTheme.PRIMARY + ";" +
                        "-fx-wrap-text: true; -fx-padding: 12 16 12 16;" +
                        "-fx-background-color: " + AppTheme.FIELD_BG + ";" +
                        "-fx-background-radius: 12; -fx-border-radius: 12;" +
                        "-fx-border-color: " + AppTheme.FIELD_BORDER + "; -fx-border-width: 1.5;"
        );
        questionLabel.setMaxWidth(Double.MAX_VALUE);
        reponseField      = textField("Votre réponse");
        nouveauMdpField   = passField("Nouveau mot de passe");
        confirmerMdpField = passField("Confirmer le mot de passe");

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: " + AppTheme.ERROR_COLOR + "; -fx-font-size: 13px;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        successLabel = new Label();
        successLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 13px; -fx-font-weight: bold;");
        successLabel.setVisible(false);
        successLabel.setWrapText(true);

        // ── Step containers ────────────────────────────────────────────────
        step1Container = stepBox(
                stepTitle("Vérification du compte"),
                hint("Entrez l'e-mail associé à votre compte. Nous vous poserons ensuite une question de sécurité."),
                fieldBox("E-mail", wrapIcon("✉", emailField))
        );

        step2Container = stepBox(
                stepTitle("Question de sécurité"),
                hint("Répondez à la question que vous avez choisie lors de l'inscription."),
                fieldBox("Votre question secrète", questionLabel),
                fieldBox("Votre réponse", wrapIcon("🔑", reponseField))
        );
        step2Container.setVisible(false);
        step2Container.setManaged(false);

        step3Container = stepBox(
                stepTitle("Nouveau mot de passe"),
                hint("Choisissez un mot de passe fort (minimum 6 caractères)."),
                fieldBox("Nouveau mot de passe",         wrapIcon("🔒", nouveauMdpField)),
                fieldBox("Confirmer le mot de passe",    wrapIcon("🔒", confirmerMdpField))
        );
        step3Container.setVisible(false);
        step3Container.setManaged(false);

        // ── Buttons ────────────────────────────────────────────────────────
        actionButton = new Button("Continuer →");
        AppTheme.stylePrimaryButton(actionButton);
        actionButton.setOnAction(e -> handleAction());

        Button backToLogin = new Button("← Retour à la connexion");
        backToLogin.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + AppTheme.TEXT_MUTED + ";" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0;"
        );
        backToLogin.setOnAction(e -> onGoToLogin.run());

        // ── Card ───────────────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setMaxWidth(440);
        AppTheme.styleCard(card);
        card.setPadding(new Insets(40));

        Label icon = new Label("🔐");
        icon.setStyle("-fx-font-size: 36px;");
        VBox iconBox = new VBox(icon);
        iconBox.setAlignment(Pos.CENTER);
        VBox.setMargin(iconBox, new Insets(0, 0, 8, 0));

        Label title = new Label("Mot de passe oublié");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");
        VBox titleBox = new VBox(4, title);
        titleBox.setAlignment(Pos.CENTER);
        VBox.setMargin(titleBox, new Insets(0, 0, 20, 0));

        miniStepBar = buildMiniStepBar();
        VBox.setMargin(miniStepBar, new Insets(0, 0, 16, 0));

        card.getChildren().addAll(
                iconBox,
                titleBox,
                miniStepBar,
                step1Container,
                step2Container,
                step3Container,
                spacer(8),
                errorLabel,
                successLabel,
                spacer(4),
                actionButton,
                spacer(8),
                backToLogin
        );

        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(24));
        wrapper.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + AppTheme.BG + "; -fx-background-color: " + AppTheme.BG + ";");
        this.getChildren().add(scroll);
    }

    // ─── Main action dispatcher ────────────────────────────────────────────

    private void handleAction() {
        hideMessages();
        switch (currentStep) {
            case 1 -> handleGetQuestion();
            case 2 -> handleVerifyAnswer();
            case 3 -> handleResetPassword();
        }
    }

    // ─── Step 1 ───────────────────────────────────────────────────────────

    private void handleGetQuestion() {
        String email = emailField.getText().trim();
        if (!email.contains("@")) { showError("Adresse e-mail invalide."); return; }

        setLoading(true, "Recherche…");
        new Thread(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("email", email);

                // action lowercase: "getquestion"
                AppRequest req = new AppRequest.Builder()
                        .controller("Auth").action("getquestion")
                        .payload(JsonUtils.toJson(payload)).build();

                AppResponse resp = tcpClient.sendAndParse(req);

                Platform.runLater(() -> {
                    setLoading(false, "Continuer →");
                    if (resp != null && resp.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = resp.getDataAs(Map.class);
                        String question = data != null ? (String) data.get("question") : null;
                        if (question != null) {
                            confirmedEmail = email;
                            questionLabel.setText(question);
                            goToStep(2);
                        } else {
                            showError("Impossible de récupérer la question de sécurité.");
                        }
                    } else {
                        showError(resp != null ? resp.getMessage() : "Aucun compte associé à cet e-mail.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> { setLoading(false, "Continuer →"); showError("Erreur réseau : " + e.getMessage()); });
            }
        }).start();
    }

    // ─── Step 2 ───────────────────────────────────────────────────────────

    private void handleVerifyAnswer() {
        String reponse = reponseField.getText().trim();
        if (reponse.isEmpty()) { showError("Veuillez saisir votre réponse."); return; }

        setLoading(true, "Vérification…");
        new Thread(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("email",   confirmedEmail);
                payload.put("reponse", reponse.toLowerCase());

                // action lowercase: "verifyanswer"
                AppRequest req = new AppRequest.Builder()
                        .controller("Auth").action("verifyanswer")
                        .payload(JsonUtils.toJson(payload)).build();

                AppResponse resp = tcpClient.sendAndParse(req);

                Platform.runLater(() -> {
                    setLoading(false, "Continuer →");
                    if (resp != null && resp.isSuccess()) {
                        goToStep(3);
                    } else {
                        showError(resp != null ? resp.getMessage() : "Réponse incorrecte. Veuillez réessayer.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> { setLoading(false, "Continuer →"); showError("Erreur réseau : " + e.getMessage()); });
            }
        }).start();
    }

    // ─── Step 3 ───────────────────────────────────────────────────────────

    private void handleResetPassword() {
        String nouveau   = nouveauMdpField.getText();
        String confirmer = confirmerMdpField.getText();
        if (nouveau.length() < 6)       { showError("Mot de passe trop court (min. 6 caractères)."); return; }
        if (!nouveau.equals(confirmer)) { showError("Les mots de passe ne correspondent pas."); return; }

        setLoading(true, "Enregistrement…");
        new Thread(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("email",   confirmedEmail);
                payload.put("nouveau", nouveau);

                // action lowercase: "resetpassword"
                AppRequest req = new AppRequest.Builder()
                        .controller("Auth").action("resetpassword")
                        .payload(JsonUtils.toJson(payload)).build();

                AppResponse resp = tcpClient.sendAndParse(req);

                Platform.runLater(() -> {
                    setLoading(false, "Enregistrer");
                    if (resp != null && resp.isSuccess()) {
                        showSuccess("Mot de passe mis à jour ! Redirection…");
                        actionButton.setDisable(true);
                        new Thread(() -> {
                            try { Thread.sleep(1800); } catch (InterruptedException ignored) {}
                            Platform.runLater(onGoToLogin);
                        }).start();
                    } else {
                        showError(resp != null ? resp.getMessage() : "Erreur lors de la mise à jour.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> { setLoading(false, "Enregistrer"); showError("Erreur réseau : " + e.getMessage()); });
            }
        }).start();
    }

    // ─── Step navigation ──────────────────────────────────────────────────

    private void goToStep(int step) {
        step1Container.setVisible(false); step1Container.setManaged(false);
        step2Container.setVisible(false); step2Container.setManaged(false);
        step3Container.setVisible(false); step3Container.setManaged(false);

        VBox target = switch (step) {
            case 1  -> step1Container;
            case 2  -> step2Container;
            default -> step3Container;
        };
        target.setVisible(true);
        target.setManaged(true);
        animateIn(target);

        actionButton.setDisable(false);
        actionButton.setText(step == 3 ? "Enregistrer" : "Continuer →");
        currentStep = step;
        updateMiniStepBar(step);
    }

    // ─── Mini step bar ────────────────────────────────────────────────────

    private HBox buildMiniStepBar() {
        HBox bar = new HBox(4);
        bar.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Region seg = new Region();
            seg.setPrefWidth(56);
            seg.setPrefHeight(4);
            seg.setStyle("-fx-background-color: " +
                    (i == 0 ? AppTheme.PRIMARY : AppTheme.TOGGLE_INACTIVE) +
                    "; -fx-background-radius: 2;");
            bar.getChildren().add(seg);
        }
        return bar;
    }

    private void updateMiniStepBar(int activeStep) {
        for (int i = 0; i < miniStepBar.getChildren().size(); i++) {
            Region seg = (Region) miniStepBar.getChildren().get(i);
            seg.setStyle("-fx-background-color: " +
                    (i < activeStep ? AppTheme.PRIMARY : AppTheme.TOGGLE_INACTIVE) +
                    "; -fx-background-radius: 2;");
        }
    }

    // ─── Animation ────────────────────────────────────────────────────────

    private void animateIn(javafx.scene.Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(220), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    // ─── UI helpers ───────────────────────────────────────────────────────

    private TextField textField(String ph) {
        TextField f = new TextField();
        f.setPromptText(ph);
        AppTheme.styleTextField(f);
        return f;
    }

    private PasswordField passField(String ph) {
        PasswordField f = new PasswordField();
        f.setPromptText(ph);
        AppTheme.styleTextField(f);
        return f;
    }

    private StackPane wrapIcon(String emoji, Control ctrl) {
        Label lbl = new Label(emoji);
        lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        StackPane p = new StackPane(ctrl, lbl);
        StackPane.setAlignment(lbl, Pos.CENTER_LEFT);
        lbl.setTranslateX(14);
        ctrl.setMaxWidth(Double.MAX_VALUE);
        return p;
    }

    private VBox fieldBox(String labelTxt, javafx.scene.Node ctrl) {
        Label l = new Label(labelTxt);
        l.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " +
                AppTheme.TEXT_MAIN + "; -fx-padding: 0 0 4 4;");
        VBox b = new VBox(4, l, ctrl);
        VBox.setMargin(b, new Insets(6, 0, 6, 0));
        return b;
    }

    private VBox stepBox(javafx.scene.Node... children) {
        VBox b = new VBox(0);
        b.getChildren().addAll(children);
        return b;
    }

    private Label stepTitle(String txt) {
        Label l = new Label(txt);
        l.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " +
                AppTheme.PRIMARY + "; -fx-padding: 0 0 4 0;");
        return l;
    }

    private Label hint(String txt) {
        Label l = new Label(txt);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: " + AppTheme.TEXT_MUTED + "; -fx-padding: 0 0 12 0;");
        l.setWrapText(true);
        return l;
    }

    private Region spacer(double h) {
        Region r = new Region();
        r.setPrefHeight(h);
        return r;
    }

    private void setLoading(boolean loading, String label) {
        actionButton.setDisable(loading);
        actionButton.setText(label);
    }

    private void showError(String msg)   { errorLabel.setText(msg);   errorLabel.setVisible(true);  successLabel.setVisible(false); }
    private void showSuccess(String msg) { successLabel.setText(msg); successLabel.setVisible(true); errorLabel.setVisible(false);  }
    private void hideMessages()          { errorLabel.setVisible(false); successLabel.setVisible(false); }
}