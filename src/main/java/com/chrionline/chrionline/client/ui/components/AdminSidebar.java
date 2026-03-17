package com.chrionline.chrionline.client.ui.components;

import com.chrionline.chrionline.client.ui.views.AdminView;
import com.chrionline.chrionline.core.interfaces.ViewManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Map;

public class AdminSidebar extends VBox {

    public enum AdminPage {
        DASHBOARD, PRODUITS, CATEGORIES, COMMANDES, UTILISATEURS
    }
    private static final Color  C_BG           = Color.web("#6B3F20");
    private static final Color  C_ACTIVE       = Color.web("#8B5A2B");
    private static final Color  C_HOVER        = Color.web("#7A4C24");
    private static final Color  C_TEXT         = Color.WHITE;
    private static final Color  C_TEXT_MUTED   = Color.web("#D4A882");

    private static final String S_TEXT         = "#FFFFFF";
    private static final String S_TEXT_MUTED   = "#D4A882";
    private static final String S_ACTIVE       = "#8B5A2B";
    private static final String S_HOVER        = "#7A4C24";

    private final ViewManager viewManager;
    private final Map<String, Object> userData;
    private final AdminPage activePage;
    private final AdminView adminView;

    public AdminSidebar(AdminPage activePage, Map<String, Object> userData,
                        ViewManager viewManager, AdminView adminView) {
        this.activePage  = activePage;
        this.userData    = userData;
        this.viewManager = viewManager;
        this.adminView   = adminView;
        buildUI();
    }

    private void buildUI() {
        setPrefWidth(240);
        setMinWidth(220);
        setMaxWidth(260);
        VBox.setVgrow(this, Priority.ALWAYS);

        setBackground(new Background(new BackgroundFill(C_BG, CornerRadii.EMPTY, Insets.EMPTY)));
        setPadding(new Insets(28, 16, 28, 16));
        setSpacing(4);
        HBox logoBox = new HBox(12);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        logoBox.setPadding(new Insets(4, 8, 32, 8));

        FontIcon logoIcon = new FontIcon(Feather.SHOPPING_BAG);
        logoIcon.setIconSize(26);
        logoIcon.setIconColor(C_TEXT);

        Label logoText = new Label("ChriOnline");
        logoText.setStyle(
                "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + S_TEXT + ";" +
                        "-fx-font-family: 'Segoe UI Semibold', 'Segoe UI', 'Arial', sans-serif;"
        );
        logoBox.getChildren().addAll(logoIcon, logoText);
        HBox dashboardBtn    = createMenuItem(Feather.GRID,         "Dashboard",    AdminPage.DASHBOARD);
        HBox produitsBtn     = createMenuItem(Feather.PACKAGE,      "Produits",     AdminPage.PRODUITS);
        HBox categoriesBtn   = createMenuItem(Feather.FOLDER,       "Catégories",   AdminPage.CATEGORIES);
        HBox commandesBtn    = createMenuItem(Feather.SHOPPING_BAG, "Commandes",    AdminPage.COMMANDES);
        HBox utilisateursBtn = createMenuItem(Feather.USERS,        "Utilisateurs", AdminPage.UTILISATEURS);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                logoBox,
                dashboardBtn,
                produitsBtn,
                categoriesBtn,
                commandesBtn,
                utilisateursBtn,
                spacer
        );
    }

    private HBox createMenuItem(Feather icon, String label, AdminPage page) {
        boolean isActive = (page == activePage);

        HBox item = new HBox(14);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(13, 18, 13, 18));
        item.setMaxWidth(Double.MAX_VALUE);
        item.setCursor(Cursor.HAND);

        if (isActive) {
            item.setBackground(new Background(
                    new BackgroundFill(C_ACTIVE, new CornerRadii(10), Insets.EMPTY)
            ));
        }

        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(20);
        fontIcon.setIconColor(isActive ? C_TEXT : C_TEXT_MUTED);

        Label itemLabel = new Label(label);
        itemLabel.setTextFill(isActive ? C_TEXT : C_TEXT_MUTED);
        itemLabel.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-font-family: 'Segoe UI', 'Arial', sans-serif;" +
                        (isActive ? "-fx-font-weight: bold;" : "")
        );

        item.getChildren().addAll(fontIcon, itemLabel);

        if (!isActive) {
            item.setOnMouseEntered(e -> {
                item.setBackground(new Background(
                        new BackgroundFill(C_HOVER, new CornerRadii(10), Insets.EMPTY)
                ));
                fontIcon.setIconColor(C_TEXT);
                itemLabel.setTextFill(C_TEXT);
            });
            item.setOnMouseExited(e -> {
                item.setBackground(Background.EMPTY);
                fontIcon.setIconColor(C_TEXT_MUTED);
                itemLabel.setTextFill(C_TEXT_MUTED);
            });
        }

        item.setOnMouseClicked(e -> {
            switch (page) {
                case DASHBOARD    -> adminView.showDashboard();
                case PRODUITS     -> adminView.showProduits();
                case CATEGORIES   -> adminView.showCategories();
                case COMMANDES    -> adminView.showCommandes();
                case UTILISATEURS -> adminView.showUtilisateurs();
            }
        });

        return item;
    }
}