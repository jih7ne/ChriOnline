package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.client.ui.components.AdminSidebar;
import com.chrionline.chrionline.core.interfaces.ViewManager;
import com.chrionline.chrionline.network.tcp.TCPClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Map;

public class AdminView extends BorderPane {
    private static final Color  C_BG          = Color.web("#EDE0D4");
    private static final Color  C_TOPBAR      = Color.web("#EDE0D4");
    private static final Color  C_AVATAR_BG   = Color.web("#6B3F20");
    private static final String S_TEXT_DARK   = "#3D2314";
    private static final String S_BORDER      = "#D4C4B0";

    private final TCPClient client;
    private final Map<String, Object> userData;
    private final ViewManager viewManager;

    private AdminSidebar.AdminPage currentPage = AdminSidebar.AdminPage.DASHBOARD;
    private final BorderPane rightPane = new BorderPane();

    public AdminView(TCPClient client, Map<String, Object> userData, ViewManager viewManager) {
        this.client      = client;
        this.userData    = userData;
        this.viewManager = viewManager;
        buildUI();
    }

    private void buildUI() {
        setBackground(new Background(new BackgroundFill(C_BG, CornerRadii.EMPTY, Insets.EMPTY)));

        rightPane.setBackground(new Background(new BackgroundFill(C_BG, CornerRadii.EMPTY, Insets.EMPTY)));
        rightPane.setTop(buildTopBar());
        rebuildSidebar();
        setCenter(rightPane);

        showDashboard();
    }

    private void rebuildSidebar() {
        AdminSidebar sidebar = new AdminSidebar(currentPage, userData, viewManager, this);
        setLeft(sidebar);
    }
    private HBox buildTopBar() {
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(12, 24, 12, 24));
        topBar.setMinHeight(60);
        topBar.setBackground(new Background(new BackgroundFill(C_TOPBAR, CornerRadii.EMPTY, Insets.EMPTY)));
        topBar.setStyle("-fx-border-color: " + S_BORDER + "; -fx-border-width: 0 0 1 0;");

        String adminName = (userData != null && userData.containsKey("nom"))
                ? "Admin " + userData.get("nom")
                : "Admin";

        Label nameLabel = new Label(adminName);
        nameLabel.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-font-weight: 600;" +
                        "-fx-text-fill: " + S_TEXT_DARK + ";" +
                        "-fx-font-family: 'Segoe UI', 'Arial', sans-serif;"
        );
        FontIcon userIcon = new FontIcon(Feather.USER);
        userIcon.setIconSize(20);
        userIcon.setIconColor(Color.WHITE);

        StackPane avatar = new StackPane(userIcon);
        avatar.setPrefSize(40, 40);
        avatar.setMinSize(40, 40);
        avatar.setMaxSize(40, 40);
        avatar.setCursor(Cursor.HAND);
        avatar.setBackground(new Background(
                new BackgroundFill(C_AVATAR_BG, new CornerRadii(50), Insets.EMPTY)
        ));

        Popup popup = buildLogoutPopup();
        avatar.setOnMouseClicked(e -> {
            if (popup.isShowing()) {
                popup.hide();
            } else {
                javafx.geometry.Bounds b = avatar.localToScreen(avatar.getBoundsInLocal());
                popup.show(avatar, b.getMinX() - 110, b.getMaxY() + 6);
            }
        });

        avatar.setOnMouseEntered(e -> avatar.setBackground(new Background(
                new BackgroundFill(Color.web("#5A3318"), new CornerRadii(50), Insets.EMPTY)
        )));
        avatar.setOnMouseExited(e -> avatar.setBackground(new Background(
                new BackgroundFill(C_AVATAR_BG, new CornerRadii(50), Insets.EMPTY)
        )));

        HBox profileBox = new HBox(12, nameLabel, avatar);
        profileBox.setAlignment(Pos.CENTER);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(spacer, profileBox);
        return topBar;
    }

    private Popup buildLogoutPopup() {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox card = new VBox(0);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(10), Insets.EMPTY)));
        card.setStyle(
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 16, 0, 0, 4);" +
                        "-fx-border-color: #E8D5C5;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;" +
                        "-fx-min-width: 160px;"
        );

        HBox logoutBtn = new HBox(10);
        logoutBtn.setAlignment(Pos.CENTER_LEFT);
        logoutBtn.setPadding(new Insets(14, 20, 14, 20));
        logoutBtn.setCursor(Cursor.HAND);

        FontIcon logoutIcon = new FontIcon(Feather.LOG_OUT);
        logoutIcon.setIconSize(15);
        logoutIcon.setIconColor(Color.web("#C0392B"));

        Label logoutLabel = new Label("Déconnexion");
        logoutLabel.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-text-fill: #C0392B;" +
                        "-fx-font-family: 'Segoe UI', 'Arial', sans-serif;"
        );

        logoutBtn.getChildren().addAll(logoutIcon, logoutLabel);

        logoutBtn.setOnMouseEntered(e ->
                logoutBtn.setBackground(new Background(
                        new BackgroundFill(Color.web("#FFF0EE"), new CornerRadii(8), Insets.EMPTY)
                ))
        );
        logoutBtn.setOnMouseExited(e -> logoutBtn.setBackground(Background.EMPTY));
        logoutBtn.setOnMouseClicked(e -> {
            popup.hide();
            viewManager.showLoginView();
        });

        card.getChildren().add(logoutBtn);
        popup.getContent().add(card);
        return popup;
    }
    public void showDashboard() {
        currentPage = AdminSidebar.AdminPage.DASHBOARD;
        rebuildSidebar();
        rightPane.setCenter(buildComingSoon("Dashboard"));
    }

    public void showProduits() {
        currentPage = AdminSidebar.AdminPage.PRODUITS;
        rebuildSidebar();
        AdminProduitsView produitsView = new AdminProduitsView(client, userData, viewManager, this);
        rightPane.setCenter(produitsView);
    }

    public void showCategories() {
        currentPage = AdminSidebar.AdminPage.CATEGORIES;
        rebuildSidebar();
        rightPane.setCenter(buildComingSoon("Catégories"));
    }

    public void showCommandes() {
        currentPage = AdminSidebar.AdminPage.COMMANDES;
        rebuildSidebar();
        rightPane.setCenter(buildComingSoon("Commandes"));
    }

    public void showUtilisateurs() {
        currentPage = AdminSidebar.AdminPage.UTILISATEURS;
        rebuildSidebar();
        rightPane.setCenter(buildComingSoon("Utilisateurs"));
    }

    private Region buildComingSoon(String pageName) {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setBackground(new Background(new BackgroundFill(C_BG, CornerRadii.EMPTY, Insets.EMPTY)));
        Label label = new Label(pageName + " — Bientôt disponible");
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #8B5A2B;");
        box.getChildren().add(label);
        return box;
    }
}