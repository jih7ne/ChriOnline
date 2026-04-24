package com.chrionline.clientmodule.client.ui.views;

import com.chrionline.core.theme.AppTheme;
import com.chrionline.core.utils.JsonUtils;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.core.network.protocol.AppResponse;
import com.chrionline.network.tcp.TCPClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashMap;
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
    private final Map<String, Object> adresseExistante;
    private final int idUtilisateur;
    private final Consumer<Map<String, Object>> onSave;

    public AdresseDialogView(TCPClient tcpClient,
                             Map<String, Object> adresseExistante,
                             int idUtilisateur,
                             Consumer<Map<String, Object>> onSave) {
        this.tcpClient        = tcpClient;
        this.adresseExistante = adresseExistante;
        this.idUtilisateur    = idUtilisateur;
        this.onSave           = onSave;

        this.setSpacing(16);
        this.setPadding(new Insets(32));
        this.setStyle("-fx-background-color: " + AppTheme.BG + "; -fx-background-radius: 16px;");
        this.setMinWidth(420);
        this.setMaxWidth(480);

        boolean isEdit = adresseExistante != null;
        Label titre = new Label(isEdit ? "Modifier l'adresse" : "Nouvelle adresse");
        titre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";");

        Separator sep1 = new Separator();

        rueField        = buildField("Numéro et nom de rue");
        complementField = buildField("Appartement, étage, bâtiment...");

        codePostalField = buildField("10000");
        codePostalField.setMaxWidth(120);

        villeField = buildField("Tétouan");

        HBox cpVilleRow = new HBox(12,
                new VBox(6, fieldLabel("Code postal *"), codePostalField),
                new VBox(6, fieldLabel("Ville *"), villeField)
        );
        HBox.setHgrow(cpVilleRow.getChildren().get(1), Priority.ALWAYS);
        ((VBox) cpVilleRow.getChildren().get(1)).setMaxWidth(Double.MAX_VALUE);

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
            rueField.setText(str(adresseExistante.get("rue")));
            complementField.setText(str(adresseExistante.get("complement")));
            codePostalField.setText(str(adresseExistante.get("code_postal")));
            villeField.setText(str(adresseExistante.get("ville")));
            String pays = str(adresseExistante.get("pays"));
            if (!pays.isBlank()) paysComboBox.setValue(pays);
            estPrincipaleCheckBox.setSelected(estPrincipale(adresseExistante));
        }

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: " + AppTheme.ERROR_COLOR + "; -fx-font-size: 13px;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        Separator sep2 = new Separator();

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

        System.out.println("[DEBUG ADRESSE] bouton enregistrer cliqué");
        System.out.println("[DEBUG ADRESSE] rue='" + rue + "' ville='" + ville + "' cp='" + codePostal + "'");
        System.out.println("[DEBUG ADRESSE] isEdit=" + (adresseExistante != null));
        if (adresseExistante != null) {
            System.out.println("[DEBUG ADRESSE] id existant=" + adresseExistante.get("id"));
        }

        if (rue.isEmpty() || ville.isEmpty() || codePostal.isEmpty()) {
            showError("Veuillez remplir les champs obligatoires (*).");
            return;
        }

        new Thread(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("rue",            rue);
                payload.put("complement",     complementField.getText().trim());
                payload.put("ville",          ville);
                payload.put("code_postal",    codePostal);
                payload.put("pays",           pays);
                payload.put("est_principale", estPrincipaleCheckBox.isSelected());
                payload.put("id_utilisateur", idUtilisateur);

                boolean isEdit = adresseExistante != null;
                String  action = isEdit ? "modifier" : "ajouter";

                AppRequest.Builder builder = new AppRequest.Builder()
                        .controller("Adresse")
                        .action(action)
                        .payload(JsonUtils.toJson(payload));

                // FIX : id envoyé en parameter (pas dans le payload)
                if (isEdit) {
                    Object idVal = adresseExistante.get("id");
                    if (idVal == null) {
                        System.err.println("[DEBUG ADRESSE] ERREUR : id est null !");
                        Platform.runLater(() -> showError("Identifiant d'adresse manquant."));
                        return;
                    }
                    int id = ((Number) idVal).intValue();
                    System.out.println("[DEBUG ADRESSE] parameter id=" + id);
                    builder.parameter("id", id);
                }

                AppRequest req = builder.build();
                System.out.println("[DEBUG ADRESSE] requête = " + req.toJson());

                AppResponse resp = tcpClient.sendAndParse(req);
                System.out.println("[DEBUG ADRESSE] réponse = " + (resp != null ? resp.toJson() : "NULL"));

                Platform.runLater(() -> {
                    if (resp != null && resp.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> adresseRetour = resp.getDataAs(Map.class);
                        onSave.accept(adresseRetour != null ? adresseRetour : payload);
                        closeDialog();
                    } else {
                        showError(resp != null ? resp.getMessage() : "Erreur réseau.");
                    }
                });
            } catch (Exception e) {
                System.err.println("[DEBUG ADRESSE] EXCEPTION : " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showError("Erreur réseau : " + e.getMessage()));
            }
        }).start();
    }

    private void closeDialog() {
        if (getScene() != null && getScene().getWindow() != null) {
            getScene().getWindow().hide();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private boolean estPrincipale(Map<String, Object> adresse) {
        Object val = adresse.get("est_principale");
        if (val == null)            return false;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Number)  return ((Number) val).intValue() == 1;
        return "true".equalsIgnoreCase(val.toString()) || "1".equals(val.toString());
    }
}
