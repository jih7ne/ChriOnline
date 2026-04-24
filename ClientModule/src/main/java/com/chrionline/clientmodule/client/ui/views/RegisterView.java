package com.chrionline.clientmodule.client.ui.views;

import com.chrionline.clientmodule.utils.CaptchaServer;
import com.chrionline.core.theme.AppTheme;
import com.chrionline.core.utils.JsonUtils;
import com.chrionline.core.network.protocol.AppResponse;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.network.tcp.TCPClient;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import javafx.scene.web.WebEngine;
import javafx.concurrent.Worker;



/**
 * Vue d'inscription en 3 étapes.
 *
 * Sécurité :
 *  - Indicateur visuel de force du mot de passe (4 barres)
 *  - Validation côté client : complexité, nom/prénom, mots de passe communs
 *  - Champ "Confirmer le mot de passe"
 *  - Bouton œil pour afficher/masquer le mot de passe
 *  - Les messages d'erreur renvoyés par le serveur s'affichent directement.
 */
public class RegisterView extends StackPane {

    // ── Règles de complexité (mêmes que PasswordValidator côté serveur) ──
    private static final int    MIN_LENGTH    = 8;
    private static final String SPECIAL_CHARS = "!@#$%^&*()-_=+[]{}|;',.<>?/`~";

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "password1", "password123", "123456", "12345678",
            "123456789", "1234567890", "000000", "111111", "qwerty",
            "azerty", "abc123", "iloveyou", "admin", "letmein",
            "welcome", "monkey", "dragon", "master", "superman",
            "batman", "trustno1", "passw0rd", "p@ssword", "p@ss1234",
            "soleil", "bonjour", "chocolat", "football", "jordan23"
    );

    // ── Champs ────────────────────────────────────────────────────────────
    private final TextField     prenomField;
    private final TextField     nomField;
    private final TextField     emailField;
    private final PasswordField passwordField;
    private final TextField     passwordVisible;   // doublon visible (toggle)
    private final PasswordField confirmField;
    private final TextField     confirmVisible;    // doublon visible (toggle)
    private final ComboBox<String> questionCombo;
    private final TextField        reponseField;
    private final TextField rueField;
    private final TextField codePostalField;
    private final TextField villeField;
    private final Label     errorLabel;
    private final Button    nextButton;
    private final Button    registerButton;
    private final TCPClient tcpClient;
    private final Runnable  onRegisterSuccess;
    private final Runnable  onGoToLogin;
    private final VBox step1Container;
    private final VBox step2Container;
    private final VBox step3Container;
    private final HBox stepIndicator;
    private int currentStep = 1;


    // ── Indicateur de force ───────────────────────────────────────────────
    private final HBox         strengthBar;
    private final Label        strengthLabel;
    private final List<Region> strengthSegments = new ArrayList<>();

    private static final String[] QUESTIONS = {
            "Quel est le prénom de votre mère ?",
            "Quel est le nom de votre premier animal de compagnie ?",
            "Quelle est la ville de naissance de votre père ?",
            "Quel était le nom de votre école primaire ?",
            "Quel est le modèle de votre première voiture ?"
    };
    private WebView captchaWebView;
    private String  captchaToken = null;

    public RegisterView(TCPClient tcpClient, Runnable onRegisterSuccess, Runnable onGoToLogin) {
        this.tcpClient         = tcpClient;
        this.onRegisterSuccess = onRegisterSuccess;
        this.onGoToLogin       = onGoToLogin;

        this.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        prenomField     = textField("Prénom");
        nomField        = textField("Nom");
        emailField      = textField("votre@email.com");
        passwordField   = passField("••••••••");
        passwordVisible = textField("••••••••");
        confirmField    = passField("••••••••");
        confirmVisible  = textField("••••••••");

        // passwordVisible et confirmVisible ne sont jamais managed en même
        // temps que leurs équivalents masqués — ils sont cachés au départ.
        passwordVisible.setVisible(false);
        passwordVisible.setManaged(false);
        confirmVisible.setVisible(false);
        confirmVisible.setManaged(false);

        // Synchronisation bidirectionnelle pour ne pas perdre la saisie
        // lors du toggle.
        passwordField.textProperty().addListener((obs, o, n) -> {
            if (!passwordVisible.isFocused()) passwordVisible.setText(n);
        });
        passwordVisible.textProperty().addListener((obs, o, n) -> {
            if (!passwordField.isFocused()) passwordField.setText(n);
        });
        confirmField.textProperty().addListener((obs, o, n) -> {
            if (!confirmVisible.isFocused()) confirmVisible.setText(n);
        });
        confirmVisible.textProperty().addListener((obs, o, n) -> {
            if (!confirmField.isFocused()) confirmField.setText(n);
        });

        questionCombo = new ComboBox<>();
        questionCombo.getItems().addAll(QUESTIONS);
        questionCombo.setPromptText("Choisir une question…");
        questionCombo.setMaxWidth(Double.MAX_VALUE);
        styleCombo(questionCombo);

        reponseField    = textField("Votre réponse");
        rueField        = textField("Numéro et nom de rue");
        codePostalField = textField("Code postal");
        villeField      = textField("Ville");

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: " + AppTheme.ERROR_COLOR + "; -fx-font-size: 13px;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        // ── Indicateur de force ───────────────────────────────────────────
        strengthBar   = buildStrengthBar();
        strengthLabel = new Label("Entrez un mot de passe");
        strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");

        // Mise à jour de l'indicateur à chaque frappe (sur les deux champs liés)
        passwordField.textProperty().addListener((obs, o, n) -> updateStrengthIndicator(n));
        passwordVisible.textProperty().addListener((obs, o, n) -> updateStrengthIndicator(n));

        VBox rulesBox = buildPasswordRules();

        // ── Champ mot de passe avec bouton œil ───────────────────────────
        StackPane passwordStack   = buildPasswordToggle(passwordField,   passwordVisible);
        StackPane confirmStack    = buildPasswordToggle(confirmField,     confirmVisible);
        // ── WebView reCAPTCHA dans step1 ──────────────────────
        captchaWebView = buildCaptchaWebView();
        // ── Étape 1 ───────────────────────────────────────────────────────
        step1Container = new VBox(0,
                fieldBox("Prénom",                  wrapIcon("👤", prenomField)),
                fieldBox("Nom",                     wrapIcon("👤", nomField)),
                fieldBox("Email",                   wrapIcon("✉",  emailField)),
                fieldBox("Mot de passe",            passwordStack),
                strengthBar,
                strengthLabel,
                rulesBox,
                fieldBox("Confirmer le mot de passe", confirmStack),
                captchaWebView
        );

        // ── Étape 2 ───────────────────────────────────────────────────────
        Label securityHint = new Label(
                "Cette question vous permettra de récupérer votre mot de passe si vous l'oubliez. "
                        + "Elle est obligatoire."
        );
        securityHint.setStyle("-fx-font-size: 12px; -fx-text-fill: " + AppTheme.TEXT_MUTED
                + "; -fx-wrap-text: true; -fx-padding: 0 0 12 0;");
        securityHint.setWrapText(true);

        step2Container = new VBox(0,
                securityHint,
                fieldBox("Question secrète", questionCombo),
                fieldBox("Votre réponse",    wrapIcon("🔑", reponseField))
        );
        step2Container.setVisible(false);
        step2Container.setManaged(false);

        // ── Étape 3 ───────────────────────────────────────────────────────
        Label adresseHint = new Label(
                "Ajoutez votre adresse de livraison principale. "
                        + "Vous pourrez en ajouter d'autres depuis votre profil."
        );
        adresseHint.setStyle("-fx-font-size: 12px; -fx-text-fill: " + AppTheme.TEXT_MUTED
                + "; -fx-wrap-text: true; -fx-padding: 0 0 12 0;");
        adresseHint.setWrapText(true);

        Button skipButton = new Button("Passer pour l'instant");
        skipButton.setStyle(
                "-fx-background-color: transparent; -fx-border-color: " + AppTheme.FIELD_BORDER + ";"
                        + "-fx-border-width: 1.5px; -fx-border-radius: 30px;"
                        + "-fx-text-fill: " + AppTheme.TEXT_MUTED + ";"
                        + "-fx-font-size: 13px; -fx-padding: 12px 24px; -fx-cursor: hand;"
        );
        skipButton.setMaxWidth(Double.MAX_VALUE);
        skipButton.setOnAction(e -> submitRegister());

        step3Container = new VBox(0,
                adresseHint,
                fieldBox("Rue",         wrapIcon("📍", rueField)),
                fieldBox("Code postal", codePostalField),
                fieldBox("Ville",       villeField),
                spacer(8),
                skipButton
        );
        step3Container.setVisible(false);
        step3Container.setManaged(false);

        // ── Boutons ───────────────────────────────────────────────────────
        nextButton = new Button("Continuer →");
        AppTheme.stylePrimaryButton(nextButton);
        nextButton.setOnAction(e -> handleNext());

        registerButton = new Button("S'inscrire");
        AppTheme.stylePrimaryButton(registerButton);
        registerButton.setVisible(false);
        registerButton.setManaged(false);
        registerButton.setOnAction(e -> submitRegister());

        Button backButton = new Button("← Retour");
        backButton.setStyle("-fx-background-color: transparent; -fx-text-fill: " + AppTheme.PRIMARY
                + "; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0;");
        backButton.setOnAction(e -> handleBack());

        HBox backRow = new HBox(backButton);
        backRow.setAlignment(Pos.CENTER_LEFT);
        backRow.setVisible(false);
        backRow.setManaged(false);
        VBox.setMargin(backRow, new Insets(0, 0, 8, 0));

        Label stepTitle = new Label("Informations personnelles");
        stepTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: "
                + AppTheme.PRIMARY + "; -fx-padding: 0 0 12 0;");

        stepIndicator = buildStepIndicator();

        // ── Card ──────────────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setMaxWidth(480);
        AppTheme.styleCard(card);
        card.setPadding(new Insets(40));

        Label icon   = new Label("🛍");  icon.setStyle("-fx-font-size: 40px;");
        VBox iconBox = new VBox(icon);   iconBox.setAlignment(Pos.CENTER);
        VBox.setMargin(iconBox, new Insets(0, 0, 8, 0));

        Label title    = new Label("ChriOnline");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");
        Label subtitle = new Label("Boutique artisanale");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        VBox titleBox  = new VBox(4, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);
        VBox.setMargin(titleBox, new Insets(0, 0, 16, 0));

        Button btnConnexion   = new Button("Connexion");
        Button btnInscription = new Button("Inscription");
        AppTheme.styleToggleInactive(btnConnexion);
        AppTheme.styleToggleActive(btnInscription);
        btnConnexion.setOnAction(e -> onGoToLogin.run());
        btnConnexion.setMaxWidth(Double.MAX_VALUE);
        btnInscription.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnConnexion,   Priority.ALWAYS);
        HBox.setHgrow(btnInscription, Priority.ALWAYS);
        HBox toggle = new HBox(0, btnConnexion, btnInscription);
        toggle.setStyle("-fx-background-color: " + AppTheme.TOGGLE_INACTIVE
                + "; -fx-background-radius: 30px; -fx-padding: 4px;");
        toggle.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(toggle, new Insets(0, 0, 16, 0));

        card.getChildren().addAll(
                iconBox, titleBox, toggle,
                stepIndicator, spacer(8),
                backRow, stepTitle,
                step1Container, step2Container, step3Container,
                spacer(8), errorLabel,
                nextButton, registerButton
        );

        nextButton.setUserData(new Object[]{ backRow, stepTitle });
        backButton.setUserData(new Object[]{ backRow, stepTitle });

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

    // ── Bouton œil (afficher / masquer) ───────────────────────────────────

    /**
     * Crée un StackPane contenant le champ masqué, le champ visible, et un
     * bouton œil aligné à droite qui bascule entre les deux.
     */
    private StackPane buildPasswordToggle(PasswordField masked, TextField visible) {
        masked.setMaxWidth(Double.MAX_VALUE);
        visible.setMaxWidth(Double.MAX_VALUE);

        Button eyeBtn = new Button("👁");
        eyeBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent;"
                        + "-fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0 12 0 0;"
        );
        eyeBtn.setFocusTraversable(false);

        eyeBtn.setOnAction(e -> {
            boolean showing = visible.isVisible();
            // Passer en visible
            masked.setVisible(showing);
            masked.setManaged(showing);
            visible.setVisible(!showing);
            visible.setManaged(!showing);
            eyeBtn.setText(showing ? "🙈" : "👁");
            if (!showing) visible.requestFocus();
            else          masked.requestFocus();
        });

        StackPane pane = new StackPane(masked, visible, eyeBtn);
        StackPane.setAlignment(eyeBtn, Pos.CENTER_RIGHT);
        return pane;
    }

    // ── Indicateur de force ────────────────────────────────────────────────

    private HBox buildStrengthBar() {
        HBox bar = new HBox(4);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 0, 2, 4));
        for (int i = 0; i < 4; i++) {
            Region seg = new Region();
            seg.setPrefWidth(60);
            seg.setPrefHeight(4);
            seg.setStyle("-fx-background-color: " + AppTheme.FIELD_BORDER + "; -fx-background-radius: 2;");
            strengthSegments.add(seg);
            bar.getChildren().add(seg);
        }
        return bar;
    }

    private VBox buildPasswordRules() {
        VBox box = new VBox(3);
        box.setPadding(new Insets(4, 0, 4, 4));
        box.getChildren().addAll(
                ruleLabel("Au moins 8 caractères"),
                ruleLabel("Une lettre majuscule"),
                ruleLabel("Une lettre minuscule"),
                ruleLabel("Un chiffre"),
                ruleLabel("Un caractère spécial (!@#$...)"),
                ruleLabel("Ne doit pas contenir votre nom ou prénom")
        );
        return box;
    }

    private Label ruleLabel(String text) {
        Label l = new Label("• " + text);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        return l;
    }

    private void updateStrengthIndicator(String password) {
        int score = computeStrength(password);
        String[] colors = { "#dc2626", "#f97316", "#eab308", "#22c55e" };
        String[] labels = { "Très faible", "Faible", "Moyen", "Fort" };
        String color = score == 0 ? AppTheme.FIELD_BORDER : colors[score - 1];

        for (int i = 0; i < strengthSegments.size(); i++) {
            strengthSegments.get(i).setStyle(
                    "-fx-background-color: " + (i < score ? color : AppTheme.FIELD_BORDER)
                            + "; -fx-background-radius: 2;"
            );
        }

        if (password == null || password.isEmpty()) {
            strengthLabel.setText("Entrez un mot de passe");
            strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        } else {
            strengthLabel.setText(labels[Math.max(0, score - 1)]);
            strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + ";");
        }
    }

    /** Score 0 (vide) → 4 (fort). Ne prend pas en compte nom/prénom ici. */
    private int computeStrength(String p) {
        if (p == null || p.isEmpty()) return 0;
        int score = 0;
        if (p.length() >= MIN_LENGTH) score++;
        if (p.chars().anyMatch(Character::isUpperCase)) score++;
        if (p.chars().anyMatch(Character::isDigit))     score++;
        if (p.chars().anyMatch(c -> SPECIAL_CHARS.indexOf(c) >= 0)) score++;
        return score;
    }

    // ── Validation locale ─────────────────────────────────────────────────

    private boolean validateStep1() {
        if (prenomField.getText().trim().isEmpty()) { showError("Veuillez saisir votre prénom."); return false; }
        if (nomField.getText().trim().isEmpty())     { showError("Veuillez saisir votre nom."); return false; }
        if (!emailField.getText().trim().contains("@")) { showError("Adresse e-mail invalide."); return false; }
        if (captchaToken == null) { showError("Veuillez valider le reCAPTCHA."); return false; }
        String password = passwordField.getText();
        List<String> errors = validatePasswordLocally(
                password,
                prenomField.getText().trim(),
                nomField.getText().trim()
        );
        if (!errors.isEmpty()) { showError(errors.get(0)); return false; }

        // Confirmation
        String confirm = confirmField.getText();
        if (!password.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas.");
            return false;
        }

        return true;
    }

    private boolean validateStep2() {
        if (questionCombo.getValue() == null)        { showError("Veuillez choisir une question secrète."); return false; }
        if (reponseField.getText().trim().isEmpty())  { showError("Veuillez saisir votre réponse secrète."); return false; }
        return true;
    }

    /**
     * Validation locale des règles de complexité.
     * Identique à PasswordValidator.validate(password, prenom, nom) côté serveur.
     */
    private List<String> validatePasswordLocally(String password, String prenom, String nom) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.length() < MIN_LENGTH) {
            errors.add("Le mot de passe doit contenir au moins " + MIN_LENGTH + " caractères.");
        }
        if (password != null && !password.chars().anyMatch(Character::isUpperCase)) {
            errors.add("Le mot de passe doit contenir au moins une lettre majuscule.");
        }
        if (password != null && !password.chars().anyMatch(Character::isLowerCase)) {
            errors.add("Le mot de passe doit contenir au moins une lettre minuscule.");
        }
        if (password != null && !password.chars().anyMatch(Character::isDigit)) {
            errors.add("Le mot de passe doit contenir au moins un chiffre.");
        }
        if (password != null && password.chars().noneMatch(c -> SPECIAL_CHARS.indexOf(c) >= 0)) {
            errors.add("Le mot de passe doit contenir au moins un caractère spécial.");
        }

        if (password != null) {
            String lower = password.toLowerCase();

            if (prenom != null && !prenom.isBlank()) {
                String prenomLower = prenom.trim().toLowerCase();
                if (prenomLower.length() >= 3 && lower.contains(prenomLower)) {
                    errors.add("Le mot de passe ne doit pas contenir votre prénom.");
                }
            }
            if (nom != null && !nom.isBlank()) {
                String nomLower = nom.trim().toLowerCase();
                if (nomLower.length() >= 3 && lower.contains(nomLower)) {
                    errors.add("Le mot de passe ne doit pas contenir votre nom.");
                }
            }
            if (COMMON_PASSWORDS.contains(lower)) {
                errors.add("Ce mot de passe est trop courant. Choisissez-en un plus original.");
            }
        }

        return errors;
    }

    // ── Navigation ────────────────────────────────────────────────────────

    private void handleNext() {
        hideError();
        Object[] refs = (Object[]) nextButton.getUserData();
        HBox  backRow   = (HBox)  refs[0];
        Label stepTitle = (Label) refs[1];

        if (currentStep == 1) {
            if (!validateStep1()) return;
            goToStep(2, backRow, stepTitle, "Question de sécurité");
        } else if (currentStep == 2) {
            if (!validateStep2()) return;
            goToStep(3, backRow, stepTitle, "Adresse de livraison");
            nextButton.setVisible(false);    nextButton.setManaged(false);
            registerButton.setVisible(true); registerButton.setManaged(true);
        }
    }

    private void handleBack() {
        hideError();
        Object[] refs = (Object[]) nextButton.getUserData();
        HBox  backRow   = (HBox)  refs[0];
        Label stepTitle = (Label) refs[1];

        if (currentStep == 3) {
            goToStep(2, backRow, stepTitle, "Question de sécurité");
            nextButton.setVisible(true);      nextButton.setManaged(true);
            registerButton.setVisible(false); registerButton.setManaged(false);
        } else if (currentStep == 2) {
            goToStep(1, backRow, stepTitle, "Informations personnelles");
            backRow.setVisible(false); backRow.setManaged(false);
        }
    }

    private void goToStep(int step, HBox backRow, Label stepTitle, String title) {
        getStepContainer(currentStep).setVisible(false);
        getStepContainer(currentStep).setManaged(false);
        currentStep = step;
        VBox target = getStepContainer(step);
        target.setVisible(true); target.setManaged(true);
        animateIn(target);
        stepTitle.setText(title);
        updateStepIndicator(step);
        backRow.setVisible(step > 1); backRow.setManaged(step > 1);
    }

    private VBox getStepContainer(int step) {
        return switch (step) { case 1 -> step1Container; case 2 -> step2Container; default -> step3Container; };
    }

    // ── Soumission ────────────────────────────────────────────────────────

    private void submitRegister() {
        String prenom    = prenomField.getText().trim();
        String nom       = nomField.getText().trim();
        String email     = emailField.getText().trim();
        String password  = passwordField.getText();
        String question  = questionCombo.getValue();
        String reponse   = reponseField.getText().trim();
        String rue        = rueField.getText().trim();
        String codePostal = codePostalField.getText().trim();
        String ville      = villeField.getText().trim();

        registerButton.setDisable(true);
        registerButton.setText("Inscription…");
        hideError();

        new Thread(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("nom",             nom);
                payload.put("prenom",          prenom);
                payload.put("email",           email);
                payload.put("password",        password);
                payload.put("questionSecrete", question);
                payload.put("reponseSecrete",  reponse.toLowerCase());

                if (!rue.isEmpty() && !codePostal.isEmpty() && !ville.isEmpty()) {
                    Map<String, String> adresse = new HashMap<>();
                    adresse.put("rue",            rue);
                    adresse.put("code_postal",    codePostal);
                    adresse.put("ville",          ville);
                    adresse.put("pays",           "Maroc");
                    adresse.put("est_principale", "true");
                    payload.put("adresse", adresse);
                }
                payload.put("captchaToken", captchaToken);

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
                        resetCaptcha();
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

    // ── Step indicator ────────────────────────────────────────────────────

    private HBox buildStepIndicator() {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(0, 0, 16, 0));
        String[] labels = { "Profil", "Sécurité", "Adresse" };
        for (int i = 0; i < labels.length; i++) {
            boolean active = (i == 0);
            Circle circle = new Circle(14);
            circle.setFill(active ? Color.web(AppTheme.PRIMARY) : Color.web(AppTheme.TOGGLE_INACTIVE));
            Label num = new Label(String.valueOf(i + 1));
            num.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: "
                    + (active ? "white" : AppTheme.TEXT_MUTED) + ";");
            StackPane dot = new StackPane(circle, num);
            dot.setPrefSize(28, 28);
            Label lbl = new Label(labels[i]);
            lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: "
                    + (active ? AppTheme.PRIMARY : AppTheme.TEXT_MUTED) + ";");
            VBox step = new VBox(4, dot, lbl);
            step.setAlignment(Pos.CENTER);
            bar.getChildren().add(step);
            if (i < labels.length - 1) {
                Region line = new Region();
                line.setPrefWidth(48); line.setPrefHeight(2);
                line.setStyle("-fx-background-color: " + AppTheme.TOGGLE_INACTIVE + "; -fx-background-radius: 2;");
                VBox lineWrap = new VBox(line);
                lineWrap.setAlignment(Pos.CENTER);
                lineWrap.setPadding(new Insets(0, 0, 16, 0));
                bar.getChildren().add(lineWrap);
            }
        }
        return bar;
    }

    private void updateStepIndicator(int activeStep) {
        for (int i = 0; i < 3; i++) {
            VBox  stepBox = (VBox)      stepIndicator.getChildren().get(i * 2);
            StackPane dot = (StackPane) stepBox.getChildren().get(0);
            Circle circle = (Circle)    dot.getChildren().get(0);
            Label  num    = (Label)     dot.getChildren().get(1);
            Label  lbl    = (Label)     stepBox.getChildren().get(1);
            boolean done   = (i + 1 < activeStep);
            boolean active = (i + 1 == activeStep);
            if (done) {
                circle.setFill(Color.web(AppTheme.PRIMARY));
                num.setText("✓");
                num.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;");
            } else if (active) {
                circle.setFill(Color.web(AppTheme.PRIMARY));
                num.setText(String.valueOf(i + 1));
                num.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;");
            } else {
                circle.setFill(Color.web(AppTheme.TOGGLE_INACTIVE));
                num.setText(String.valueOf(i + 1));
                num.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
            }
            lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: "
                    + (active || done ? AppTheme.PRIMARY : AppTheme.TEXT_MUTED) + ";");
            if (i < 2) {
                VBox  lineWrap = (VBox)   stepIndicator.getChildren().get(i * 2 + 1);
                Region line    = (Region) lineWrap.getChildren().get(0);
                line.setStyle("-fx-background-color: "
                        + (done ? AppTheme.PRIMARY : AppTheme.TOGGLE_INACTIVE) + "; -fx-background-radius: 2;");
            }
        }
    }

    // ── Animation ─────────────────────────────────────────────────────────

    private void animateIn(javafx.scene.Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(220), node);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(220), node);
        tt.setFromX(24); tt.setToX(0);
        ft.play(); tt.play();
    }

    // ── UI helpers ────────────────────────────────────────────────────────

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

    private StackPane wrapIcon(String emoji, Control field) {
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size: 14px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        StackPane pane = new StackPane(field, icon);
        StackPane.setAlignment(icon, Pos.CENTER_LEFT);
        icon.setTranslateX(14);
        field.setMaxWidth(Double.MAX_VALUE);
        return pane;
    }

    private VBox fieldBox(String labelText, javafx.scene.Node field) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: "
                + AppTheme.TEXT_MAIN + "; -fx-padding: 0 0 4 4;");
        VBox box = new VBox(4, lbl, field);
        VBox.setMargin(box, new Insets(6, 0, 6, 0));
        return box;
    }

    private Region spacer(double h) {
        Region r = new Region(); r.setPrefHeight(h); return r;
    }

    private void styleCombo(ComboBox<String> combo) {
        combo.setStyle(
                "-fx-background-color: " + AppTheme.FIELD_BG + ";"
                        + "-fx-border-color: " + AppTheme.FIELD_BORDER + ";"
                        + "-fx-border-radius: 30px; -fx-background-radius: 30px;"
                        + "-fx-padding: 4px 12px; -fx-font-size: 13px;"
                        + "-fx-text-fill: " + AppTheme.TEXT_MAIN + ";"
        );
    }

    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
    private void hideError()           { errorLabel.setVisible(false); }
    private WebView buildCaptchaWebView() {
        WebView wv = new WebView();
        wv.setPrefSize(400, 560);
        wv.setMinSize(400, 560);
        wv.setMaxSize(400, 560);
        VBox.setMargin(wv, new Insets(10, 0, 12, 0));

        WebEngine engine = wv.getEngine();

        try {
            int port = CaptchaServer.start();
            engine.load("http://localhost:" + port + "/recaptcha");
        } catch (Exception e) {
            System.err.println("Erreur démarrage serveur captcha : " + e.getMessage());
        }

        // Polling toutes les 500 ms — plus fiable que le bridge JS
        Timeline poller = new Timeline(new KeyFrame(Duration.millis(500), ev -> {
            try {
                Object result = engine.executeScript(
                    "(typeof grecaptcha !== 'undefined' && typeof grecaptcha.getResponse === 'function')"
                    + " ? grecaptcha.getResponse() : ''"
                );
                String token = (result instanceof String) ? (String) result : "";
                if (!token.isEmpty()) {
                    captchaToken = token;
                } else {
                    captchaToken = null;
                }
            } catch (Exception ignored) {}
        }));
        poller.setCycleCount(Animation.INDEFINITE);

        engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                poller.play();
            }
        });

        return wv;
    }

    private void resetCaptcha() {
        captchaToken = null;
        try {
            captchaWebView.getEngine().executeScript("grecaptcha.reset()");
        } catch (Exception ignored) {}
    }
}
