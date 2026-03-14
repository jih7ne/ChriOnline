package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.client.ui.components.ClientNavbar;
import com.chrionline.chrionline.client.ui.components.ProduitCard;
import com.chrionline.chrionline.core.interfaces.ViewManager;
import com.chrionline.chrionline.core.utils.JsonUtils;
import com.chrionline.chrionline.core.utils.PanierUtils;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.network.tcp.TCPClient;
import com.chrionline.chrionline.server.data.models.Produit;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CatalogueView extends BorderPane {

    private final TCPClient client;
    private final Map<String, Object> userData;
    private final ViewManager viewManager;

    private FlowPane produitsGrid;
    private VBox sidebarCategories;
    private Label totalLabel;
    private List<Produit> tousLesProduits = new ArrayList<>();
    private final List<String> categoriesSelectionnees = new ArrayList<>();
    private ClientNavbar navbar;

    public CatalogueView(TCPClient client, Map<String, Object> userData, ViewManager viewManager) {
        this.client = client;
        this.userData = userData;
        this.viewManager = viewManager;
        setStyle("-fx-background-color: #EDE0D4;");
        buildUI();
        chargerProduits();
    }

    private void buildUI() {
        navbar = new ClientNavbar(0, userData, viewManager, this::rechercherProduits);
        setTop(navbar);
        PanierUtils.chargerCartCount(client, userData, navbar); // charge le vrai count depuis serveur

        HBox content = new HBox(0);
        content.setPadding(new Insets(24, 24, 24, 0));

        VBox sidebar = buildSidebar();
        VBox main = buildMainContent();
        HBox.setHgrow(main, Priority.ALWAYS);

        content.getChildren().addAll(sidebar, main);
        setCenter(content);
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(6);
        sidebar.setPadding(new Insets(24, 20, 8, 40));
        sidebar.setMinWidth(230);
        sidebar.setMaxWidth(230);

        Label titre = new Label("Catégories");
        titre.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #7F5539; -fx-padding: 0 0 10 0;");

        HBox toutAfficher = new HBox();
        toutAfficher.setAlignment(Pos.CENTER_LEFT);
        toutAfficher.setPadding(new Insets(7, 14, 7, 14));
        toutAfficher.setStyle("-fx-background-color: #B08968; -fx-background-radius: 8; -fx-cursor: hand;");
        Label toutLabel = new Label("Tout afficher");
        toutLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #EDE0D4; -fx-font-weight: bold;");
        toutAfficher.getChildren().add(toutLabel);
        toutAfficher.setMaxWidth(Double.MAX_VALUE);
        toutAfficher.setOnMouseEntered(e -> toutAfficher.setStyle("-fx-background-color: #9A7457; -fx-background-radius: 8; -fx-cursor: hand;"));
        toutAfficher.setOnMouseExited(e -> toutAfficher.setStyle("-fx-background-color: #B08968; -fx-background-radius: 8; -fx-cursor: hand;"));
        toutAfficher.setOnMouseClicked(e -> {
            categoriesSelectionnees.clear();
            afficherCategories(tousLesProduits);
            afficherProduits(tousLesProduits);
        });

        sidebarCategories = new VBox(4);
        sidebar.getChildren().addAll(titre, toutAfficher, sidebarCategories);
        return sidebar;
    }

    private VBox buildMainContent() {
        VBox main = new VBox(16);
        main.setPadding(new Insets(24, 24, 24, 16));

        Label titre = new Label("Catalogue Produits");
        titre.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #7F5539;");

        totalLabel = new Label("Chargement...");
        totalLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7F5539;");

        produitsGrid = new FlowPane();
        produitsGrid.setHgap(16);
        produitsGrid.setVgap(16);

        ScrollPane scrollPane = new ScrollPane(produitsGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        main.getChildren().addAll(titre, totalLabel, scrollPane);
        return main;
    }

    private void rechercherProduits(String query) {
        if (query == null || query.isEmpty()) {
            afficherProduits(tousLesProduits);
        } else {
            List<Produit> filtres = tousLesProduits.stream()
                    .filter(p -> p.getNom().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
            afficherProduits(filtres);
        }
    }

    private void chargerProduits() {
        new Thread(() -> {
            try {
                AppRequest request = new AppRequest.Builder()
                        .controller("Produit")
                        .action("lister")
                        .authToken(client.getAuthToken())
                        .build();
                AppResponse response = client.sendAndParse(request);
                if (response.isSuccess()) {
                    Type type = new TypeToken<List<Produit>>(){}.getType();
                    String json = new JsonUtils().toJson(response.getData());
                    List<Produit> produits = JsonUtils.fromJson(json, type);
                    Platform.runLater(() -> {
                        tousLesProduits = produits;
                        afficherCategories(produits);
                        afficherProduits(produits);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> totalLabel.setText("Erreur de chargement"));
            }
        }).start();
    }

    private void afficherCategories(List<Produit> produits) {
        List<String> categories = produits.stream()
                .map(Produit::getNomCategorie)
                .filter(c -> c != null && !c.isEmpty())
                .distinct().sorted()
                .collect(Collectors.toList());

        sidebarCategories.getChildren().clear();

        for (String cat : categories) {
            HBox catItem = new HBox(10);
            catItem.setAlignment(Pos.CENTER_LEFT);
            catItem.setPadding(new Insets(6, 12, 6, 8));
            catItem.setStyle("-fx-background-radius: 8; -fx-cursor: hand;");

            StackPane checkBox = new StackPane();
            checkBox.setPrefSize(18, 18);
            checkBox.setMinSize(18, 18);
            checkBox.setStyle("-fx-background-color: #EDE0D4; -fx-background-radius: 4; -fx-border-color: #B08968; -fx-border-radius: 4; -fx-border-width: 1.5;");

            Label checkMark = new Label("✓");
            checkMark.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;");
            checkMark.setVisible(false);
            checkBox.getChildren().add(checkMark);

            Label catLabel = new Label(cat);
            catLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #3B1F0E;");
            catItem.getChildren().addAll(checkBox, catLabel);

            final boolean[] selected = {false};
            catItem.setOnMouseClicked(e -> {
                selected[0] = !selected[0];
                if (selected[0]) {
                    categoriesSelectionnees.add(cat);
                    checkBox.setStyle("-fx-background-color: #B08968; -fx-background-radius: 4; -fx-border-color: #B08968; -fx-border-radius: 4; -fx-border-width: 1.5;");
                    checkMark.setVisible(true);
                    catLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7F5539; -fx-font-weight: bold;");
                    catItem.setStyle("-fx-background-color: #E6CCB2; -fx-background-radius: 8; -fx-cursor: hand;");
                } else {
                    categoriesSelectionnees.remove(cat);
                    checkBox.setStyle("-fx-background-color: #EDE0D4; -fx-background-radius: 4; -fx-border-color: #B08968; -fx-border-radius: 4; -fx-border-width: 1.5;");
                    checkMark.setVisible(false);
                    catLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #3B1F0E;");
                    catItem.setStyle("-fx-background-radius: 8; -fx-cursor: hand;");
                }
                filtrerProduits();
            });
            catItem.setOnMouseEntered(e -> { if (!selected[0]) catItem.setStyle("-fx-background-color: #E6CCB2; -fx-background-radius: 8; -fx-cursor: hand;"); });
            catItem.setOnMouseExited(e -> { if (!selected[0]) catItem.setStyle("-fx-background-radius: 8; -fx-cursor: hand;"); });
            sidebarCategories.getChildren().add(catItem);
        }
    }

    private void filtrerProduits() {
        if (categoriesSelectionnees.isEmpty()) {
            afficherProduits(tousLesProduits);
        } else {
            List<Produit> filtres = tousLesProduits.stream()
                    .filter(p -> categoriesSelectionnees.contains(p.getNomCategorie()))
                    .collect(Collectors.toList());
            afficherProduits(filtres);
        }
    }

    private void afficherProduits(List<Produit> produits) {
        produitsGrid.getChildren().clear();
        int nb = produits.size();
        totalLabel.setText(nb + " produit" + (nb > 1 ? "s" : "") + " disponible" + (nb > 1 ? "s" : ""));
        for (Produit p : produits) {
            ProduitCard card = new ProduitCard(
                    p,
                    prod -> viewManager.showDetailsProduit(prod, userData),
                    prod -> ajouterAuPanier(prod)
            );
            produitsGrid.getChildren().add(card);
        }
    }

    private void ajouterAuPanier(Produit produit) {
        new Thread(() -> {
            try {
                int idUtilisateur = ((Double) userData.get("id")).intValue();
                AppRequest request = new AppRequest.Builder()
                        .controller("Panier")
                        .action("ajouterProduit")
                        .parameter("idUtilisateur", idUtilisateur)
                        .parameter("idProduit", produit.getId())
                        .parameter("quantite", 1)
                        .authToken(client.getAuthToken())
                        .build();
                AppResponse response = client.sendAndParse(request);
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        PanierUtils.chargerCartCount(client, userData, navbar);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> totalLabel.setText("Erreur panier"));
            }
        }).start();
    }
}