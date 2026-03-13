package com.chrionline.chrionline.client.ui.components;

import com.chrionline.chrionline.server.data.models.Produit;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;

public class ProduitCard extends VBox {

    public ProduitCard(Produit produit,
                       Consumer<Produit> onVoirDetails,
                       Consumer<Produit> onAjouterPanier) {

        setStyle(
                "-fx-background-color: #EDE0D4;" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(127,85,57,0.1), 12, 0, 0, 4);"
        );
        setPrefWidth(280);
        setMaxWidth(280);
        setSpacing(0);

        //  IMAGE
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(200);
        imageContainer.setMinHeight(200);
        imageContainer.setMaxHeight(200);
        imageContainer.setStyle(
                "-fx-background-color: #DDB892;" +
                        "-fx-background-radius: 12 12 0 0;"
        );

        // Placeholder
        FontIcon placeholderIcon = new FontIcon(Feather.IMAGE);
        placeholderIcon.setIconSize(40);
        placeholderIcon.setIconColor(Color.web("#EDE0D4"));
        imageContainer.getChildren().add(placeholderIcon);

        // Charger image depuis URL
        if (produit.getUrlImage() != null && !produit.getUrlImage().isEmpty()) {
            new Thread(() -> {
                try {
                    Image image = new Image(
                            produit.getUrlImage(),
                            280, 200,
                            false,
                            true,
                            true
                    );
                    Platform.runLater(() -> {
                        if (!image.isError()) {
                            ImageView imageView = new ImageView(image);
                            imageView.setFitWidth(280);
                            imageView.setFitHeight(200);
                            imageView.setPreserveRatio(false);

                            // Arrondir les coins de l'image
                            Rectangle clip = new Rectangle(280, 200);
                            clip.setArcWidth(24);
                            clip.setArcHeight(24);
                            imageView.setClip(clip);

                            imageContainer.getChildren().setAll(imageView);
                        }
                    });
                } catch (Exception e) {
                    // garde le placeholder
                }
            }).start();
        }

        //  CONTENU
        VBox content = new VBox(10);
        content.setPadding(new Insets(14));

        // Nom
        Label nomLabel = new Label(produit.getNom());
        nomLabel.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #3B1F0E;" +
                        "-fx-wrap-text: true;"
        );
        nomLabel.setMaxWidth(Double.MAX_VALUE);

        // Prix + Stock
        HBox prixStock = new HBox();
        prixStock.setAlignment(Pos.CENTER_LEFT);

        Label prixLabel = new Label(String.format("%.2f MAD", produit.getPrix()));
        prixLabel.setStyle(
                "-fx-font-size: 22px;" +
                        "-fx-text-fill: #7F5539;" +
                        "-fx-font-weight: bold;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean enStock = produit.getStock() > 0;
        Label stockLabel = new Label(enStock ? "Stock: " + produit.getStock() : "Rupture");
        stockLabel.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-text-fill: " + (enStock ? "#16a34a" : "#dc2626") + ";"
        );
        prixStock.getChildren().addAll(prixLabel, spacer, stockLabel);

        // Bouton Voir détails
        Button voirDetailsBtn = new Button("Voir détails");
        voirDetailsBtn.setMaxWidth(Double.MAX_VALUE);
        styleButton(voirDetailsBtn, "#E6CCB2", "#7F5539", "#DDB892");
        voirDetailsBtn.setOnAction(e -> onVoirDetails.accept(produit));

        // Bouton Ajouter au panier
        Button panierBtn = new Button("Ajouter au panier");
        panierBtn.setMaxWidth(Double.MAX_VALUE);
        panierBtn.setDisable(!enStock);
        if (enStock) {
            styleButton(panierBtn, "#B08968", "#EDE0D4", "#9A7457");
        } else {
            panierBtn.setStyle(
                    "-fx-background-color: #cccccc;" +
                            "-fx-text-fill: #ffffff;" +
                            "-fx-background-radius: 8;" +
                            "-fx-padding: 9 16 9 16;" +
                            "-fx-font-size: 13px;"
            );
        }
        panierBtn.setOnAction(e -> onAjouterPanier.accept(produit));

        content.getChildren().addAll(nomLabel, prixStock, voirDetailsBtn, panierBtn);
        getChildren().addAll(imageContainer, content);
    }

    private void styleButton(Button btn, String bg, String text, String hoverBg) {
        String base =
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + text + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 9 16 9 16;" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;";
        String hover =
                "-fx-background-color: " + hoverBg + ";" +
                        "-fx-text-fill: " + text + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 9 16 9 16;" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }
}