package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.client.ui.components.ClientNavbar;
import com.chrionline.chrionline.core.interfaces.ViewManager;
import com.chrionline.chrionline.core.utils.PanierUtils;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.network.tcp.TCPClient;
import com.chrionline.chrionline.server.data.models.PanierProduit;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PanierView extends BorderPane {

    private final TCPClient client;
    private final Map<String, Object> userData;
    private final ViewManager viewManager;

    private VBox itemsContainer;
    private List<PanierProduit> panierItems = new ArrayList<>();
    private VBox recapItemsBox;
    private Label totalLabel;
    private ClientNavbar navbar;

    private static final String BG_MAIN     = "#EDE0D4";
    private static final String BG_CARD     = "#EDE0D4";
    private static final String ACCENT_DARK = "#7F5539";
    private static final String ACCENT_MID  = "#B08968";
    private static final String ACCENT_LIGHT= "#DDB892";
    private static final String TEXT_DARK   = "#3B1F0E";
    private static final String SEPARATOR   = "#DDB892";

    public PanierView(TCPClient client, Map<String, Object> userData, ViewManager viewManager) {
        this.client = client;
        this.userData = userData;
        this.viewManager = viewManager;
        setStyle("-fx-background-color: " + BG_MAIN + ";");
        buildUI();
        chargerPanier();
    }

    private void buildUI() {
        navbar = new ClientNavbar(0, userData, viewManager, null);
        setTop(navbar);
        PanierUtils.chargerCartCount(client, userData, navbar); // charge le vrai count depuis serveur

        ScrollPane scrollPane = new ScrollPane(buildContent());
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        setCenter(scrollPane);
    }

    private VBox buildContent() {
        VBox wrapper = new VBox(16);
        wrapper.setPadding(new Insets(28, 60, 40, 60));
        wrapper.setStyle("-fx-background-color: " + BG_MAIN + ";");

        Button retourBtn = new Button("← Retour au catalogue");
        retourBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ACCENT_DARK + "; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0;");
        retourBtn.setOnMouseEntered(e -> retourBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_DARK + "; -fx-font-size: 13px; -fx-cursor: hand; -fx-underline: true; -fx-padding: 0;"));
        retourBtn.setOnMouseExited(e -> retourBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ACCENT_DARK + "; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0;"));
        retourBtn.setOnAction(e -> viewManager.showCatalogueView(userData));

        Label titre = new Label("Panier");
        titre.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT_DARK + ";");

        HBox mainRow = new HBox(32);
        mainRow.setAlignment(Pos.TOP_LEFT);
        mainRow.setFillHeight(false);

        itemsContainer = new VBox(12);
        HBox.setHgrow(itemsContainer, Priority.ALWAYS);

        VBox recapSection = buildRecapSection();
        recapSection.setPrefWidth(300);
        recapSection.setMinWidth(280);
        recapSection.setMaxWidth(320);
        recapSection.setMaxHeight(Region.USE_PREF_SIZE);
        VBox.setVgrow(recapSection, Priority.NEVER);

        mainRow.getChildren().addAll(itemsContainer, recapSection);
        wrapper.getChildren().addAll(retourBtn, titre, mainRow);
        return wrapper;
    }

    private VBox buildRecapSection() {
        VBox recap = new VBox(14);
        recap.setPadding(new Insets(20));
        recap.setStyle(
                "-fx-background-color: " + BG_CARD + ";" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: " + ACCENT_LIGHT + ";" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(127,85,57,0.10), 14, 0, 0, 3);"
        );

        Label recapTitre = new Label("Récapitulatif");
        recapTitre.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT_DARK + ";");

        recapItemsBox = new VBox(10);

        Region sep = new Region();
        sep.setPrefHeight(1.5);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: " + SEPARATOR + ";");

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        Label totalTitre = new Label("Total");
        totalTitre.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT_DARK + ";");
        totalLabel = new Label("0.00 MAD");
        totalLabel.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT_DARK + ";");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        totalRow.getChildren().addAll(totalTitre, spacer, totalLabel);

        String styleBtn = "-fx-background-color: " + ACCENT_DARK + "; -fx-text-fill: " + BG_MAIN + "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 12 20 12 20; -fx-cursor: hand;";
        String styleBtnHover = "-fx-background-color: #6A4730; -fx-text-fill: " + BG_MAIN + "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 12 20 12 20; -fx-cursor: hand;";

        Button commandeBtn = new Button("Passer la commande");
        commandeBtn.setMaxWidth(Double.MAX_VALUE);
        commandeBtn.setStyle(styleBtn);
        commandeBtn.setOnMouseEntered(e -> commandeBtn.setStyle(styleBtnHover));
        commandeBtn.setOnMouseExited(e -> commandeBtn.setStyle(styleBtn));
        commandeBtn.setOnAction(e -> passerCommande());

        recap.getChildren().addAll(recapTitre, recapItemsBox, sep, totalRow, commandeBtn);
        return recap;
    }

    private HBox buildItemCard(PanierProduit item) {
        HBox card = new HBox(16);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: " + BG_CARD + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + ACCENT_LIGHT + ";" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(127,85,57,0.08), 10, 0, 0, 2);"
        );

        StackPane imgContainer = new StackPane();
        imgContainer.setPrefSize(88, 88);
        imgContainer.setMinSize(88, 88);
        imgContainer.setMaxSize(88, 88);
        imgContainer.setStyle("-fx-background-color: " + ACCENT_LIGHT + "; -fx-background-radius: 10;");
        FontIcon placeholder = new FontIcon(Feather.IMAGE);
        placeholder.setIconSize(24);
        placeholder.setIconColor(Color.web(BG_MAIN));
        imgContainer.getChildren().add(placeholder);

        if (item.getUrlImage() != null && !item.getUrlImage().isEmpty()) {
            new Thread(() -> {
                try {
                    Image image = new Image(item.getUrlImage(), true);
                    Platform.runLater(() -> {
                        if (!image.isError()) {
                            ImageView iv = new ImageView(image);
                            iv.setFitWidth(88); iv.setFitHeight(88);
                            iv.setPreserveRatio(false); iv.setSmooth(true);
                            Rectangle clip = new Rectangle(88, 88);
                            clip.setArcWidth(20); clip.setArcHeight(20);
                            iv.setClip(clip);
                            imgContainer.getChildren().setAll(iv);
                        }
                    });
                } catch (Exception ignored) {}
            }).start();
        }

        VBox info = new VBox(8);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nomLabel = new Label(item.getNomProduit());
        nomLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_DARK + ";");

        Label prixUnitLabel = new Label(String.format("%.2f MAD", item.getPrix()));
        prixUnitLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + ACCENT_DARK + ";");

        HBox quantiteControl = new HBox(0);
        quantiteControl.setAlignment(Pos.CENTER);
        quantiteControl.setMaxWidth(Region.USE_PREF_SIZE);
        quantiteControl.setStyle("-fx-border-color: " + ACCENT_LIGHT + "; -fx-border-radius: 20; -fx-border-width: 1.5; -fx-background-color: transparent; -fx-background-radius: 20;");

        Label qtyLabel = new Label(String.valueOf(item.getQuantite()));
        qtyLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_DARK + "; -fx-padding: 4 12 4 12;");

        Button moinsBtn = createQtyBtn("−");
        Button plusBtn = createQtyBtn("+");
        moinsBtn.setOnAction(e -> modifierQuantite(item, item.getQuantite() - 1, qtyLabel));
        plusBtn.setOnAction(e -> modifierQuantite(item, item.getQuantite() + 1, qtyLabel));
        quantiteControl.getChildren().addAll(moinsBtn, qtyLabel, plusBtn);
        info.getChildren().addAll(nomLabel, prixUnitLabel, quantiteControl);

        VBox rightBox = new VBox(8);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        HBox prixSupprRow = new HBox(12);
        prixSupprRow.setAlignment(Pos.CENTER_RIGHT);

        Label sousTotalLabel = new Label(String.format("%.2f MAD", item.getSousTotal()));
        sousTotalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT_DARK + ";");

        Button supprimerBtn = new Button();
        FontIcon trashIcon = new FontIcon(Feather.TRASH_2);
        trashIcon.setIconSize(15);
        trashIcon.setIconColor(Color.web("#dc2626"));
        supprimerBtn.setGraphic(trashIcon);
        supprimerBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;");
        supprimerBtn.setOnMouseEntered(e -> supprimerBtn.setStyle("-fx-background-color: #fef2f2; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4;"));
        supprimerBtn.setOnMouseExited(e -> supprimerBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;"));
        supprimerBtn.setOnAction(e -> supprimerProduit(item));

        prixSupprRow.getChildren().addAll(sousTotalLabel, supprimerBtn);
        rightBox.getChildren().add(prixSupprRow);
        card.getChildren().addAll(imgContainer, info, rightBox);
        return card;
    }

    private Button createQtyBtn(String text) {
        Button btn = new Button(text);
        btn.setPrefSize(30, 30);
        String base = "-fx-background-color: transparent; -fx-text-fill: " + ACCENT_DARK + "; -fx-font-size: 15px; -fx-font-weight: bold; -fx-cursor: hand;";
        String hover = "-fx-background-color: " + ACCENT_LIGHT + "; -fx-text-fill: " + ACCENT_DARK + "; -fx-font-size: 15px; -fx-font-weight: bold; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private VBox buildEmptyState() {
        VBox empty = new VBox(16);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(80, 0, 80, 0));
        FontIcon bagIcon = new FontIcon(Feather.SHOPPING_BAG);
        bagIcon.setIconSize(64);
        bagIcon.setIconColor(Color.web(ACCENT_MID));
        Label titre = new Label("Votre panier est vide");
        titre.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT_DARK + ";");
        Label desc = new Label("Découvrez nos produits artisanaux");
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ACCENT_MID + ";");
        Button catalogueBtn = new Button("Voir le catalogue");
        catalogueBtn.setStyle("-fx-background-color: " + ACCENT_DARK + "; -fx-text-fill: " + BG_MAIN + "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 24 10 24; -fx-cursor: hand;");
        catalogueBtn.setOnAction(e -> viewManager.showCatalogueView(userData));
        empty.getChildren().addAll(bagIcon, titre, desc, catalogueBtn);
        return empty;
    }

    private void updateRecap() {
        recapItemsBox.getChildren().clear();
        double total = 0;
        for (PanierProduit item : panierItems) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            VBox leftInfo = new VBox(2);
            Label nomLabel = new Label(item.getNomProduit());
            nomLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_DARK + ";");
            Label detailLabel = new Label(item.getQuantite() + " × " + String.format("%.2f MAD", item.getPrix()));
            detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + ACCENT_MID + ";");
            leftInfo.getChildren().addAll(nomLabel, detailLabel);
            HBox.setHgrow(leftInfo, Priority.ALWAYS);
            Label montantLabel = new Label(String.format("%.2f MAD", item.getSousTotal()));
            montantLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + ACCENT_DARK + ";");
            row.getChildren().addAll(leftInfo, montantLabel);
            recapItemsBox.getChildren().add(row);
            total += item.getSousTotal();
        }
        totalLabel.setText(String.format("%.2f MAD", total));
    }

    private void chargerPanier() {
        new Thread(() -> {
            try {
                int idUtilisateur = ((Double) userData.get("id")).intValue();
                AppRequest request = new AppRequest.Builder()
                        .controller("Panier")
                        .action("getPanier")
                        .parameter("idUtilisateur", idUtilisateur)
                        .authToken(client.getAuthToken())
                        .build();
                AppResponse response = client.sendAndParse(request);
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        Type type = new TypeToken<List<PanierProduit>>(){}.getType();
                        panierItems = new Gson().fromJson(new Gson().toJson(response.getData()), type);
                        if (panierItems == null) panierItems = new ArrayList<>();
                        afficherItems();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void afficherItems() {
        itemsContainer.getChildren().clear();
        if (panierItems.isEmpty()) {
            itemsContainer.getChildren().add(buildEmptyState());
            return;
        }
        for (PanierProduit item : panierItems) {
            itemsContainer.getChildren().add(buildItemCard(item));
        }
        updateRecap();
    }

    private void modifierQuantite(PanierProduit item, int nouvelleQte, Label qtyLabel) {
        if (nouvelleQte <= 0) {
            supprimerProduit(item);
            return;
        }
        new Thread(() -> {
            try {
                int idUtilisateur = ((Double) userData.get("id")).intValue();
                AppRequest request = new AppRequest.Builder()
                        .controller("Panier")
                        .action("modifierQuantite")
                        .parameter("idUtilisateur", idUtilisateur)
                        .parameter("idProduit", item.getIdProduit())
                        .parameter("quantite", nouvelleQte)
                        .authToken(client.getAuthToken())
                        .build();
                AppResponse response = client.sendAndParse(request);
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        item.setQuantite(nouvelleQte);
                        qtyLabel.setText(String.valueOf(nouvelleQte));
                        updateRecap();
                        PanierUtils.chargerCartCount(client, userData, navbar);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void supprimerProduit(PanierProduit item) {
        new Thread(() -> {
            try {
                int idUtilisateur = ((Double) userData.get("id")).intValue();
                AppRequest request = new AppRequest.Builder()
                        .controller("Panier")
                        .action("supprimerProduit")
                        .parameter("idUtilisateur", idUtilisateur)
                        .parameter("idProduit", item.getIdProduit())
                        .authToken(client.getAuthToken())
                        .build();
                AppResponse response = client.sendAndParse(request);
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        panierItems.remove(item);
                        afficherItems();
                        PanierUtils.chargerCartCount(client, userData, navbar);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void passerCommande() {
        System.out.println("Passer commande...");
    }
}