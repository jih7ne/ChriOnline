package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.core.theme.AppTheme;
import com.chrionline.chrionline.core.utils.JsonUtils;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.network.tcp.TCPClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AdresseDialogView extends VBox {

    private final TextField rueField;
    private final TextField complementField;
    private final TextField codePostalField;
    private final TextField villeField;
    private final ComboBox<String> paysComboBox;
    private final CheckBox estPrincipaleCheckBox;
    private final Label errorLabel;

    private final TCPClient tcpClient;
    private final Map<String, Object> adresseExistante; // null = création, sinon modification
    private final int idUtilisateur;
    private final Consumer<Map<String, Object>> onSave;

    public AdresseDialogView(TCPClient tcpClient,
                             Map<String, Object> adresseExistante,
                             int idUtilisateur,
                             Consumer<Map<String, Object>> onSave) {
        this.tcpClient         = tcpClient;
        this.adresseExistante  = adresseExistante;
        this.idUtilisateur     = idUtilisateur;
        this.onSave            = onSave;

        this.setSpacing(16);
        this.setPadding(new Insets(32));
        this.setStyle("-fx-background-color: " + AppTheme.BG + "; -fx-background-radius: 16px;");
        this.setMinWidth(420);
        this.setMaxWidth(480);

        // ─── TITRE ─────────────────────────────────────────────────────────
        boolean isEdit = adresseExistante != null;
        Label titre = new Label(isEdit ? "Modifier l'adresse" : "Nouvelle adresse");
        titre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";");

        Separator sep1 = new Separator();

        // ─── CHAMPS ────────────────────────────────────────────────────────
        rueField = buildField("Numéro et nom de rue");
        complementField = buildField("Appartement, étage, bâtiment...");

        codePostalField = buildField("10000");
        codePostalField.setMaxWidth(120);

        villeField = buildField("Tétouan");

        HBox cpVilleRow = new HBox(12,
                new VBox(6, fieldLabel("Code postal *"), codePostalField),
                new VBox(6, fieldLabel("Ville *"), villeField)
        );
        HBox.setHgrow(villeField, Priority.ALWAYS);
        ((VBox) cpVilleRow.getChildren().get(1)).setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(cpVilleRow, Priority.NEVER);

        paysComboBox = new ComboBox<>();
        paysComboBox.getItems().addAll(
                "Maroc", "France", "Espagne", "Belgique",
                "Suisse", "Canada", "Allemagne", "Italie"
        );
        paysComboBox.setValue("Maroc");
        paysComboBox.setMaxWidth(Double.MAX_VALUE);
        paysComboBox.setStyle(
                "-fx-background-color: " + AppTheme.FIELD_BG + ";" +
                        "-fx-border-color: " + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                        "-fx-font-size: 14px;"
        );

        estPrincipaleCheckBox = new CheckBox("Définir comme adresse principale");
        estPrincipaleCheckBox.setStyle("-fx-text-fill: " + AppTheme.TEXT_MAIN + "; -fx-font-size: 14px;");

        // Pré-remplir si modification
        if (isEdit) {
            rueField.setText(String.valueOf(adresseExistante.getOrDefault("rue", "")));
            complementField.setText(String.valueOf(adresseExistante.getOrDefault("complement", "")));
            codePostalField.setText(String.valueOf(adresseExistante.getOrDefault("code_postal", "")));
            villeField.setText(String.valueOf(adresseExistante.getOrDefault("ville", "")));
            paysComboBox.setValue(String.valueOf(adresseExistante.getOrDefault("pays", "Maroc")));
            estPrincipaleCheckBox.setSelected(
                    Boolean.parseBoolean(String.valueOf(adresseExistante.getOrDefault("est_principale", false)))
            );
        }

        // ─── ERREUR ────────────────────────────────────────────────────────
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: " + AppTheme.ERROR_COLOR + "; -fx-font-size: 13px;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        Separator sep2 = new Separator();

        // ─── BOUTONS ───────────────────────────────────────────────────────
        Button btnAnnuler = new Button("Annuler");
        AppTheme.styleOutlineButton(btnAnnuler);
        btnAnnuler.setOnAction(e -> closeDialog());

        Button btnEnregistrer = new Button("Enregistrer");
        AppTheme.stylePrimaryButton(btnEnregistrer);
        btnEnregistrer.setOnAction(e -> handleEnregistrer());

        HBox boutons = new HBox(12, btnAnnuler, btnEnregistrer);
        HBox.setHgrow(btnAnnuler, Priority.ALWAYS);
        HBox.setHgrow(btnEnregistrer, Priority.ALWAYS);
        btnAnnuler.setMaxWidth(Double.MAX_VALUE);

        // ─── ASSEMBLAGE ────────────────────────────────────────────────────
        this.getChildren().addAll(
                titre, sep1,
                new VBox(6, fieldLabel("Rue *"), rueField),
                new VBox(6, fieldLabel("Complément (optionnel)"), complementField),
                cpVilleRow,
                new VBox(6, fieldLabel("Pays *"), paysComboBox),
                estPrincipaleCheckBox,
                errorLabel,
                sep2,
                boutons
        );
    }

    private void handleEnregistrer() {
        String rue        = rueField.getText().trim();
        String ville      = villeField.getText().trim();
        String codePostal = codePostalField.getText().trim();
        String pays       = paysComboBox.getValue();

        if (rue.isEmpty() || ville.isEmpty() || codePostal.isEmpty()) {
            showError("Veuillez remplir les champs obligatoires (*).");
            return;
        }

        new Thread(() -> {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("rue",            rue);
                params.put("complement",     complementField.getText().trim());
                params.put("ville",          ville);
                params.put("code_postal",    codePostal);
                params.put("pays",           pays);
                params.put("est_principale", estPrincipaleCheckBox.isSelected());

                String action = adresseExistante != null ? "modifier" : "ajouter";
                if (adresseExistante != null) {
                    params.put("id", adresseExistante.get("id"));
                    params.put("id_utilisateur", adresseExistante.getOrDefault("id_utilisateur", idUtilisateur));
                } else {
                    params.put("id_utilisateur", idUtilisateur);
                }

                AppRequest req = new AppRequest.Builder()
                        .controller("Adresse").action(action)
                        .payload(JsonUtils.toJson(params)).build();
                AppResponse resp = tcpClient.sendAndParse(req);

                Platform.runLater(() -> {
                    if (resp != null && resp.isSuccess()) {
                        // Si la réponse contient l'adresse créée avec son id, l'utiliser
                        @SuppressWarnings("unchecked")
                        Map<String, Object> adresseAvecId = resp.getDataAs(Map.class);
                        if (adresseAvecId != null) {
                            onSave.accept(adresseAvecId);
                        } else {
                            onSave.accept(params); // fallback
                        }
                        closeDialog();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Erreur réseau : " + e.getMessage()));
            }
        }).start();
    }

    private void closeDialog() {
        if (getScene() != null && getScene().getWindow() != null) {
            getScene().getWindow().hide();
        }
    }

    private TextField buildField(String placeholder) {
        TextField f = new TextField();
        f.setPromptText(placeholder);
        f.setMaxWidth(Double.MAX_VALUE);
        f.setStyle(
                "-fx-background-color: " + AppTheme.FIELD_BG + ";" +
                        "-fx-border-color: " + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                        "-fx-padding: 12px 16px; -fx-font-size: 14px;" +
                        "-fx-text-fill: " + AppTheme.TEXT_MAIN + ";"
        );
        return f;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + AppTheme.TEXT_MAIN + "; -fx-font-size: 14px;");
        return l;
    }

    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
}