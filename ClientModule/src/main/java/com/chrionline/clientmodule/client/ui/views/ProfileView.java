package com.chrionline.clientmodule.client.ui.views;

import com.chrionline.clientmodule.client.ui.components.ClientNavbar;
import com.chrionline.core.interfaces.ViewManager;
import com.chrionline.core.theme.AppTheme;
import com.chrionline.core.utils.JsonUtils;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.core.network.protocol.AppResponse;
import com.chrionline.network.tcp.TCPClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileView extends BorderPane {

    private final TCPClient client;
    private final Map<String, Object> userData;
    private final ViewManager viewManager;

    private TextField nomField;
    private TextField prenomField;
    private Label profilFeedbackLabel;

    private PasswordField ancienMdpField;
    private PasswordField nouveauMdpField;
    private PasswordField confirmerMdpField;
    private Label mdpFeedbackLabel;

    private VBox adressesContainer;
    private List<Map<String, Object>> adressesList = new ArrayList<>();

    public ProfileView(TCPClient client, Map<String, Object> userData, ViewManager viewManager) {
        this.client      = client;
        this.userData    = userData;
        this.viewManager = viewManager;

        setStyle("-fx-background-color: " + AppTheme.BG + ";");

        ClientNavbar navbar = new ClientNavbar(0, userData, viewManager, null);
        setTop(navbar);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background: " + AppTheme.BG + ";" +
                        "-fx-background-color: " + AppTheme.BG + ";"
        );

        VBox root = new VBox(28);
        root.setPadding(new Insets(32, 64, 48, 64));
        root.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        Button retourBtn = new Button("← Retour au catalogue");
        retourBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + AppTheme.PRIMARY + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0;"
        );
        retourBtn.setOnMouseEntered(e -> retourBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + AppTheme.TEXT_MAIN + ";" +
                        "-fx-font-size: 13px; -fx-cursor: hand;" +
                        "-fx-underline: true; -fx-padding: 0;"
        ));
        retourBtn.setOnMouseExited(e -> retourBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + AppTheme.PRIMARY + ";" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0;"
        ));
        retourBtn.setOnAction(e -> viewManager.showCatalogueView(userData));

        Label titre = new Label("Mon profil");
        titre.setStyle(
                "-fx-font-size: 28px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + AppTheme.PRIMARY + ";"
        );

        VBox cardInfos    = buildCardInfosPersonnelles();
        VBox cardMdp      = buildCardMotDePasse();
        VBox cardAdresses = buildCardAdresses();
        VBox card2FA      = buildCard2FA();          // ← NOUVEAU card dédié

        root.getChildren().addAll(retourBtn, titre, cardInfos, cardMdp, card2FA, cardAdresses);
        scroll.setContent(root);
        setCenter(scroll);

        chargerAdresses();
    }

    // =========================================================================
    // CARD INFOS PERSONNELLES
    // =========================================================================
    private VBox buildCardInfosPersonnelles() {
        VBox card = buildCard();
        Label sectionTitre = sectionLabel("Informations personnelles");

        String emailVal = userData.getOrDefault("email", "").toString();
        HBox emailRow = new HBox(8);
        emailRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon lockIcon = new FontIcon(Feather.LOCK);
        lockIcon.setIconSize(14);
        lockIcon.setIconColor(Color.web(AppTheme.TEXT_MUTED));
        TextField emailReadOnly = new TextField(emailVal);
        emailReadOnly.setEditable(false);
        emailReadOnly.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(emailReadOnly, Priority.ALWAYS);
        emailReadOnly.setStyle(
                "-fx-background-color: " + AppTheme.FIELD_BG + ";" +
                        "-fx-border-color: " + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                        "-fx-padding: 11px 14px; -fx-font-size: 14px;" +
                        "-fx-text-fill: " + AppTheme.TEXT_MUTED + "; -fx-opacity: 0.7;"
        );
        Label emailLockHint = new Label("L'email ne peut pas être modifié");
        emailLockHint.setStyle("-fx-font-size: 11px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        emailRow.getChildren().addAll(lockIcon, emailReadOnly);

        nomField    = buildTextField(userData.getOrDefault("nom",    "").toString(), "Nom");
        prenomField = buildTextField(userData.getOrDefault("prenom", "").toString(), "Prénom");

        VBox nomBox    = buildFieldBox("Nom",    nomField);
        VBox prenomBox = buildFieldBox("Prénom", prenomField);
        HBox.setHgrow(nomBox,    Priority.ALWAYS);
        HBox.setHgrow(prenomBox, Priority.ALWAYS);
        HBox nomPrenomRow = new HBox(14, nomBox, prenomBox);

        profilFeedbackLabel = new Label();
        profilFeedbackLabel.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: " + AppTheme.ERROR_COLOR + ";"
        );
        profilFeedbackLabel.setVisible(false);
        profilFeedbackLabel.setWrapText(true);

        Button saveBtn = new Button("Enregistrer les modifications");
        AppTheme.stylePrimaryButton(saveBtn);
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> handleUpdateProfil());

        card.getChildren().addAll(
                sectionTitre,
                fieldLabel("Email (non modifiable)"), emailRow, emailLockHint,
                nomPrenomRow,
                profilFeedbackLabel,
                saveBtn
        );
        return card;
    }

    private void handleUpdateProfil() {
        String nom    = nomField.getText().trim();
        String prenom = prenomField.getText().trim();

        if (nom.isEmpty() || prenom.isEmpty()) {
            showFeedback(profilFeedbackLabel, "Le nom et le prénom sont requis.", false);
            return;
        }
        new Thread(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("nom",    nom);
                payload.put("prenom", prenom);

                AppRequest req = new AppRequest.Builder()
                        .controller("Auth").action("updateprofil")
                        .payload(JsonUtils.toJson(payload))
                        .authToken(client.getAuthToken())
                        .build();

                AppResponse resp = client.sendAndParse(req);
                Platform.runLater(() -> {
                    if (resp != null && resp.isSuccess()) {
                        userData.put("nom",    nom);
                        userData.put("prenom", prenom);
                        showFeedback(profilFeedbackLabel, "Profil mis à jour avec succès !", true);
                    } else {
                        showFeedback(profilFeedbackLabel,
                                resp != null ? resp.getMessage() : "Erreur réseau.", false);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() ->
                        showFeedback(profilFeedbackLabel, "Erreur réseau : " + ex.getMessage(), false));
            }
        }).start();
    }

    // =========================================================================
    // CARD MOT DE PASSE
    // =========================================================================
    private VBox buildCardMotDePasse() {
        VBox card = buildCard();
        Label sectionTitre = sectionLabel("Changer le mot de passe");

        ancienMdpField    = buildPasswordField("Mot de passe actuel");
        nouveauMdpField   = buildPasswordField("Nouveau mot de passe");
        confirmerMdpField = buildPasswordField("Confirmer le nouveau mot de passe");

        mdpFeedbackLabel = new Label();
        mdpFeedbackLabel.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: " + AppTheme.ERROR_COLOR + ";"
        );
        mdpFeedbackLabel.setVisible(false);
        mdpFeedbackLabel.setWrapText(true);

        Button mdpBtn = new Button("Changer le mot de passe");
        AppTheme.stylePrimaryButton(mdpBtn);
        mdpBtn.setMaxWidth(Double.MAX_VALUE);
        mdpBtn.setOnAction(e -> handleUpdatePassword());

        card.getChildren().addAll(
                sectionTitre,
                buildFieldBox("Mot de passe actuel",               ancienMdpField),
                buildFieldBox("Nouveau mot de passe",              nouveauMdpField),
                buildFieldBox("Confirmer le nouveau mot de passe", confirmerMdpField),
                mdpFeedbackLabel,
                mdpBtn
        );
        return card;
    }

    private void handleUpdatePassword() {
        String ancien    = ancienMdpField.getText();
        String nouveau   = nouveauMdpField.getText();
        String confirmer = confirmerMdpField.getText();

        if (ancien.isEmpty() || nouveau.isEmpty() || confirmer.isEmpty()) {
            showFeedback(mdpFeedbackLabel, "Tous les champs sont requis.", false); return;
        }
        if (nouveau.length() < 6) {
            showFeedback(mdpFeedbackLabel, "Au moins 6 caractères.", false); return;
        }
        if (!nouveau.equals(confirmer)) {
            showFeedback(mdpFeedbackLabel, "Les mots de passe ne correspondent pas.", false); return;
        }
        new Thread(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("ancien",  ancien);
                payload.put("nouveau", nouveau);

                AppRequest req = new AppRequest.Builder()
                        .controller("Auth").action("updatepassword")
                        .payload(JsonUtils.toJson(payload))
                        .authToken(client.getAuthToken())
                        .build();

                AppResponse resp = client.sendAndParse(req);
                Platform.runLater(() -> {
                    if (resp != null && resp.isSuccess()) {
                        ancienMdpField.clear();
                        nouveauMdpField.clear();
                        confirmerMdpField.clear();
                        showFeedback(mdpFeedbackLabel, "Mot de passe modifié. Reconnexion requise...", true);

                        // Redirection automatique après 2 secondes
                        new Timeline(new KeyFrame(Duration.seconds(2), ev -> viewManager.showLoginView())).play();
                    } else {
                        showFeedback(mdpFeedbackLabel,
                                resp != null ? resp.getMessage() : "Erreur réseau.", false);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() ->
                        showFeedback(mdpFeedbackLabel, "Erreur réseau : " + ex.getMessage(), false));
            }
        }).start();
    }

    // =========================================================================
    // CARD 2FA — NOUVEAU
    // =========================================================================
    private VBox buildCard2FA() {
        VBox card = buildCard();
        Label sectionTitre = sectionLabel("Double authentification (2FA)");

        Label hint = new Label(
                "Protégez votre compte avec Google Authenticator. " +
                        "Une fois activé, un code sera demandé à chaque connexion."
        );
        hint.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        hint.setWrapText(true);

        Label feedbackLabel = new Label();
        feedbackLabel.setStyle("-fx-font-size: 13px;");
        feedbackLabel.setVisible(false);
        feedbackLabel.setWrapText(true);

        Button btn2FA = new Button("🔐 Activer le 2FA");
        AppTheme.stylePrimaryButton(btn2FA);
        btn2FA.setMaxWidth(Double.MAX_VALUE);
        btn2FA.setDisable(true); // désactivé par défaut en attendant le chargement

        Button disableBtn = new Button("🔓 Désactiver le 2FA");
        disableBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #C0392B; -fx-border-width: 1.5;" +
                        "-fx-border-radius: 20; -fx-text-fill: #C0392B;" +
                        "-fx-font-size: 14px; -fx-padding: 10 24; -fx-cursor: hand;"
        );
        disableBtn.setMaxWidth(Double.MAX_VALUE);
        disableBtn.setDisable(true); // désactivé par défaut en attendant le chargement

        btn2FA.setOnAction(e -> initEnable2FA(feedbackLabel, btn2FA, disableBtn));
        disableBtn.setOnAction(e -> handleDisable2FA(feedbackLabel, btn2FA, disableBtn));

        card.getChildren().addAll(sectionTitre, hint, feedbackLabel, btn2FA, disableBtn);

        // Charger le statut 2FA au lancement
        charger2FAStatus(btn2FA, disableBtn);

        return card;
    }
    private void charger2FAStatus(Button btnActiver, Button btnDesactiver) {
        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("Auth").action("twofactorstatus")
                        .authToken(client.getAuthToken())
                        .build();
                AppResponse resp = client.sendAndParse(req);
                Platform.runLater(() -> {
                    if (resp != null && resp.isSuccess()) {
                        Object raw = resp.getData();
                        boolean enabled = false;
                        if (raw instanceof Map) {
                            Object val = ((Map<?, ?>) raw).get("enabled");
                            enabled = Boolean.TRUE.equals(val) || "true".equals(String.valueOf(val));
                        }
                        btnActiver.setDisable(enabled);
                        btnDesactiver.setDisable(!enabled);
                    } else {
                        btnActiver.setDisable(false);
                        btnDesactiver.setDisable(true);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    btnActiver.setDisable(false);
                    btnDesactiver.setDisable(true);
                });
            }
        }).start();
    }

    // =========================================================================
    // CARD ADRESSES
    // =========================================================================
    private VBox buildCardAdresses() {
        VBox card = buildCard();

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label sectionTitre = sectionLabel("Mes adresses");
        HBox.setHgrow(sectionTitre, Priority.ALWAYS);

        Button ajouterBtn = new Button("+ Ajouter");
        ajouterBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: " + AppTheme.PRIMARY + ";" +
                        "-fx-border-width: 1.5; -fx-border-radius: 20;" +
                        "-fx-text-fill: " + AppTheme.PRIMARY + ";" +
                        "-fx-font-size: 13px; -fx-padding: 6 16; -fx-cursor: hand;"
        );
        ajouterBtn.setOnAction(e -> openAdresseDialog(null));
        headerRow.getChildren().addAll(sectionTitre, ajouterBtn);

        adressesContainer = new VBox(10);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: " + AppTheme.FIELD_BORDER + ";");

        Button logoutBtn = new Button("Se déconnecter");
        logoutBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #C0392B; -fx-border-width: 1.5;" +
                        "-fx-border-radius: 20; -fx-text-fill: #C0392B;" +
                        "-fx-font-size: 14px; -fx-padding: 10 24; -fx-cursor: hand;"
        );
        logoutBtn.setOnAction(e -> handleLogout());

        card.getChildren().addAll(headerRow, adressesContainer, sep, logoutBtn);
        return card;
    }

    private void chargerAdresses() {
        int idUtilisateur = ((Number) userData.get("id")).intValue();
        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("Adresse").action("lister")
                        .parameter("idUtilisateur", idUtilisateur)
                        .authToken(client.getAuthToken())
                        .build();
                AppResponse resp = client.sendAndParse(req);
                Platform.runLater(() -> {
                    if (resp != null && resp.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> adresses = resp.getDataAs(List.class);
                        adressesList = adresses != null ? adresses : new ArrayList<>();
                        afficherAdresses();
                    }
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    private void afficherAdresses() {
        adressesContainer.getChildren().clear();
        if (adressesList.isEmpty()) {
            Label vide = new Label("Aucune adresse enregistrée.");
            vide.setStyle("-fx-text-fill: " + AppTheme.TEXT_MUTED + "; -fx-font-size: 13px;");
            adressesContainer.getChildren().add(vide);
            return;
        }
        for (Map<String, Object> adresse : adressesList) {
            adressesContainer.getChildren().add(buildAdresseRow(adresse));
        }
    }

    private HBox buildAdresseRow(Map<String, Object> adresse) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setStyle(
                "-fx-background-color: " + AppTheme.FIELD_BG + ";" +
                        "-fx-background-radius: 10; -fx-border-color: " + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius: 10; -fx-border-width: 1;"
        );

        FontIcon adresseIcon = new FontIcon(Feather.MAP_PIN);
        adresseIcon.setIconSize(16);
        adresseIcon.setIconColor(Color.web(AppTheme.PRIMARY));

        String  rue        = str(adresse.get("rue"));
        String  ville      = str(adresse.get("ville"));
        String  cp         = str(adresse.get("code_postal"));
        String  pays       = str(adresse.get("pays"));
        boolean principale = estPrincipale(adresse);

        VBox texteBox = new VBox(3);
        HBox.setHgrow(texteBox, Priority.ALWAYS);
        Label ligneRue = new Label(rue);
        ligneRue.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: "
                + AppTheme.TEXT_MAIN + ";");
        Label ligneVille = new Label(cp + " " + ville + ", " + pays);
        ligneVille.setStyle("-fx-font-size: 12px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        texteBox.getChildren().addAll(ligneRue, ligneVille);

        if (principale) {
            Label badge = new Label("Principale");
            badge.setStyle(
                    "-fx-background-color: " + AppTheme.PRIMARY + ";" +
                            "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;" +
                            "-fx-padding: 2 8; -fx-background-radius: 10;"
            );
            texteBox.getChildren().add(badge);
        }

        Button starBtn = new Button();
        FontIcon starIcon = new FontIcon(Feather.STAR);
        starIcon.setIconSize(15);
        starIcon.setIconColor(principale ? Color.web("#F59E0B") : Color.web(AppTheme.TEXT_MUTED));
        starBtn.setGraphic(starIcon);
        starBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;");
        starBtn.setTooltip(new Tooltip(principale ? "Adresse principale" : "Définir comme principale"));
        if (!principale) starBtn.setOnAction(e -> handleSetPrincipale(adresse));

        Button editBtn = new Button();
        FontIcon editIcon = new FontIcon(Feather.EDIT_2);
        editIcon.setIconSize(15);
        editIcon.setIconColor(Color.web(AppTheme.PRIMARY));
        editBtn.setGraphic(editIcon);
        editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;");
        editBtn.setOnAction(e -> openAdresseDialog(adresse));

        Button deleteBtn = new Button();
        FontIcon deleteIcon = new FontIcon(Feather.TRASH_2);
        deleteIcon.setIconSize(15);
        deleteIcon.setIconColor(Color.web("#C0392B"));
        deleteBtn.setGraphic(deleteIcon);
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;");
        deleteBtn.setOnAction(e -> handleSupprimerAdresse(adresse));

        row.getChildren().addAll(adresseIcon, texteBox, starBtn, editBtn, deleteBtn);
        return row;
    }

    private void openAdresseDialog(Map<String, Object> adresseExistante) {
        int idUtilisateur = ((Number) userData.get("id")).intValue();
        AdresseDialogView dialogView = new AdresseDialogView(
                client, adresseExistante, idUtilisateur,
                savedAdresse -> Platform.runLater(this::chargerAdresses)
        );
        Stage stage = new Stage();
        stage.setScene(new Scene(dialogView));
        stage.setTitle(adresseExistante == null ? "Nouvelle adresse" : "Modifier l'adresse");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();
    }

    private void handleSupprimerAdresse(Map<String, Object> adresse) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer l'adresse");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer cette adresse définitivement ?");
        Button okBtn = (Button) confirm.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Supprimer");
        okBtn.setStyle("-fx-background-color: #C0392B; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-padding: 8 20;");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                Object idObj = adresse.get("id");
                if (idObj == null) return;
                int id = ((Number) idObj).intValue();
                new Thread(() -> {
                    try {
                        AppRequest req = new AppRequest.Builder()
                                .controller("Adresse").action("supprimer")
                                .parameter("id", id)
                                .authToken(client.getAuthToken())
                                .build();
                        AppResponse resp = client.sendAndParse(req);
                        Platform.runLater(() -> {
                            if (resp != null && resp.isSuccess()) chargerAdresses();
                        });
                    } catch (Exception ex) { ex.printStackTrace(); }
                }).start();
            }
        });
    }

    private void handleSetPrincipale(Map<String, Object> adresse) {
        Object idObj = adresse.get("id");
        if (idObj == null) return;
        int idAdresse     = ((Number) idObj).intValue();
        int idUtilisateur = ((Number) userData.get("id")).intValue();
        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("Adresse").action("setPrincipale")
                        .parameter("idUtilisateur", idUtilisateur)
                        .parameter("idAdresse",     idAdresse)
                        .authToken(client.getAuthToken())
                        .build();
                AppResponse resp = client.sendAndParse(req);
                Platform.runLater(() -> {
                    if (resp != null && resp.isSuccess()) chargerAdresses();
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    private void handleLogout() {
        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("Auth").action("logout")
                        .authToken(client.getAuthToken())
                        .build();
                client.sendAndParse(req);
            } catch (Exception ignored) {}
            Platform.runLater(() -> viewManager.showLoginView());
        }).start();
    }

    // =========================================================================
    // 2FA — logique
    // =========================================================================
    private void initEnable2FA(Label feedbackLabel, Button btnActiver, Button btnDesactiver) {
        new Thread(() -> {
            try {
                AppRequest request = new AppRequest.Builder()
                        .controller("Auth").action("twofactorenable")
                        .authToken(client.getAuthToken())
                        .build();
                AppResponse response = client.sendAndParse(request);
                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = response.getDataAs(Map.class);
                        String qrCodeBase64 = (String) data.get("qrCode");
                        showQRCodeDialog(qrCodeBase64, feedbackLabel, btnActiver, btnDesactiver);
                    } else {
                        showFeedback(feedbackLabel,
                                "Erreur activation 2FA : " +
                                        (response != null ? response.getMessage() : "Erreur réseau"), false);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() ->
                        showFeedback(feedbackLabel, "Erreur réseau : " + ex.getMessage(), false));
            }
        }).start();
    }

    private void showQRCodeDialog(String qrCodeBase64, Label feedbackLabel,
                                  Button btnActiver, Button btnDesactiver) {

        // Décoder le Base64 → image JavaFX
        byte[] imageBytes = java.util.Base64.getDecoder().decode(qrCodeBase64);
        javafx.scene.image.Image qrImage = new javafx.scene.image.Image(
                new java.io.ByteArrayInputStream(imageBytes)
        );
        javafx.scene.image.ImageView qrView = new javafx.scene.image.ImageView(qrImage);
        qrView.setFitWidth(200);
        qrView.setFitHeight(200);
        qrView.setPreserveRatio(true);

        Label hint = new Label(
                "1. Ouvre Google Authenticator\n" +
                        "2. Appuie sur '+' → 'Scanner un QR code'\n" +
                        "3. Scanne l'image ci-dessus\n" +
                        "4. Saisis le code généré pour confirmer"
        );
        hint.setWrapText(true);
        hint.setStyle("-fx-font-size: 12px;");

        TextField codeField = new TextField();
        codeField.setPromptText("Code à 6 chiffres");
        codeField.setStyle("-fx-font-size: 18px; -fx-alignment: center;");

        Label dialogFeedback = new Label();
        dialogFeedback.setVisible(false);
        dialogFeedback.setWrapText(true);

        Button confirmBtn = new Button("Confirmer l'activation");
        confirmBtn.setStyle(
                "-fx-background-color: " + AppTheme.PRIMARY + ";" +
                        "-fx-text-fill: white; -fx-font-size: 14px;" +
                        "-fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;"
        );
        confirmBtn.setMaxWidth(Double.MAX_VALUE);

        Stage dialog = new Stage();
        confirmBtn.setOnAction(e ->
                confirmEnable2FA(codeField.getText(), feedbackLabel,
                        dialogFeedback, dialog, btnActiver, btnDesactiver)
        );

        // Centrer le QR
        HBox qrBox = new HBox(qrView);
        qrBox.setAlignment(javafx.geometry.Pos.CENTER);

        VBox content = new VBox(14,
                new Label("Scanne ce QR code avec ton application :"),
                qrBox,
                hint,
                new Label("Code de confirmation :"),
                codeField,
                dialogFeedback,
                confirmBtn
        );
        content.setPadding(new Insets(24));

        dialog.setTitle("Activer le 2FA");
        dialog.setScene(new javafx.scene.Scene(content, 400, 480));
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.show();
    }
    private void confirmEnable2FA(String code, Label feedbackLabel,
                                  Label dialogFeedback, Stage dialog,Button btnActiver, Button btnDesactiver) {
        if (code == null || code.trim().length() != 6) {
            dialogFeedback.setText("Le code doit contenir 6 chiffres.");
            dialogFeedback.setStyle("-fx-text-fill: " + AppTheme.ERROR_COLOR + ";");
            dialogFeedback.setVisible(true);
            return;
        }
        new Thread(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("code", code.trim());

                AppRequest request = new AppRequest.Builder()
                        .controller("Auth").action("twofactorenableconfirm")
                        .authToken(client.getAuthToken())   // ✅ client.getAuthToken()
                        .payload(JsonUtils.toJson(payload))
                        .build();

                AppResponse response = client.sendAndParse(request); // ✅ client

                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        dialog.close();
                        showFeedback(feedbackLabel, "✅ 2FA activé avec succès !", true);
                        btnActiver.setDisable(true);     // ← griser
                        btnDesactiver.setDisable(false); // ← réactiver
                    } else {
                        dialogFeedback.setText("❌ Code invalide. Réessayez.");
                        dialogFeedback.setStyle("-fx-text-fill: " + AppTheme.ERROR_COLOR + ";");
                        dialogFeedback.setVisible(true);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    dialogFeedback.setText("Erreur réseau : " + ex.getMessage());
                    dialogFeedback.setVisible(true);
                });
            }
        }).start();
    }

    private void handleDisable2FA(Label feedbackLabel, Button btnActiver, Button btnDesactiver) {
        TextInputDialog inputDialog = new TextInputDialog();
        inputDialog.setTitle("Désactiver le 2FA");
        inputDialog.setHeaderText("Saisis ton code Google Authenticator pour confirmer");
        inputDialog.setContentText("Code à 6 chiffres :");
        inputDialog.showAndWait().ifPresent(code -> {
            if (code.trim().length() != 6) {
                showFeedback(feedbackLabel, "Code invalide (6 chiffres requis).", false);
                return;
            }
            new Thread(() -> {
                try {
                    Map<String, String> payload = new HashMap<>();
                    payload.put("code", code.trim());
                    AppRequest req = new AppRequest.Builder()
                            .controller("Auth").action("twofactordisable")
                            .authToken(client.getAuthToken())
                            .payload(JsonUtils.toJson(payload))
                            .build();
                    AppResponse resp = client.sendAndParse(req);
                    Platform.runLater(() -> {
                        if (resp != null && resp.isSuccess()) {
                            showFeedback(feedbackLabel, "✅ 2FA désactivé avec succès.", true);
                            btnActiver.setDisable(false);    // ← réactiver
                            btnDesactiver.setDisable(true);  // ← griser
                        } else {
                            showFeedback(feedbackLabel,
                                    "❌ " + (resp != null ? resp.getMessage() : "Erreur réseau."), false);
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() ->
                            showFeedback(feedbackLabel, "Erreur réseau : " + ex.getMessage(), false));
                }
            }).start();
        });
    }

    // =========================================================================
    // Helpers UI
    // =========================================================================
    private VBox buildCard() {
        VBox card = new VBox(14);
        card.setStyle(
                "-fx-background-color: " + AppTheme.CARD_BG + ";" +
                        "-fx-background-radius: 16; -fx-padding: 24;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 14, 0, 0, 3);"
        );
        return card;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";");
        return l;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");
        return l;
    }

    private TextField buildTextField(String value, String placeholder) {
        TextField f = new TextField(value);
        f.setPromptText(placeholder);
        f.setMaxWidth(Double.MAX_VALUE);
        f.setStyle(
                "-fx-background-color: " + AppTheme.FIELD_BG + ";" +
                        "-fx-border-color: " + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius: 10; -fx-background-radius: 10;" +
                        "-fx-padding: 11 14; -fx-font-size: 14px;" +
                        "-fx-text-fill: " + AppTheme.TEXT_MAIN + ";"
        );
        return f;
    }

    private PasswordField buildPasswordField(String placeholder) {
        PasswordField f = new PasswordField();
        f.setPromptText(placeholder);
        f.setMaxWidth(Double.MAX_VALUE);
        f.setStyle(
                "-fx-background-color: " + AppTheme.FIELD_BG + ";" +
                        "-fx-border-color: " + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius: 10; -fx-background-radius: 10;" +
                        "-fx-padding: 11 14; -fx-font-size: 14px;" +
                        "-fx-text-fill: " + AppTheme.TEXT_MAIN + ";"
        );
        return f;
    }

    private VBox buildFieldBox(String label, Control field) {
        Label lbl = fieldLabel(label);
        VBox box = new VBox(6, lbl, field);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private void showFeedback(Label label, String message, boolean success) {
        label.setText(message);
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: " +
                (success ? "#16a34a" : AppTheme.ERROR_COLOR) + ";");
        label.setVisible(true);
    }

    private boolean estPrincipale(Map<String, Object> adresse) {
        Object val = adresse.get("est_principale");
        if (val == null)            return false;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Number)  return ((Number) val).intValue() == 1;
        return "true".equalsIgnoreCase(val.toString()) || "1".equals(val.toString());
    }

    private String str(Object o) { return o == null ? "" : String.valueOf(o); }
}
