package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.client.ui.components.ClientNavbar;
import com.chrionline.chrionline.core.interfaces.ViewManager;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.network.tcp.TCPClient;
import com.chrionline.chrionline.server.data.models.Produit;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Map;

public class DetailsProduitView extends BorderPane {

    private final TCPClient client;
    private final Map<String, Object> userData;
    private final ViewManager viewManager;
    private final Produit produit;

    private int quantite = 1;
    private Label quantiteLabel;
    private Label totalLabel;
    private int cartCount = 0;
    private ClientNavbar navbar;

    public DetailsProduitView(TCPClient client, Produit produit,
                              Map<String, Object> userData, ViewManager viewManager) {
        this.client = client;
        this.produit = produit;
        this.userData = userData;
        this.viewManager = viewManager;

        setStyle("-fx-background-color: #EDE0D4;");
        buildUI();
    }

    private void buildUI() {
        // Navbar
        navbar = new ClientNavbar(cartCount, userData, viewManager, null);
        setTop(navbar);

        ScrollPane scrollPane = new ScrollPane(buildContent());
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        setCenter(scrollPane);
    }

    private VBox buildContent() {
        VBox content = new VBox(32);
        content.setPadding(new Insets(24, 64, 32, 64));
        content.setStyle("-fx-background-color: #EDE0D4;");

        // Retour
        Button retourBtn = new Button("← Retour au catalogue");
        retourBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #7F5539;" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0;"
        );
        retourBtn.setOnMouseEntered(e -> retourBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #3B1F0E;" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;" +
                        "-fx-underline: true;" +
                        "-fx-padding: 0;"
        ));
        retourBtn.setOnMouseExited(e -> retourBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #7F5539;" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0;"
        ));
        retourBtn.setOnAction(e -> viewManager.showCatalogueView(userData));

        // Section principale
        HBox topSection = new HBox(48);
        topSection.setAlignment(Pos.CENTER);
        topSection.setMaxWidth(1000);

        StackPane imageContainer = buildImageSection();
        VBox infoSection = buildInfoSection();
        infoSection.setPrefWidth(420);
        infoSection.setMinWidth(380);
        infoSection.setMaxWidth(420);

        topSection.getChildren().addAll(imageContainer, infoSection);

        // Wrapper centré
        HBox topWrapper = new HBox(topSection);
        topWrapper.setAlignment(Pos.CENTER);

        // Tabs
        VBox tabsSection = buildTabsSection();
        tabsSection.setMaxWidth(1000);

        HBox tabsWrapper = new HBox(tabsSection);
        tabsWrapper.setAlignment(Pos.CENTER);
        HBox.setHgrow(tabsSection, Priority.ALWAYS);

        content.getChildren().addAll(retourBtn, topWrapper, tabsWrapper);
        return content;
    }

    private StackPane buildImageSection() {
        StackPane container = new StackPane();
        container.setPrefWidth(480);
        container.setMinWidth(480);
        container.setMaxWidth(480);
        container.setPrefHeight(360);
        container.setMinHeight(360);
        container.setMaxHeight(360);
        container.setStyle(
                "-fx-background-color: #DDB892;" +
                        "-fx-background-radius: 16;"
        );

        FontIcon placeholder = new FontIcon(Feather.IMAGE);
        placeholder.setIconSize(48);
        placeholder.setIconColor(Color.web("#EDE0D4"));
        container.getChildren().add(placeholder);

        if (produit.getUrlImage() != null && !produit.getUrlImage().isEmpty()) {
            new Thread(() -> {
                try {
                    Image image = new Image(produit.getUrlImage(), true);
                    Platform.runLater(() -> {
                        if (!image.isError()) {
                            ImageView imageView = new ImageView(image);
                            imageView.setFitWidth(480);
                            imageView.setFitHeight(360);
                            imageView.setPreserveRatio(false);
                            imageView.setSmooth(true);

                            Rectangle clip = new Rectangle(480, 360);
                            clip.setArcWidth(32);
                            clip.setArcHeight(32);
                            imageView.setClip(clip);

                            container.getChildren().setAll(imageView);
                        }
                    });
                } catch (Exception ignored) {}
            }).start();
        }
        return container;
    }

    private VBox buildInfoSection() {
        VBox info = new VBox(16);
        info.setAlignment(Pos.TOP_LEFT);

        Label categorieLabel = new Label(
                produit.getNomCategorie() != null ? produit.getNomCategorie() : ""
        );
        categorieLabel.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-text-fill: #B08968;"
        );

        Label nomLabel = new Label(produit.getNom());
        nomLabel.setStyle(
                "-fx-font-size: 32px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #7F5539;" +
                        "-fx-wrap-text: true;"
        );
        nomLabel.setMaxWidth(Double.MAX_VALUE);

        HBox prixStock = new HBox(16);
        prixStock.setAlignment(Pos.CENTER_LEFT);

        Label prixLabel = new Label(String.format("%.2f MAD", produit.getPrix()));
        prixLabel.setStyle(
                "-fx-font-size: 36px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #7F5539;"
        );

        HBox stockBox = new HBox(6);
        stockBox.setAlignment(Pos.CENTER);
        Circle dot = new Circle(5);
        boolean enStock = produit.getStock() > 0;
        dot.setFill(Color.web(enStock ? "#16a34a" : "#dc2626"));
        Label stockLabel = new Label(
                enStock ? "En stock (" + produit.getStock() + " unités)" : "Rupture de stock"
        );
        stockLabel.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-text-fill: " + (enStock ? "#16a34a" : "#dc2626") + ";"
        );
        stockBox.getChildren().addAll(dot, stockLabel);
        prixStock.getChildren().addAll(prixLabel, stockBox);

        Label descLabel = new Label(produit.getDescription() != null ?
                produit.getDescription() : "");
        descLabel.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-text-fill: #3B1F0E;" +
                        "-fx-wrap-text: true;" +
                        "-fx-line-spacing: 4;"
        );
        descLabel.setMaxWidth(Double.MAX_VALUE);

        Label quantiteTitre = new Label("Quantité");
        quantiteTitre.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #3B1F0E;"
        );

        HBox quantiteRow = new HBox(16);
        quantiteRow.setAlignment(Pos.CENTER_LEFT);

        HBox quantiteControl = new HBox(0);
        quantiteControl.setAlignment(Pos.CENTER);
        quantiteControl.setStyle(
                "-fx-border-color: #DDB892;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-width: 2;" +
                        "-fx-background-radius: 8;"
        );

        Button moinsBtn = createQuantiteBtn("-");
        moinsBtn.setOnAction(e -> {
            if (quantite > 1) {
                quantite--;
                quantiteLabel.setText(String.valueOf(quantite));
                updateTotal();
            }
        });

        quantiteLabel = new Label("1");
        quantiteLabel.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-text-fill: #3B1F0E;" +
                        "-fx-padding: 8 20 8 20;" +
                        "-fx-font-weight: bold;"
        );

        Button plusBtn = createQuantiteBtn("+");
        plusBtn.setOnAction(e -> {
            if (quantite < produit.getStock()) {
                quantite++;
                quantiteLabel.setText(String.valueOf(quantite));
                updateTotal();
            }
        });

        quantiteControl.getChildren().addAll(moinsBtn, quantiteLabel, plusBtn);

        HBox totalBox = new HBox(6);
        totalBox.setAlignment(Pos.CENTER_LEFT);
        Label totalTitre = new Label("Total:");
        totalTitre.setStyle("-fx-font-size: 14px; -fx-text-fill: #7F5539;");
        totalLabel = new Label(String.format("%.2f MAD", produit.getPrix()));
        totalLabel.setStyle(
                "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #7F5539;"
        );
        totalBox.getChildren().addAll(totalTitre, totalLabel);
        quantiteRow.getChildren().addAll(quantiteControl, totalBox);

        Button panierBtn = new Button("  Ajouter au panier");
        panierBtn.setMaxWidth(Double.MAX_VALUE);
        panierBtn.setDisable(!enStock);

        FontIcon cartIcon = new FontIcon(Feather.SHOPPING_CART);
        cartIcon.setIconSize(18);
        cartIcon.setIconColor(Color.web("#EDE0D4"));
        panierBtn.setGraphic(cartIcon);

        panierBtn.setStyle(
                "-fx-background-color: #7F5539;" +
                        "-fx-text-fill: #EDE0D4;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 14 24 14 24;" +
                        "-fx-cursor: hand;"
        );
        panierBtn.setOnMouseEntered(e -> panierBtn.setStyle(
                "-fx-background-color: #6A4730;" +
                        "-fx-text-fill: #EDE0D4;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 14 24 14 24;" +
                        "-fx-cursor: hand;"
        ));
        panierBtn.setOnMouseExited(e -> panierBtn.setStyle(
                "-fx-background-color: #7F5539;" +
                        "-fx-text-fill: #EDE0D4;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 14 24 14 24;" +
                        "-fx-cursor: hand;"
        ));
        panierBtn.setOnAction(e -> ajouterAuPanier());

        info.getChildren().addAll(
                categorieLabel,
                nomLabel,
                prixStock,
                descLabel,
                new Separator(),
                quantiteTitre,
                quantiteRow,
                panierBtn
        );
        return info;
    }

    private Button createQuantiteBtn(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(40, 40);
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #7F5539;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #E6CCB2;" +
                        "-fx-text-fill: #7F5539;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #7F5539;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: transparent;"
        ));
        return btn;
    }

    private VBox buildTabsSection() {
        VBox section = new VBox(16);
        section.setStyle(
                "-fx-background-color: #EDE0D4;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(127,85,57,0.08), 12, 0, 0, 2);"
        );
        section.setPadding(new Insets(24));

        HBox tabsHeader = new HBox(8);
        Button descTab = createTabBtn("Description", true);
        Button caracTab = createTabBtn("Caractéristiques", false);

        VBox tabContent = new VBox(12);
        tabContent.setPadding(new Insets(8, 0, 0, 0));
        showDescriptionTab(tabContent);

        descTab.setOnAction(e -> {
            styleTabActive(descTab);
            styleTabInactive(caracTab);
            showDescriptionTab(tabContent);
        });
        caracTab.setOnAction(e -> {
            styleTabActive(caracTab);
            styleTabInactive(descTab);
            showCaracteristiquesTab(tabContent);
        });

        tabsHeader.getChildren().addAll(descTab, caracTab);
        section.getChildren().addAll(tabsHeader, tabContent);
        return section;
    }

    private Button createTabBtn(String text, boolean active) {
        Button btn = new Button(text);
        if (active) styleTabActive(btn);
        else styleTabInactive(btn);
        return btn;
    }

    private void styleTabActive(Button btn) {
        btn.setStyle(
                "-fx-background-color: #7F5539;" +
                        "-fx-text-fill: #EDE0D4;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 7 18 7 18;" +
                        "-fx-cursor: hand;"
        );
    }

    private void styleTabInactive(Button btn) {
        btn.setStyle(
                "-fx-background-color: #E6CCB2;" +
                        "-fx-text-fill: #7F5539;" +
                        "-fx-font-size: 13px;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 7 18 7 18;" +
                        "-fx-cursor: hand;"
        );
    }

    private void showDescriptionTab(VBox container) {
        container.getChildren().clear();
        String desc = produit.getDescription() != null ?
                produit.getDescription() : "Aucune description disponible.";
        Label descLabel = new Label(desc);
        descLabel.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-text-fill: #3B1F0E;" +
                        "-fx-wrap-text: true;" +
                        "-fx-line-spacing: 4;"
        );
        descLabel.setMaxWidth(Double.MAX_VALUE);
        container.getChildren().add(descLabel);
    }

    private void showCaracteristiquesTab(VBox container) {
        container.getChildren().clear();
        String[] features = {
                "Catégorie: " + (produit.getNomCategorie() != null ? produit.getNomCategorie() : "N/A"),
                "Prix: " + String.format("%.2f MAD", produit.getPrix()),
                "Stock disponible: " + produit.getStock() + " unités",
                "Référence: #" + produit.getId()
        };
        for (String feature : features) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            Circle dot = new Circle(4);
            dot.setFill(Color.web("#B08968"));
            Label label = new Label(feature);
            label.setStyle("-fx-font-size: 14px; -fx-text-fill: #3B1F0E;");
            row.getChildren().addAll(dot, label);
            container.getChildren().add(row);
        }
    }

    private void updateTotal() {
        totalLabel.setText(String.format("%.2f MAD", produit.getPrix() * quantite));
    }

    private void ajouterAuPanier() {
        new Thread(() -> {
            try {
                int idUtilisateur = ((Double) userData.get("id")).intValue();
                AppRequest request = new AppRequest.Builder()
                        .controller("Panier")
                        .action("ajouterProduit")
                        .parameter("idUtilisateur", idUtilisateur)
                        .parameter("idProduit", produit.getId())
                        .parameter("quantite", quantite)
                        .authToken(client.getAuthToken())
                        .build();
                AppResponse response = client.sendAndParse(request);
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        cartCount += quantite;
                        navbar.updateCartCount(cartCount);
                        // Retourner au catalogue
                        viewManager.showCatalogueView(userData);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}