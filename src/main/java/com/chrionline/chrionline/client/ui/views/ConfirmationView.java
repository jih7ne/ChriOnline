package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.core.theme.AppTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;

import java.util.Map;

public class ConfirmationView extends StackPane {

    public ConfirmationView(Map<String, Object> paiementData,
                            Runnable onVoirHistorique,
                            Runnable onContinuerAchats) {

        this.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + AppTheme.BG + "; -fx-background-color: " + AppTheme.BG + ";");

        VBox root = new VBox(32);
        root.setPadding(new Insets(48, 32, 48, 32));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        // ─── ICÔNE SUCCÈS ──────────────────────────────────────────────────
        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 72px;");

        Label titre = new Label("Commande confirmée !");
        titre.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";");

        Label sousTitre = new Label("Merci pour votre achat");
        sousTitre.setStyle("-fx-font-size: 16px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");

        VBox headerBox = new VBox(8, icon, titre, sousTitre);
        headerBox.setAlignment(Pos.CENTER);

        // ─── UUID COMMANDE ─────────────────────────────────────────────────
        String uuid = paiementData != null
                ? String.valueOf(paiementData.getOrDefault("uuidCommande", "N/A"))
                : "N/A";

        Label uuidLabel = new Label(uuid);
        uuidLabel.setStyle(
                "-fx-font-family: 'Courier New'; -fx-font-size: 16px;" +
                        "-fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";"
        );

        Label copiedLabel = new Label("✓ Copié !");
        copiedLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
        copiedLabel.setVisible(false);

        Button btnCopier = new Button("📋 Copier");
        btnCopier.setStyle(
                "-fx-background-color: " + AppTheme.FIELD_BG + ";" +
                        "-fx-border-color: " + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius: 8px; -fx-background-radius: 8px;" +
                        "-fx-text-fill: " + AppTheme.PRIMARY + "; -fx-font-size: 12px;" +
                        "-fx-padding: 6px 12px; -fx-cursor: hand;"
        );
        btnCopier.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(uuid);
            Clipboard.getSystemClipboard().setContent(content);
            copiedLabel.setVisible(true);
            new Thread(() -> {
                try { Thread.sleep(2000); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                javafx.application.Platform.runLater(() -> copiedLabel.setVisible(false));
            }).start();
        });

        HBox uuidRow = new HBox(12, uuidLabel, btnCopier);
        uuidRow.setAlignment(Pos.CENTER);

        Label numLabel = new Label("Numéro de commande");
        numLabel.setStyle("-fx-text-fill: " + AppTheme.TEXT_MUTED + "; -fx-font-size: 13px;");

        VBox uuidBox = new VBox(8, numLabel, uuidRow, copiedLabel);
        uuidBox.setAlignment(Pos.CENTER);
        uuidBox.setMaxWidth(600);
        uuidBox.setStyle(
                "-fx-background-color: " + AppTheme.CARD_BG + ";" +
                        "-fx-background-radius: 16px; -fx-padding: 24px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 12, 0, 0, 4);"
        );

        // ─── RÉSUMÉ COMPLET ────────────────────────────────────────────────
        String numeroMasque = paiementData != null
                ? String.valueOf(paiementData.getOrDefault("numeroMasque", "****"))
                : "****";
        String statut = paiementData != null
                ? String.valueOf(paiementData.getOrDefault("statut", "CONFIRME"))
                : "CONFIRME";

        VBox resumeBox = new VBox(16);
        resumeBox.setMaxWidth(600);
        resumeBox.setStyle(
                "-fx-background-color: " + AppTheme.CARD_BG + ";" +
                        "-fx-background-radius: 16px; -fx-padding: 24px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 12, 0, 0, 4);"
        );

        Label resumeTitre = new Label("Détails de la commande");
        resumeTitre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";");

        resumeBox.getChildren().addAll(
                resumeTitre,
                infoRow("💳  Paiement :", "**** **** **** " + numeroMasque.replace("**** **** **** ", "")),
                infoRow("✅  Statut :",   statut),
                new Separator()
        );

        // ─── BOUTONS ───────────────────────────────────────────────────────
        Button btnHistorique = new Button("Voir mes commandes");
        AppTheme.stylePrimaryButton(btnHistorique);
        btnHistorique.setOnAction(e -> onVoirHistorique.run());

        Button btnContinuer = new Button("Continuer mes achats");
        AppTheme.styleOutlineButton(btnContinuer);
        btnContinuer.setOnAction(e -> onContinuerAchats.run());

        HBox boutons = new HBox(16, btnHistorique, btnContinuer);
        boutons.setAlignment(Pos.CENTER);
        boutons.setMaxWidth(500);
        HBox.setHgrow(btnHistorique, Priority.ALWAYS);
        HBox.setHgrow(btnContinuer, Priority.ALWAYS);
        btnHistorique.setMaxWidth(Double.MAX_VALUE);
        btnContinuer.setMaxWidth(Double.MAX_VALUE);

        root.getChildren().addAll(headerBox, uuidBox, resumeBox, boutons);
        scroll.setContent(root);
        this.getChildren().add(scroll);
    }

    private HBox infoRow(String label, String valeur) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: " + AppTheme.TEXT_MUTED + "; -fx-font-size: 14px;");
        lbl.setMinWidth(130);
        Label val = new Label(valeur);
        val.setStyle("-fx-text-fill: " + AppTheme.TEXT_MAIN + "; -fx-font-size: 14px;");
        val.setWrapText(true);
        return new HBox(lbl, val);
    }
}