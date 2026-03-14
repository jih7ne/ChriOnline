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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CheckoutView extends StackPane {

    private final TCPClient tcpClient;
    private final Map<String, Object> userData;
    private final Consumer<Map<String, Object>> onPaiementSuccess;
    private final Runnable onAnnuler;

    private final ComboBox<Map<String, Object>> adresseComboBox;
    private final TextField numeroCarteField;
    private final TextField moisField;
    private final TextField anneeField;
    private final PasswordField cvvField;
    private final TextField titulaireField;
    private final Label cardPreviewLabel;
    private final Label datePrevLabel;
    private final Label titulairePrevLabel;
    private final Label totalLabel;
    private final Label errorLabel;
    private final Button btnConfirmer;
    private final TableView<Map<String, Object>> articlesTable;

    private final List<Map<String, Object>> lignes;

    // ─── Constructeur — plus de idUtilisateur séparé ──────────────────────
    public CheckoutView(TCPClient tcpClient,
                        List<Map<String, Object>> lignes,
                        Map<String, Object> userData,
                        Consumer<Map<String, Object>> onPaiementSuccess,
                        Runnable onAnnuler) {
        this.tcpClient          = tcpClient;
        this.lignes             = lignes;
        this.userData           = userData;
        this.onPaiementSuccess  = onPaiementSuccess;
        this.onAnnuler          = onAnnuler;

        this.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + AppTheme.BG + "; -fx-background-color: " + AppTheme.BG + ";");

        VBox root = new VBox(24);
        root.setPadding(new Insets(32));
        root.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        // ─── TITRE ─────────────────────────────────────────────────────────
        Label titre = new Label("Finaliser la commande");
        titre.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";");

        // ─── BARRE DE PROGRESSION ──────────────────────────────────────────
        HBox progressBar = buildProgressBar(1);

        // ─── CONTENU PRINCIPAL ─────────────────────────────────────────────
        HBox content = new HBox(24);
        content.setAlignment(Pos.TOP_CENTER);

        VBox leftCol = new VBox(16);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        // --- Section adresse ---
        adresseComboBox = new ComboBox<>();
        adresseComboBox.setMaxWidth(Double.MAX_VALUE);
        adresseComboBox.setPromptText("Sélectionner une adresse");
        adresseComboBox.setStyle(
                "-fx-background-color: " + AppTheme.FIELD_BG + ";" +
                        "-fx-border-color: " + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius: 8px; -fx-background-radius: 8px;" +
                        "-fx-font-size: 14px;"
        );
        adresseComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : item.get("rue") + ", " + item.get("ville"));
            }
        });
        adresseComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Sélectionner une adresse"
                        : item.get("rue") + ", " + item.get("ville"));
            }
        });

        Button btnNouvelleAdresse = new Button("+ Nouvelle adresse");
        AppTheme.styleOutlineButton(btnNouvelleAdresse);
        btnNouvelleAdresse.setOnAction(e -> openAdresseDialog());

        HBox adresseRow = new HBox(12, adresseComboBox, btnNouvelleAdresse);
        HBox.setHgrow(adresseComboBox, Priority.ALWAYS);
        adresseRow.setAlignment(Pos.CENTER_LEFT);

        VBox sectionAdresse = buildSection("Adresse de livraison", adresseRow);

        // --- Aperçu carte ---
        cardPreviewLabel = new Label("•••• •••• •••• ••••");
        cardPreviewLabel.setStyle(
                "-fx-text-fill: " + AppTheme.WHITE + ";" +
                        "-fx-font-size: 22px; -fx-font-family: 'Courier New';"
        );
        titulairePrevLabel = new Label("VOTRE NOM");
        titulairePrevLabel.setStyle("-fx-text-fill: " + AppTheme.WHITE + "; -fx-font-size: 14px;");
        datePrevLabel = new Label("MM/AA");
        datePrevLabel.setStyle("-fx-text-fill: " + AppTheme.WHITE + "; -fx-font-size: 14px;");

        Label chipIcon = new Label("💳");
        chipIcon.setStyle("-fx-font-size: 32px;");
        Label bankLabel = new Label("ChriBank");
        bankLabel.setStyle("-fx-text-fill: " + AppTheme.WHITE + "; -fx-font-size: 13px;");
        Region spacerCard = new Region();
        HBox.setHgrow(spacerCard, Priority.ALWAYS);
        HBox cardTop = new HBox(chipIcon, spacerCard, bankLabel);

        VBox titulaireBox = new VBox(2,
                new Label("Titulaire") {{ setStyle("-fx-text-fill: rgba(237,224,212,0.6); -fx-font-size: 11px;"); }},
                titulairePrevLabel
        );
        VBox dateBoxCard = new VBox(2,
                new Label("Expire") {{ setStyle("-fx-text-fill: rgba(237,224,212,0.6); -fx-font-size: 11px;"); }},
                datePrevLabel
        );
        Region spacerBottom = new Region();
        HBox.setHgrow(spacerBottom, Priority.ALWAYS);
        HBox cardBottom = new HBox(titulaireBox, spacerBottom, dateBoxCard);

        VBox cardPreview = new VBox(16, cardTop, cardPreviewLabel, cardBottom);
        cardPreview.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " + AppTheme.PRIMARY + ", #4A3525);" +
                        "-fx-background-radius: 16px; -fx-padding: 24px;"
        );

        // --- Champs paiement ---
        numeroCarteField = new TextField();
        numeroCarteField.setPromptText("1234 5678 9012 3456");
        numeroCarteField.setStyle(buildFieldStyle() + "-fx-font-family: 'Courier New';");
        numeroCarteField.textProperty().addListener((obs, old, val) -> updateCardPreview());

        moisField = new TextField();
        moisField.setPromptText("MM");
        moisField.setMaxWidth(70);
        moisField.setStyle(buildFieldStyle());
        moisField.textProperty().addListener((obs, old, val) -> updateDatePreview());

        anneeField = new TextField();
        anneeField.setPromptText("AA");
        anneeField.setMaxWidth(70);
        anneeField.setStyle(buildFieldStyle());
        anneeField.textProperty().addListener((obs, old, val) -> updateDatePreview());

        cvvField = new PasswordField();
        cvvField.setPromptText("•••");
        cvvField.setMaxWidth(90);
        cvvField.setStyle(buildFieldStyle());

        titulaireField = new TextField();
        titulaireField.setPromptText("NOM PRÉNOM");
        titulaireField.setStyle(buildFieldStyle());
        titulaireField.textProperty().addListener((obs, old, val) ->
                titulairePrevLabel.setText(val.isEmpty() ? "VOTRE NOM" : val.toUpperCase()));

        Label slashLabel = new Label("/");
        slashLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: " + AppTheme.PRIMARY + ";");
        HBox dateRow = new HBox(8, moisField, slashLabel, anneeField);
        dateRow.setAlignment(Pos.CENTER_LEFT);

        VBox dateBox2   = new VBox(6, fieldLabel("Date d'expiration"), dateRow);
        VBox cvvBox     = new VBox(6, fieldLabel("CVV"), cvvField);
        HBox dateCvvRow = new HBox(16, dateBox2, cvvBox);
        HBox.setHgrow(dateBox2, Priority.ALWAYS);

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: " + AppTheme.ERROR_COLOR + "; -fx-font-size: 13px;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        Label sslLabel = new Label("🔒 Paiement sécurisé par SSL");
        sslLabel.setStyle("-fx-text-fill: " + AppTheme.TEXT_MUTED + "; -fx-font-size: 12px;");

        VBox sectionPaiement = buildSection("Informations de paiement",
                cardPreview,
                new VBox(6, fieldLabel("Numéro de carte"), numeroCarteField),
                dateCvvRow,
                new VBox(6, fieldLabel("Nom du titulaire"), titulaireField),
                errorLabel,
                sslLabel
        );

        // --- Boutons ---
        Button btnAnnulerBtn = new Button("Annuler");
        AppTheme.styleOutlineButton(btnAnnulerBtn);
        btnAnnulerBtn.setMaxWidth(Double.MAX_VALUE);
        btnAnnulerBtn.setOnAction(e -> onAnnuler.run());

        btnConfirmer = new Button("🔒  Payer maintenant");
        AppTheme.stylePrimaryButton(btnConfirmer);
        btnConfirmer.setOnAction(e -> handlePaiement());

        HBox boutons = new HBox(16, btnAnnulerBtn, btnConfirmer);
        HBox.setHgrow(btnAnnulerBtn, Priority.ALWAYS);
        HBox.setHgrow(btnConfirmer, Priority.ALWAYS);

        leftCol.getChildren().addAll(sectionAdresse, sectionPaiement, boutons);

        // --- Colonne droite : résumé ---
        articlesTable = new TableView<>();
        articlesTable.setPrefHeight(200);
        articlesTable.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        TableColumn<Map<String, Object>, String> colProduit = new TableColumn<>("Produit");
        colProduit.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colProduit.setPrefWidth(140);
        TableColumn<Map<String, Object>, Integer> colQte = new TableColumn<>("Qté");
        colQte.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colQte.setPrefWidth(40);
        TableColumn<Map<String, Object>, Double> colPrix = new TableColumn<>("Prix");
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        colPrix.setPrefWidth(70);
        articlesTable.getColumns().addAll(colProduit, colQte, colPrix);

        totalLabel = new Label("0.00 MAD");
        totalLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";");

        Label totalTxt = new Label("Total");
        totalTxt.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";");
        Region spacerTotal = new Region();
        HBox.setHgrow(spacerTotal, Priority.ALWAYS);
        HBox totalRow = new HBox(totalTxt, spacerTotal, totalLabel);

        Label livraisonLabel = new Label("Gratuit");
        livraisonLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        Label livraisonTxt = new Label("Livraison");
        Region spacerLiv = new Region();
        HBox.setHgrow(spacerLiv, Priority.ALWAYS);
        HBox livraisonRow = new HBox(livraisonTxt, spacerLiv, livraisonLabel);

        Separator sep1 = new Separator();
        Separator sep2 = new Separator();

        VBox rightCol = buildSection("Récapitulatif",
                articlesTable, sep1, livraisonRow, sep2, totalRow);
        rightCol.setMinWidth(280);
        rightCol.setMaxWidth(320);

        content.getChildren().addAll(leftCol, rightCol);
        root.getChildren().addAll(titre, progressBar, content);
        scroll.setContent(root);
        this.getChildren().add(scroll);

        // ─── Charger les données au démarrage ─────────────────────────────
        // idUtilisateur extrait UNE SEULE FOIS ici (corrige le double-define)
        int idUtilisateur = ((Double) userData.get("id")).intValue();
        chargerAdresses(idUtilisateur);
        chargerResume();
    }

    // ─── Réseau ────────────────────────────────────────────────────────────

    private void chargerAdresses(int idUtilisateur) {
        new Thread(() -> {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("idUtilisateur", idUtilisateur);
                AppRequest req = new AppRequest.Builder()
                        .controller("Adresse").action("lister")
                        .payload(JsonUtils.toJson(params)).build();
                AppResponse resp = tcpClient.sendAndParse(req);
                if (resp != null && resp.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> adresses = resp.getDataAs(List.class);
                    Platform.runLater(() -> {
                        if (adresses != null) adresseComboBox.getItems().addAll(adresses);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handlePaiement() {
        if (adresseComboBox.getValue() == null) {
            showError("Veuillez sélectionner une adresse de livraison.");
            return;
        }
        String numeroCarte = numeroCarteField.getText().replace(" ", "");
        String cvv         = cvvField.getText();
        String mois        = moisField.getText();
        String annee       = anneeField.getText();

        if (numeroCarte.isEmpty() || cvv.isEmpty() || mois.isEmpty() || annee.isEmpty()) {
            showError("Veuillez remplir tous les champs de paiement.");
            return;
        }

        btnConfirmer.setDisable(true);
        btnConfirmer.setText("Traitement en cours...");
        hideError();

        new Thread(() -> {
            try {
                // idUtilisateur extrait depuis userData (pas de redéclaration)
                int idUser    = ((Double) userData.get("id")).intValue();
                int idAdresse = ((Double) adresseComboBox.getValue().get("id")).intValue();

                // ── ÉTAPE 1 : Créer la commande ────────────────────────────
                Map<String, Object> commandeParams = new HashMap<>();
                commandeParams.put("idUtilisateur", idUser);
                commandeParams.put("idAdresse",     idAdresse);
                commandeParams.put("lignes",        lignes);

                AppRequest reqCommande = new AppRequest.Builder()
                        .controller("Commande").action("valider")
                        .payload(JsonUtils.toJson(commandeParams)).build();
                AppResponse respCommande = tcpClient.sendAndParse(reqCommande);

                if (respCommande == null || !respCommande.isSuccess()) {
                    Platform.runLater(() -> {
                        btnConfirmer.setDisable(false);
                        btnConfirmer.setText("🔒  Payer maintenant");
                        showError(respCommande != null
                                ? respCommande.getMessage()
                                : "Erreur lors de la création de la commande.");
                    });
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> commandeData  = respCommande.getDataAs(Map.class);
                int    idCommandeCreee            = ((Double) commandeData.get("idCommande")).intValue();
                String uuidCommande               = (String) commandeData.get("uuidCommande");

                // ── ÉTAPE 2 : Traiter le paiement ─────────────────────────
                Map<String, Object> paiementParams = new HashMap<>();
                paiementParams.put("idCommande",     idCommandeCreee);
                paiementParams.put("numeroCarte",    numeroCarte);
                paiementParams.put("cvv",            cvv);
                paiementParams.put("dateExpiration", mois + "/" + annee);
                paiementParams.put("methodePaiement","FICTIF");

                AppRequest reqPaiement = new AppRequest.Builder()
                        .controller("Paiement").action("traiter")
                        .payload(JsonUtils.toJson(paiementParams)).build();
                AppResponse respPaiement = tcpClient.sendAndParse(reqPaiement);

                Platform.runLater(() -> {
                    btnConfirmer.setDisable(false);
                    btnConfirmer.setText("🔒  Payer maintenant");

                    if (respPaiement != null && respPaiement.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = respPaiement.getDataAs(Map.class);
                        if (data != null) {
                            data.put("uuidCommande", uuidCommande);
                            data.put("userData",     userData);
                            onPaiementSuccess.accept(data);
                        }
                    } else {
                        showError(respPaiement != null
                                ? respPaiement.getMessage()
                                : "Paiement échoué.");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnConfirmer.setDisable(false);
                    btnConfirmer.setText("🔒  Payer maintenant");
                    showError("Erreur réseau : " + e.getMessage());
                });
            }
        }).start();
    }

    // ─── Helpers UI ────────────────────────────────────────────────────────

    private void chargerResume() {
        if (lignes == null) return;
        double total = 0;
        for (Map<String, Object> ligne : lignes) {
            double prix = ((Number) ligne.getOrDefault("prix_unitaire", 0)).doubleValue();
            int    qte  = ((Number) ligne.getOrDefault("quantite", 1)).intValue();
            total      += prix * qte;
        }
        double finalTotal = total;
        Platform.runLater(() -> totalLabel.setText(String.format("%.2f MAD", finalTotal)));
    }

    private void updateCardPreview() {
        String raw = numeroCarteField.getText().replace(" ", "");
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < raw.length() && i < 16; i++) {
            if (i > 0 && i % 4 == 0) formatted.append(" ");
            formatted.append(raw.charAt(i));
        }
        while (formatted.toString().replace(" ", "").length() < 16) {
            if (formatted.length() > 0 && formatted.length() % 5 == 4) formatted.append(" ");
            formatted.append("•");
        }
        cardPreviewLabel.setText(formatted.toString());
    }

    private void updateDatePreview() {
        String m = moisField.getText().isEmpty() ? "MM" : moisField.getText();
        String a = anneeField.getText().isEmpty() ? "AA" : anneeField.getText();
        datePrevLabel.setText(m + "/" + a);
    }

    private void openAdresseDialog() {
        AdresseDialogView dialog = new AdresseDialogView(tcpClient, null, adresse ->
                Platform.runLater(() -> adresseComboBox.getItems().add(adresse))
        );
        javafx.scene.Scene scene = new javafx.scene.Scene(dialog);
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setScene(scene);
        stage.setTitle("Nouvelle adresse");
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.showAndWait();
    }

    static HBox buildProgressBar(int activeIndex) {
        String[] labels = {"Panier", "Paiement", "Confirmation"};
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER);
        for (int i = 0; i < labels.length; i++) {
            boolean completed = i < activeIndex;
            boolean active    = i == activeIndex;
            String bg        = completed ? AppTheme.PRIMARY
                    : active    ? AppTheme.PRIMARY_LIGHT
                    :             AppTheme.TOGGLE_INACTIVE;
            String textColor = (completed || active) ? AppTheme.WHITE : AppTheme.PRIMARY;
            String stepText  = completed ? "✓" : String.valueOf(i + 1);

            Label circle = new Label(stepText);
            circle.setStyle(
                    "-fx-background-color: " + bg + ";" +
                            "-fx-background-radius: 20px;" +
                            "-fx-min-width: 40px; -fx-min-height: 40px;" +
                            "-fx-alignment: CENTER;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: " + textColor + ";"
            );
            Label lbl = new Label(labels[i]);
            lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: "
                    + ((completed || active) ? AppTheme.PRIMARY : AppTheme.TEXT_MUTED) + ";");
            VBox step = new VBox(4, circle, lbl);
            step.setAlignment(Pos.CENTER);
            bar.getChildren().add(step);

            if (i < labels.length - 1) {
                Region line = new Region();
                line.setPrefWidth(80);
                line.setPrefHeight(4);
                line.setStyle("-fx-background-color: "
                        + (i < activeIndex ? AppTheme.PRIMARY : AppTheme.TOGGLE_INACTIVE) + ";");
                line.setTranslateY(-10);
                bar.getChildren().add(line);
            }
        }
        return bar;
    }

    private VBox buildSection(String titre, javafx.scene.Node... children) {
        Label titreLabel = new Label(titre);
        titreLabel.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";"
        );
        VBox section = new VBox(16);
        section.setStyle(
                "-fx-background-color: " + AppTheme.CARD_BG + ";" +
                        "-fx-background-radius: 16px; -fx-padding: 24px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 12, 0, 0, 4);"
        );
        section.getChildren().add(titreLabel);
        section.getChildren().addAll(children);
        return section;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + AppTheme.TEXT_MAIN + "; -fx-font-size: 14px;");
        return l;
    }

    private String buildFieldStyle() {
        return "-fx-background-color: " + AppTheme.FIELD_BG + ";" +
                "-fx-border-color: " + AppTheme.FIELD_BORDER + ";" +
                "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                "-fx-padding: 12px 16px; -fx-font-size: 14px;" +
                "-fx-text-fill: " + AppTheme.TEXT_MAIN + ";";
    }

    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
    private void hideError()           { errorLabel.setVisible(false); }
}