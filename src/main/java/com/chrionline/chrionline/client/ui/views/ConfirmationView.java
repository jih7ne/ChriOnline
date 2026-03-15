package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.core.theme.AppTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Map;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.feather.Feather;

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
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(80, 80);
        iconContainer.setMaxSize(80, 80);
        iconContainer.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-border-color: #27ae60;" +
                "-fx-border-width: 6;" +
                "-fx-border-radius: 40;" +
                "-fx-background-radius: 40;"
        );

        FontIcon checkIcon = new FontIcon(Feather.CHECK);
        checkIcon.setIconSize(40);
        checkIcon.setIconColor(javafx.scene.paint.Color.web("#27ae60"));
        iconContainer.getChildren().add(checkIcon);

        Label titre = new Label("Commande confirmée!");
        titre.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #7F5539;");

        // ─── UUID COMMANDE ─────────────────────────────────────────────────
        String uuid = paiementData != null
                ? String.valueOf(paiementData.getOrDefault("uuidCommande", "N/A"))
                : "N/A";

        Label numLabel = new Label("Numéro de commande : ");
        numLabel.setStyle("-fx-text-fill: #5C3D2E; -fx-font-size: 16px;");
        Label uuidLabel = new Label("#" + uuid.toUpperCase().substring(0, Math.min(uuid.length(), 13)));
        uuidLabel.setStyle("-fx-text-fill: #5C3D2E; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        HBox uuidRow = new HBox(numLabel, uuidLabel);
        uuidRow.setAlignment(Pos.CENTER);

        Label sousTitre = new Label("Vous recevrez un email de confirmation sous peu");
        sousTitre.setStyle("-fx-font-size: 14px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");

        VBox headerBox = new VBox(20, iconContainer, titre, uuidRow, sousTitre);
        headerBox.setAlignment(Pos.CENTER);
        
        VBox mainContainer = new VBox(40);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setMaxWidth(600);
        mainContainer.setPadding(new Insets(40));
        mainContainer.setStyle(
                "-fx-background-color: " + AppTheme.CARD_BG + ";" +
                        "-fx-background-radius: 16px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4);"
        );

        // ─── BOUTONS ───────────────────────────────────────────────────────
        Button btnHistorique = new Button("Voir mes commandes");
        btnHistorique.setStyle(
                "-fx-background-color: " + AppTheme.PRIMARY + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-padding: 12px 24px;" +
                "-fx-background-radius: 8px;" +
                "-fx-cursor: hand;"
        );
        btnHistorique.setOnAction(e -> onVoirHistorique.run());
        btnHistorique.setOnMouseEntered(e -> btnHistorique.setStyle(btnHistorique.getStyle() + "-fx-opacity: 0.9;"));
        btnHistorique.setOnMouseExited(e -> btnHistorique.setStyle(btnHistorique.getStyle().replace("-fx-opacity: 0.9;", "")));

        Button btnContinuer = new Button("Continuer mes achats");
        btnContinuer.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-border-color: " + AppTheme.PRIMARY + ";" +
                "-fx-border-width: 1.5;" +
                "-fx-text-fill: " + AppTheme.PRIMARY + ";" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-padding: 10px 24px;" +
                "-fx-background-radius: 8px;" +
                "-fx-border-radius: 8px;" +
                "-fx-cursor: hand;"
        );
        btnContinuer.setOnAction(e -> onContinuerAchats.run());
        btnContinuer.setOnMouseEntered(e -> btnContinuer.setStyle(btnContinuer.getStyle() + "-fx-background-color: rgba(127, 85, 57, 0.05);"));
        btnContinuer.setOnMouseExited(e -> btnContinuer.setStyle(btnContinuer.getStyle().replace("-fx-background-color: rgba(127, 85, 57, 0.05);", "")));

        HBox boutons = new HBox(16, btnHistorique, btnContinuer);
        boutons.setAlignment(Pos.CENTER);

        mainContainer.getChildren().addAll(headerBox, boutons);
        root.getChildren().add(mainContainer);
        scroll.setContent(root);
        this.getChildren().add(scroll);
    }
}