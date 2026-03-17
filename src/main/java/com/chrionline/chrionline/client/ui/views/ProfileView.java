package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.client.ui.components.ClientNavbar;
import com.chrionline.chrionline.core.interfaces.ViewManager;
import com.chrionline.chrionline.core.theme.AppTheme;
import com.chrionline.chrionline.core.utils.JsonUtils;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.network.tcp.TCPClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
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
        this.client = client;
        this.userData = userData;
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
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;" +
                        "-fx-underline: true;" +
                        "-fx-padding: 0;"
        ));
        retourBtn.setOnMouseExited(e -> retourBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + AppTheme.PRIMARY + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0;"
        ));
        retourBtn.setOnAction(e -> viewManager.showCatalogueView(userData));

        Label titre = new Label("Mon profil");
        titre.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + AppTheme.PRIMARY + ";"
        );

        VBox cardInfos    = buildCardInfosPersonnelles();
        VBox cardMdp      = buildCardMotDePasse();
        VBox cardAdresses = buildCardAdresses();

        root.getChildren().addAll(retourBtn, titre, cardInfos, cardMdp, cardAdresses);
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

        // Email read-only
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
                        "-fx-border-radius: 10px;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-padding: 11px 14px;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: " + AppTheme.TEXT_MUTED + ";" +
                        "-fx-opacity: 0.7;"
        );
        Label emailLockHint = new Label("L'email ne peut pas être modifié");
        emailLockHint.setStyle("-fx-font-size: 11px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        emailRow.getChildren().addAll(lockIcon, emailReadOnly);

        nomField = buildTextField(userData.getOrDefault("nom", "").toString(), "Nom");
        prenomField = buildTextField(userData.getOrDefault("prenom", "").toString(), "Prénom");

        VBox nomBox    = buildFieldBox("Nom", nomField);
        VBox prenomBox = buildFieldBox("Prénom", prenomField);
        HBox.setHgrow(nomBox, Priority.ALWAYS);
        HBox.setHgrow(prenomBox, Priority.ALWAYS);
        HBox nomPrenomRow = new HBox(14, nomBox, prenomBox);

        profilFeedbackLabel = new Label();
        profilFeedbackLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.ERROR_COLOR + ";");
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

        System.out.println("[DEBUG PROFIL] bouton cliqué nom='" + nom + "' prenom='" + prenom + "'");

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

                System.out.println("[DEBUG PROFIL] requête = " + req.toJson());

                AppResponse resp = client.sendAndParse(req);

                System.out.println("[DEBUG PROFIL] réponse = " + (resp != null ? resp.toJson() : "NULL"));

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
                System.err.println("[DEBUG PROFIL] EXCEPTION : " + ex.getMessage());
                ex.printStackTrace();
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
        mdpFeedbackLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.ERROR_COLOR + ";");
        mdpFeedbackLabel.setVisible(false);
        mdpFeedbackLabel.setWrapText(true);

        Button mdpBtn = new Button("Changer le mot de passe");
        AppTheme.stylePrimaryButton(mdpBtn);
        mdpBtn.setMaxWidth(Double.MAX_VALUE);
        mdpBtn.setOnAction(e -> handleUpdatePassword());

        card.getChildren().addAll(
                sectionTitre,
                buildFieldBox("Mot de passe actuel", ancienMdpField),
                buildFieldBox("Nouveau mot de passe", nouveauMdpField),
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

        System.out.println("[DEBUG MDP] bouton cliqué");
        System.out.println("[DEBUG MDP] ancien vide=" + ancien.isEmpty()
                + " nouveau vide=" + nouveau.isEmpty()
                + " confirmer vide=" + confirmer.isEmpty());

        if (ancien.isEmpty() || nouveau.isEmpty() || confirmer.isEmpty()) {
            showFeedback(mdpFeedbackLabel, "Tous les champs sont requis.", false); return;
        }
        if (nouveau.length() < 6) {
            showFeedback(mdpFeedbackLabel, "Le nouveau mot de passe doit faire au moins 6 caractères.", false); return;
        }
        if (!nouveau.equals(confirmer)) {
            showFeedback(mdpFeedbackLabel, "Les mots de passe ne correspondent pas.", false); return;
        }

        System.out.println("[DEBUG MDP] validations OK, authToken=" + client.getAuthToken());

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

                System.out.println("[DEBUG MDP] requête = " + req.toJson());

                AppResponse resp = client.sendAndParse(req);

                System.out.println("[DEBUG MDP] réponse = " + (resp != null ? resp.toJson() : "NULL"));

                Platform.runLater(() -> {
                    if (resp != null && resp.isSuccess()) {
                        ancienMdpField.clear();
                        nouveauMdpField.clear();
                        confirmerMdpField.clear();
                        showFeedback(mdpFeedbackLabel, "Mot de passe modifié avec succès !", true);
                    } else {
                        showFeedback(mdpFeedbackLabel,
                                resp != null ? resp.getMessage() : "Erreur réseau.", false);
                    }
                });
            } catch (Exception ex) {
                System.err.println("[DEBUG MDP] EXCEPTION : " + ex.getMessage());
                ex.printStackTrace();
                Platform.runLater(() ->
                        showFeedback(mdpFeedbackLabel, "Erreur réseau : " + ex.getMessage(), false));
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
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 20;" +
                        "-fx-text-fill: " + AppTheme.PRIMARY + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 6 16 6 16;" +
                        "-fx-cursor: hand;"
        );
        ajouterBtn.setOnAction(e -> openAdresseDialog(null));
        headerRow.getChildren().addAll(sectionTitre, ajouterBtn);

        adressesContainer = new VBox(10);

        Separator sepLogout = new Separator();
        sepLogout.setStyle("-fx-background-color: " + AppTheme.FIELD_BORDER + ";");

        Button logoutBtn = new Button("Se déconnecter");
        logoutBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #C0392B;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 20;" +
                        "-fx-text-fill: #C0392B;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 24 10 24;" +
                        "-fx-cursor: hand;"
        );
        logoutBtn.setOnAction(e -> handleLogout());

        card.getChildren().addAll(headerRow, adressesContainer, sepLogout, logoutBtn);
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
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;"
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
        ligneRue.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");

        Label ligneVille = new Label(cp + " " + ville + ", " + pays);
        ligneVille.setStyle("-fx-font-size: 12px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");

        texteBox.getChildren().addAll(ligneRue, ligneVille);

        if (principale) {
            Label badge = new Label("Principale");
            badge.setStyle(
                    "-fx-background-color: " + AppTheme.PRIMARY + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 11px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 2 8 2 8;" +
                            "-fx-background-radius: 10;"
            );
            texteBox.getChildren().add(badge);
        }

        // Étoile principale
        Button starBtn = new Button();
        FontIcon starIcon = new FontIcon(Feather.STAR);
        starIcon.setIconSize(15);
        starIcon.setIconColor(principale ? Color.web("#F59E0B") : Color.web(AppTheme.TEXT_MUTED));
        starBtn.setGraphic(starIcon);
        starBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;");
        starBtn.setTooltip(new Tooltip(principale ? "Adresse principale" : "Définir comme principale"));
        if (!principale) {
            starBtn.setOnAction(e -> handleSetPrincipale(adresse));
        }

        // Modifier
        Button editBtn = new Button();
        FontIcon editIcon = new FontIcon(Feather.EDIT_2);
        editIcon.setIconSize(15);
        editIcon.setIconColor(Color.web(AppTheme.PRIMARY));
        editBtn.setGraphic(editIcon);
        editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;");
        editBtn.setOnMouseEntered(e -> editBtn.setStyle(
                "-fx-background-color: #E6CCB2; -fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 4;"));
        editBtn.setOnMouseExited(e -> editBtn.setStyle(
                "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;"));
        editBtn.setOnAction(e -> openAdresseDialog(adresse));

        // Supprimer
        Button deleteBtn = new Button();
        FontIcon deleteIcon = new FontIcon(Feather.TRASH_2);
        deleteIcon.setIconSize(15);
        deleteIcon.setIconColor(Color.web("#C0392B"));
        deleteBtn.setGraphic(deleteIcon);
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;");
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(
                "-fx-background-color: #FEF2F2; -fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 4;"));
        deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(
                "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;"));
        deleteBtn.setOnAction(e -> handleSupprimerAdresse(adresse));

        row.getChildren().addAll(adresseIcon, texteBox, starBtn, editBtn, deleteBtn);
        return row;
    }

    private void openAdresseDialog(Map<String, Object> adresseExistante) {
        int idUtilisateur = ((Number) userData.get("id")).intValue();
        AdresseDialogView dialogView = new AdresseDialogView(
                client,
                adresseExistante,
                idUtilisateur,
                savedAdresse -> Platform.runLater(this::chargerAdresses)
        );
        Scene scene = new Scene(dialogView);
        Stage stage = new Stage();
        stage.setScene(scene);
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
        okBtn.setStyle(
                "-fx-background-color: #C0392B; -fx-text-fill: white;" +
                        "-fx-background-radius: 8; -fx-padding: 8 20 8 20;"
        );

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
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
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
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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
    // Helpers UI
    // =========================================================================
    private VBox buildCard() {
        VBox card = new VBox(14);
        card.setStyle(
                "-fx-background-color: " + AppTheme.CARD_BG + ";" +
                        "-fx-background-radius: 16;" +
                        "-fx-padding: 24;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 14, 0, 0, 3);"
        );
        return card;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-font-size: 17px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + AppTheme.PRIMARY + ";"
        );
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
        label.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: " +
                        (success ? "#16a34a" : AppTheme.ERROR_COLOR) + ";"
        );
        label.setVisible(true);
    }

    private boolean estPrincipale(Map<String, Object> adresse) {
        Object val = adresse.get("est_principale");
        if (val == null)            return false;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Number)  return ((Number) val).intValue() == 1;
        return "true".equalsIgnoreCase(val.toString()) || "1".equals(val.toString());
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}