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
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CheckoutView extends BorderPane {

    private final TCPClient tcpClient;
    private final Map<String, Object> userData;
    private final ViewManager viewManager;
    private final Consumer<Map<String, Object>> onPaiementSuccess;
    private final Runnable onAnnuler;

    private final ComboBox<Map<String, Object>> adresseComboBox;
    private final TextField numeroCarteField;
    private final TextField moisField;
    private final TextField anneeField;
    private final PasswordField cvvField;
    private final Label cardPreviewLabel;
    private final Label datePrevLabel;
    private final Label totalLabel;
    private final Label errorLabel;
    private final Button btnConfirmer;
    private final VBox produitsContainer;

    private final List<Map<String, Object>> lignes;

    public CheckoutView(TCPClient tcpClient,
                        List<Map<String, Object>> lignes,
                        Map<String, Object> userData,
                        ViewManager viewManager,
                        Consumer<Map<String, Object>> onPaiementSuccess,
                        Runnable onAnnuler) {
        this.tcpClient         = tcpClient;
        this.lignes            = lignes;
        this.userData          = userData;
        this.viewManager       = viewManager;
        this.onPaiementSuccess = onPaiementSuccess;
        this.onAnnuler         = onAnnuler;

        this.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        // ─── NAVBAR ────────────────────────────────────────────────────────
        ClientNavbar navbar = new ClientNavbar(0, userData, viewManager, null);
        this.setTop(navbar);

        // ─── CONTENU SCROLLABLE ────────────────────────────────────────────
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + AppTheme.BG + "; -fx-background-color: " + AppTheme.BG + ";");

        VBox root = new VBox(28);
        root.setPadding(new Insets(28, 60, 48, 60));
        root.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        // ─── STEP INDICATOR ────────────────────────────────────────────────
        HBox stepIndicator = buildStepIndicator(1);

        // ─── CONTENU PRINCIPAL ─────────────────────────────────────────────
        HBox content = new HBox(28);
        content.setAlignment(Pos.TOP_LEFT);

        // ══════════════════════════════════════════════
        // COLONNE GAUCHE
        // ══════════════════════════════════════════════
        VBox leftCol = new VBox(20);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        // ── Section adresse ────────────────────────────────────────────────
        adresseComboBox = new ComboBox<>();
        adresseComboBox.setMaxWidth(Double.MAX_VALUE);
        adresseComboBox.setPromptText("Sélectionner une adresse");
        adresseComboBox.setStyle(fieldStyle());
        adresseComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.get("rue") + ", " + item.get("ville"));
            }
        });
        adresseComboBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Sélectionner une adresse"
                        : item.get("rue") + ", " + item.get("ville"));
            }
        });

        Button btnNouvelleAdresse = new Button("+ Nouvelle adresse");
        btnNouvelleAdresse.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: " + AppTheme.PRIMARY + ";" +
                        "-fx-border-width: 1.5px; -fx-border-radius: 20px;" +
                        "-fx-text-fill: " + AppTheme.PRIMARY + ";" +
                        "-fx-font-size: 13px; -fx-padding: 8px 16px; -fx-cursor: hand;"
        );
        btnNouvelleAdresse.setOnAction(e -> openAdresseDialog());

        HBox adresseRow = new HBox(12, adresseComboBox, btnNouvelleAdresse);
        HBox.setHgrow(adresseComboBox, Priority.ALWAYS);
        adresseRow.setAlignment(Pos.CENTER_LEFT);

        VBox sectionAdresse = buildCard("📍  Adresse de livraison", adresseRow);

        // ── Aperçu carte bancaire ──────────────────────────────────────────
        cardPreviewLabel = new Label("•••• •••• •••• ••••");
        cardPreviewLabel.setStyle(
                "-fx-text-fill: white; -fx-font-size: 20px; -fx-font-family: 'Courier New';"
        );
        datePrevLabel = new Label("MM/AA");
        datePrevLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 13px;");

        Label chipIcon  = new Label("💳");
        chipIcon.setStyle("-fx-font-size: 28px;");
        Label bankLabel = new Label("ChriBank");
        bankLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 12px;");
        Region spacerCard = new Region();
        HBox.setHgrow(spacerCard, Priority.ALWAYS);
        HBox cardTopRow = new HBox(chipIcon, spacerCard, bankLabel);

        VBox expireBox = new VBox(2,
                new Label("Expire") {{ setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 10px;"); }},
                datePrevLabel
        );
        HBox cardBottomRow = new HBox();
        Region spacerCardBottom = new Region();
        HBox.setHgrow(spacerCardBottom, Priority.ALWAYS);
        cardBottomRow.getChildren().addAll(spacerCardBottom, expireBox);

        VBox cardVisual = new VBox(12, cardTopRow, cardPreviewLabel, cardBottomRow);
        cardVisual.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #7B5B3A, #3D2010);" +
                        "-fx-background-radius: 14px; -fx-padding: 20px;"
        );

        // ── Champs paiement ───────────────────────────────────────────────
        numeroCarteField = new TextField();
        numeroCarteField.setPromptText("1234 5678 9012 3456");
        numeroCarteField.setStyle(fieldStyle() + "-fx-font-family: 'Courier New';");
        numeroCarteField.textProperty().addListener((obs, old, val) -> {
            // Formater automatiquement avec espaces
            String digits = val.replace(" ", "").replaceAll("[^0-9]", "");
            if (digits.length() > 16) digits = digits.substring(0, 16);
            StringBuilder formatted = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i > 0 && i % 4 == 0) formatted.append(" ");
                formatted.append(digits.charAt(i));
            }
            String result = formatted.toString();
            if (!result.equals(val)) {
                numeroCarteField.setText(result);
                numeroCarteField.positionCaret(result.length());
            }
            updateCardPreview(digits);
        });

        moisField = new TextField();
        moisField.setPromptText("MM");
        moisField.setMaxWidth(70);
        moisField.setStyle(fieldStyle());
        moisField.textProperty().addListener((obs, old, val) -> updateDatePreview());

        anneeField = new TextField();
        anneeField.setPromptText("AA");
        anneeField.setMaxWidth(70);
        anneeField.setStyle(fieldStyle());
        anneeField.textProperty().addListener((obs, old, val) -> updateDatePreview());

        cvvField = new PasswordField();
        cvvField.setPromptText("•••");
        cvvField.setMaxWidth(90);
        cvvField.setStyle(fieldStyle());

        Label slashLabel = new Label("/");
        slashLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        HBox dateRow = new HBox(8, moisField, slashLabel, anneeField);
        dateRow.setAlignment(Pos.CENTER_LEFT);

        VBox dateBoxField = new VBox(6, fieldLabel("Date d'expiration"), dateRow);
        VBox cvvBoxField  = new VBox(6, fieldLabel("CVV"), cvvField);
        HBox dateCvvRow   = new HBox(16, dateBoxField, cvvBoxField);
        HBox.setHgrow(dateBoxField, Priority.ALWAYS);

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: " + AppTheme.ERROR_COLOR + "; -fx-font-size: 13px;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        Label sslLabel = new Label("🔒  Paiement sécurisé — Données chiffrées SSL");
        sslLabel.setStyle("-fx-text-fill: " + AppTheme.TEXT_MUTED + "; -fx-font-size: 12px;");
        sslLabel.setMaxWidth(Double.MAX_VALUE);
        sslLabel.setAlignment(Pos.CENTER);

        VBox sectionPaiement = buildCard("💳  Informations de paiement",
                cardVisual,
                new VBox(6, fieldLabel("Numéro de carte"), numeroCarteField),
                dateCvvRow,
                errorLabel,
                sslLabel
        );

        // ── Boutons ───────────────────────────────────────────────────────
        Button btnAnnulerBtn = new Button("Annuler");
        btnAnnulerBtn.setMaxWidth(Double.MAX_VALUE);
        btnAnnulerBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: " + AppTheme.PRIMARY + ";" +
                        "-fx-border-width: 1.5px; -fx-border-radius: 30px;" +
                        "-fx-text-fill: " + AppTheme.PRIMARY + ";" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-padding: 13px 24px; -fx-cursor: hand;"
        );
        btnAnnulerBtn.setOnAction(e -> onAnnuler.run());

        btnConfirmer = new Button("🔒   Payer maintenant");
        AppTheme.stylePrimaryButton(btnConfirmer);
        btnConfirmer.setOnAction(e -> handlePaiement());

        HBox boutons = new HBox(16, btnAnnulerBtn, btnConfirmer);
        HBox.setHgrow(btnAnnulerBtn, Priority.ALWAYS);
        HBox.setHgrow(btnConfirmer, Priority.ALWAYS);

        leftCol.getChildren().addAll(sectionAdresse, sectionPaiement, boutons);

        // ══════════════════════════════════════════════
        // COLONNE DROITE : Récapitulatif
        // ══════════════════════════════════════════════
        produitsContainer = new VBox(10);

        totalLabel = new Label("0.00 MAD");
        totalLabel.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";"
        );
        Label totalTxt = new Label("Total");
        totalTxt.setStyle(
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";"
        );
        Region spacerTotal = new Region();
        HBox.setHgrow(spacerTotal, Priority.ALWAYS);
        HBox totalRow = new HBox(totalTxt, spacerTotal, totalLabel);
        totalRow.setAlignment(Pos.CENTER_LEFT);

        Label livraisonVal = new Label("Gratuit");
        livraisonVal.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label livraisonTxt = new Label("Livraison");
        livraisonTxt.setStyle("-fx-text-fill: " + AppTheme.TEXT_MAIN + "; -fx-font-size: 13px;");
        Region spacerLiv = new Region();
        HBox.setHgrow(spacerLiv, Priority.ALWAYS);
        HBox livraisonRow = new HBox(livraisonTxt, spacerLiv, livraisonVal);

        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: " + AppTheme.FIELD_BORDER + ";");
        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color: " + AppTheme.FIELD_BORDER + ";");

        VBox rightCol = buildCard("🛒  Récapitulatif",
                produitsContainer, sep1, livraisonRow, sep2, totalRow);
        rightCol.setMinWidth(300);
        rightCol.setMaxWidth(340);
        VBox.setVgrow(rightCol, Priority.NEVER);

        content.getChildren().addAll(leftCol, rightCol);
        root.getChildren().addAll(stepIndicator, content);
        scroll.setContent(root);
        this.setCenter(scroll);

        // ─── Charger les données ───────────────────────────────────────────
        int idUtilisateur = ((Double) userData.get("id")).intValue();
        chargerAdresses(idUtilisateur);
        chargerResume();
    }

    // ─── Step Indicator amélioré ───────────────────────────────────────────

    private HBox buildStepIndicator(int activeStep) {
        String[] labels = {"Panier", "Paiement", "Confirmation"};
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(8, 0, 8, 0));

        for (int i = 0; i < labels.length; i++) {
            boolean done   = i < activeStep;
            boolean active = i == activeStep;

            // Cercle
            StackPane circlePane = new StackPane();
            circlePane.setPrefSize(40, 40);

            Circle outerCircle = new Circle(20);
            outerCircle.setFill(done || active
                    ? Color.web(AppTheme.PRIMARY)
                    : Color.web(AppTheme.TOGGLE_INACTIVE));
            if (active) {
                outerCircle.setStroke(Color.web(AppTheme.PRIMARY));
                outerCircle.setStrokeWidth(2.5);
            }

            Label stepLbl = new Label(done ? "✓" : String.valueOf(i + 1));
            stepLbl.setStyle(
                    "-fx-font-size: 14px; -fx-font-weight: bold;" +
                            "-fx-text-fill: " + ((done || active) ? "white" : AppTheme.TEXT_MUTED) + ";"
            );
            circlePane.getChildren().addAll(outerCircle, stepLbl);

            // Texte sous le cercle
            Label stepLabel = new Label(labels[i]);
            stepLabel.setStyle(
                    "-fx-font-size: 12px; -fx-font-weight: " + (active ? "bold" : "normal") + ";" +
                            "-fx-text-fill: " + (done || active ? AppTheme.PRIMARY : AppTheme.TEXT_MUTED) + ";"
            );

            VBox stepBox = new VBox(6, circlePane, stepLabel);
            stepBox.setAlignment(Pos.CENTER);
            bar.getChildren().add(stepBox);

            // Ligne entre les étapes
            if (i < labels.length - 1) {
                VBox lineWrapper = new VBox();
                lineWrapper.setAlignment(Pos.CENTER);
                lineWrapper.setPadding(new Insets(0, 0, 20, 0));
                Region line = new Region();
                line.setPrefWidth(100);
                line.setPrefHeight(3);
                line.setStyle(
                        "-fx-background-color: " + (i < activeStep
                                ? AppTheme.PRIMARY
                                : AppTheme.TOGGLE_INACTIVE) + ";" +
                                "-fx-background-radius: 2px;"
                );
                lineWrapper.getChildren().add(line);
                bar.getChildren().add(lineWrapper);
            }
        }
        return bar;
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
        Map<String, Object> adresseSelectionnee = adresseComboBox.getValue();
        System.out.println("Adresse sélectionnée : " + adresseSelectionnee);
        System.out.println("id type : " + (adresseSelectionnee.get("id") != null
                ? adresseSelectionnee.get("id").getClass().getName()
                : "NULL"));

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
                int idUser = ((Number) userData.get("id")).intValue();
                Object idAdresseObj = adresseComboBox.getValue().get("id");
                if (idAdresseObj == null) {
                    Platform.runLater(() -> showError("Adresse invalide, veuillez en sélectionner une autre."));
                    return;
                }
                int idAdresse;
                if (idAdresseObj instanceof Number) {
                    idAdresse = ((Number) idAdresseObj).intValue();
                } else {
                    idAdresse = Integer.parseInt(idAdresseObj.toString());
                }

                // Étape 1 : Créer la commande
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
                        btnConfirmer.setText("🔒   Payer maintenant");
                        showError(respCommande != null
                                ? respCommande.getMessage()
                                : "Erreur lors de la création de la commande.");
                    });
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> commandeData = respCommande.getDataAs(Map.class);
                int    idCommandeCreee           = ((Double) commandeData.get("idCommande")).intValue();
                String uuidCommande              = (String) commandeData.get("uuidCommande");

                // Étape 2 : Traiter le paiement
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
                    btnConfirmer.setText("🔒   Payer maintenant");

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
                                ? respPaiement.getMessage() : "Paiement échoué.");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnConfirmer.setDisable(false);
                    btnConfirmer.setText("🔒   Payer maintenant");
                    showError("Erreur réseau : " + e.getMessage());
                });
            }
        }).start();
    }

    // ─── Résumé produits ───────────────────────────────────────────────────

    private void chargerResume() {
        produitsContainer.getChildren().clear();
        if (lignes == null || lignes.isEmpty()) return;

        double total = 0;
        for (Map<String, Object> ligne : lignes) {
            String nom   = String.valueOf(ligne.getOrDefault("nom", "Produit"));
            double prix  = ((Number) ligne.getOrDefault("prix_unitaire", 0)).doubleValue();
            int    qte   = ((Number) ligne.getOrDefault("quantite", 1)).intValue();
            double sous  = prix * qte;
            total       += sous;

            // Ligne produit
            Label nomLabel = new Label(nom);
            nomLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");
            nomLabel.setWrapText(true);
            HBox.setHgrow(nomLabel, Priority.ALWAYS);

            Label detailLabel = new Label(qte + " × " + String.format("%.2f MAD", prix));
            detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");

            Label montantLabel = new Label(String.format("%.2f MAD", sous));
            montantLabel.setStyle(
                    "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";"
            );

            VBox leftInfo = new VBox(2, nomLabel, detailLabel);
            HBox.setHgrow(leftInfo, Priority.ALWAYS);

            HBox row = new HBox(leftInfo, montantLabel);
            row.setAlignment(Pos.CENTER_LEFT);
            produitsContainer.getChildren().add(row);
        }

        double finalTotal = total;
        Platform.runLater(() -> totalLabel.setText(String.format("%.2f MAD", finalTotal)));
    }

    // ─── Helpers preview carte ─────────────────────────────────────────────

    private void updateCardPreview(String digits) {
        StringBuilder preview = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            if (i > 0 && i % 4 == 0) preview.append(" ");
            preview.append(i < digits.length() ? digits.charAt(i) : '•');
        }
        cardPreviewLabel.setText(preview.toString());
    }

    private void updateDatePreview() {
        String m = moisField.getText().isEmpty() ? "MM" : moisField.getText();
        String a = anneeField.getText().isEmpty() ? "AA" : anneeField.getText();
        datePrevLabel.setText(m + "/" + a);
    }

    private void openAdresseDialog() {
        AdresseDialogView dialog = new AdresseDialogView(tcpClient, null,
                adresse -> Platform.runLater(() -> adresseComboBox.getItems().add(adresse)));
        javafx.scene.Scene scene = new javafx.scene.Scene(dialog);
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setScene(scene);
        stage.setTitle("Nouvelle adresse");
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.showAndWait();
    }

    // ─── Helpers UI ────────────────────────────────────────────────────────

    private VBox buildCard(String titre, javafx.scene.Node... children) {
        Label titreLabel = new Label(titre);
        titreLabel.setStyle(
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";"
        );
        VBox card = new VBox(14);
        card.setStyle(
                "-fx-background-color: " + AppTheme.CARD_BG + ";" +
                        "-fx-background-radius: 16px; -fx-padding: 22px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 14, 0, 0, 4);"
        );
        card.getChildren().add(titreLabel);
        card.getChildren().addAll(children);
        return card;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + AppTheme.TEXT_MAIN + "; -fx-font-size: 13px;");
        return l;
    }

    private String fieldStyle() {
        return "-fx-background-color: " + AppTheme.FIELD_BG + ";" +
                "-fx-border-color: " + AppTheme.FIELD_BORDER + ";" +
                "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                "-fx-padding: 11px 14px; -fx-font-size: 14px;" +
                "-fx-text-fill: " + AppTheme.TEXT_MAIN + ";";
    }

    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
    private void hideError()           { errorLabel.setVisible(false); }
}