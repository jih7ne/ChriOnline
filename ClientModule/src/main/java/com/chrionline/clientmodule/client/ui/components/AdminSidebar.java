package com.chrionline.clientmodule.client.ui.components;

import com.chrionline.clientmodule.client.ui.views.AdminView;
import com.chrionline.core.interfaces.ViewManager;
import com.chrionline.core.theme.AppTheme;
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
        DASHBOARD, PRODUITS, CATEGORIES, COMMANDES, UTILISATEURS, SETTINGS
    }

    private static final Color  C_BG         = Color.web(AppTheme.PRIMARY);
    private static final Color  C_ACTIVE     = Color.web("#5A3E26");
    private static final Color  C_HOVER      = Color.web(AppTheme.PRIMARY_LIGHT);
    private static final Color  C_TEXT       = Color.WHITE;
    private static final Color  C_TEXT_MUTED = Color.web("#D4A882");

    private final ViewManager         viewManager;
    private final Map<String, Object> userData;
    private final AdminPage           activePage;
    private final AdminView           adminView;

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
        setMinWidth(240);
        setMaxWidth(240);
        setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(this, Priority.ALWAYS);

        setBackground(new Background(new BackgroundFill(C_BG, CornerRadii.EMPTY, Insets.EMPTY)));
        setPadding(new Insets(28, 16, 28, 16));
        setSpacing(4);

        // Logo
        HBox logoBox = new HBox(12);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        logoBox.setPadding(new Insets(0, 8, 28, 8));

        FontIcon logoIcon = new FontIcon(Feather.SHOPPING_BAG);
        logoIcon.setIconSize(26);
        logoIcon.setIconColor(C_TEXT);

        Label logoText = new Label("ChriOnline");
        logoText.setStyle(
                "-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#FFFFFF;" +
                        "-fx-font-family:'Segoe UI Semibold','Segoe UI','Arial',sans-serif;"
        );
        logoBox.getChildren().addAll(logoIcon, logoText);

        // Nav items
        HBox dashboardBtn    = createMenuItem(Feather.GRID,         "Dashboard",    AdminPage.DASHBOARD);
        HBox produitsBtn     = createMenuItem(Feather.PACKAGE,      "Produits",     AdminPage.PRODUITS);
        HBox categoriesBtn   = createMenuItem(Feather.FOLDER,       "Catégories",   AdminPage.CATEGORIES);
        HBox commandesBtn    = createMenuItem(Feather.SHOPPING_BAG, "Commandes",    AdminPage.COMMANDES);
        HBox utilisateursBtn = createMenuItem(Feather.USERS,        "Utilisateurs", AdminPage.UTILISATEURS);

        // Spacer pushes settings to the bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox settingsBtn = createMenuItem(Feather.KEY, "Clés RSA", AdminPage.SETTINGS);

        getChildren().addAll(
                logoBox,
                dashboardBtn,
                produitsBtn,
                categoriesBtn,
                commandesBtn,
                utilisateursBtn,
                spacer,
                settingsBtn
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
                "-fx-font-size:15px;" +
                        "-fx-font-family:'Segoe UI','Arial',sans-serif;" +
                        (isActive ? "-fx-font-weight:bold;" : "")
        );
        HBox.setHgrow(itemLabel, Priority.ALWAYS);

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
                case SETTINGS     -> adminView.showSettings();
            }
        });

        return item;
    }
}