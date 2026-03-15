package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.core.theme.AppTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.Map;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.feather.Feather;

public class ConfirmationView extends StackPane {

    // ─── MODE SUCCÈS ──────────────────────────────────────────────────────────
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
        checkIcon.setIconColor(Color.web("#27ae60"));
        iconContainer.getChildren().add(checkIcon);

        Label titre = new Label("Commande confirmée !");
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

        VBox headerBox = new VBox(20, iconContainer, titre, uuidRow);
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

        HBox boutons = new HBox(16, btnHistorique, btnContinuer);
        boutons.setAlignment(Pos.CENTER);

        mainContainer.getChildren().addAll(headerBox, boutons);
        root.getChildren().add(mainContainer);
        scroll.setContent(root);
        this.getChildren().add(scroll);
    }

    // ─── MODE ÉCHEC ───────────────────────────────────────────────────────────
    /**
     * Crée une vue de confirmation d'échec de paiement.
     *
     * @param messageErreur  Le message d'erreur renvoyé par le serveur.
     * @param onReessayer    Callback → retourne au formulaire de paiement (Checkout).
     * @param onRetourCatalogue Callback → retourne au catalogue.
     */
    public static ConfirmationView echouee(String messageErreur,
                                           Runnable onReessayer,
                                           Runnable onRetourCatalogue) {
        ConfirmationView view = new ConfirmationView();

        view.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + AppTheme.BG + "; -fx-background-color: " + AppTheme.BG + ";");

        VBox root = new VBox(32);
        root.setPadding(new Insets(48, 32, 48, 32));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        // ─── ICÔNE ÉCHEC ───────────────────────────────────────────────────
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(80, 80);
        iconContainer.setMaxSize(80, 80);
        iconContainer.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-border-color: #E74C3C;" +
                "-fx-border-width: 6;" +
                "-fx-border-radius: 40;" +
                "-fx-background-radius: 40;"
        );

        FontIcon xIcon = new FontIcon(Feather.X);
        xIcon.setIconSize(40);
        xIcon.setIconColor(Color.web("#E74C3C"));
        iconContainer.getChildren().add(xIcon);

        Label titre = new Label("Paiement échoué");
        titre.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #E74C3C;");

        String msg = (messageErreur != null && !messageErreur.isBlank())
                ? messageErreur
                : "Une erreur s'est produite lors du traitement de votre paiement.";
        Label msgLabel = new Label(msg);
        msgLabel.setStyle("-fx-text-fill: #5C3D2E; -fx-font-size: 15px;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(460);
        msgLabel.setAlignment(Pos.CENTER);

        Label infoLabel = new Label("Votre commande a été annulée. Aucun montant ne vous a été débité.");
        infoLabel.setStyle("-fx-text-fill: #7F5539; -fx-font-size: 13px; -fx-font-style: italic;");
        infoLabel.setWrapText(true);
        infoLabel.setMaxWidth(460);
        infoLabel.setAlignment(Pos.CENTER);

        VBox headerBox = new VBox(16, iconContainer, titre, msgLabel, infoLabel);
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
        Button btnReessayer = new Button("🔄   Réessayer le paiement");
        btnReessayer.setStyle(
                "-fx-background-color: " + AppTheme.PRIMARY + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-padding: 12px 24px;" +
                "-fx-background-radius: 8px;" +
                "-fx-cursor: hand;"
        );
        btnReessayer.setOnAction(e -> onReessayer.run());

        Button btnCatalogue = new Button("Retour au catalogue");
        btnCatalogue.setStyle(
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
        btnCatalogue.setOnAction(e -> onRetourCatalogue.run());

        HBox boutons = new HBox(16, btnReessayer, btnCatalogue);
        boutons.setAlignment(Pos.CENTER);

        mainContainer.getChildren().addAll(headerBox, boutons);
        root.getChildren().add(mainContainer);
        scroll.setContent(root);
        view.getChildren().add(scroll);
        return view;
    }

    /** Constructeur privé pour la factory method `echouee()`. */
    private ConfirmationView() {}
}