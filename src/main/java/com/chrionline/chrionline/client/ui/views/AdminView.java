package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.client.ui.components.AdminSidebar;
import com.chrionline.chrionline.core.interfaces.ViewManager;
import com.chrionline.chrionline.core.theme.AppTheme;
import com.chrionline.chrionline.network.tcp.TCPClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Map;

public class AdminView extends BorderPane {

    private final TCPClient           client;
    private final Map<String, Object> userData;
    private final ViewManager         viewManager;

    private AdminSidebar.AdminPage currentPage = AdminSidebar.AdminPage.DASHBOARD;
    private final BorderPane       rightPane   = new BorderPane();

    public AdminView(TCPClient client, Map<String, Object> userData, ViewManager viewManager) {
        this.client      = client;
        this.userData    = userData;
        this.viewManager = viewManager;
        buildUI();
    }

    private void buildUI() {
        setBackground(new Background(new BackgroundFill(
                Color.web(AppTheme.BG), CornerRadii.EMPTY, Insets.EMPTY)));
        rightPane.setBackground(new Background(new BackgroundFill(
                Color.web(AppTheme.BG), CornerRadii.EMPTY, Insets.EMPTY)));
        rightPane.setTop(buildTopBar());
        HBox.setHgrow(rightPane, Priority.ALWAYS);
        VBox.setVgrow(rightPane, Priority.ALWAYS);
        rebuildSidebar();
        setCenter(rightPane);
        showDashboard();
    }

    private void rebuildSidebar() {
        setLeft(new AdminSidebar(currentPage, userData, viewManager, this));
    }

    private HBox buildTopBar() {
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(0, 24, 0, 24));
        topBar.setMinHeight(60);
        topBar.setPrefHeight(60);
        topBar.setMaxHeight(60);
        topBar.setMaxWidth(Double.MAX_VALUE);
        topBar.setBackground(new Background(new BackgroundFill(
                Color.web(AppTheme.BG), CornerRadii.EMPTY, Insets.EMPTY)));
        topBar.setStyle("-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-width:0 0 1 0;");

        String adminName = (userData != null && userData.containsKey("nom"))
                ? "Admin " + userData.get("nom") : "Admin";

        Label nameLabel = new Label(adminName);
        nameLabel.setStyle(
                "-fx-font-size:15px;-fx-font-weight:600;" +
                        "-fx-text-fill:" + AppTheme.TEXT_MAIN + ";" +
                        "-fx-font-family:'Segoe UI','Arial',sans-serif;"
        );

        FontIcon userIcon = new FontIcon(Feather.USER);
        userIcon.setIconSize(19);
        userIcon.setIconColor(Color.WHITE);
        StackPane avatar = new StackPane(userIcon);
        avatar.setPrefSize(38, 38); avatar.setMinSize(38, 38); avatar.setMaxSize(38, 38);
        avatar.setCursor(Cursor.HAND);
        avatar.setBackground(new Background(new BackgroundFill(
                Color.web(AppTheme.PRIMARY), new CornerRadii(50), Insets.EMPTY)));
        avatar.setOnMouseEntered(e -> avatar.setBackground(new Background(new BackgroundFill(
                Color.web(AppTheme.PRIMARY_LIGHT), new CornerRadii(50), Insets.EMPTY))));
        avatar.setOnMouseExited(e -> avatar.setBackground(new Background(new BackgroundFill(
                Color.web(AppTheme.PRIMARY), new CornerRadii(50), Insets.EMPTY))));
        Tooltip.install(avatar, new Tooltip("Mon profil"));

        FontIcon logoutIcon = new FontIcon(Feather.LOG_OUT);
        logoutIcon.setIconSize(19);
        logoutIcon.setIconColor(Color.web(AppTheme.ERROR_COLOR));
        StackPane logoutBtn = new StackPane(logoutIcon);
        logoutBtn.setPrefSize(38, 38); logoutBtn.setMinSize(38, 38); logoutBtn.setMaxSize(38, 38);
        logoutBtn.setCursor(Cursor.HAND);
        logoutBtn.setStyle("-fx-background-color:transparent;-fx-background-radius:8;");
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle(
                "-fx-background-color:#FEF2F2;-fx-background-radius:8;"));
        logoutBtn.setOnMouseExited(e -> logoutBtn.setStyle(
                "-fx-background-color:transparent;-fx-background-radius:8;"));
        logoutBtn.setOnMouseClicked(e ->
                new Thread(() -> Platform.runLater(() -> viewManager.showLoginView())).start());
        Tooltip.install(logoutBtn, new Tooltip("Se déconnecter"));

        HBox profileBox = new HBox(10, nameLabel, avatar, logoutBtn);
        profileBox.setAlignment(Pos.CENTER);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(spacer, profileBox);
        return topBar;
    }

    // ─── Page navigation ──────────────────────────────────────────────────────

    public void showDashboard() {
        currentPage = AdminSidebar.AdminPage.DASHBOARD;
        rebuildSidebar();
        rightPane.setCenter(buildComingSoon("Dashboard"));
    }

    public void showProduits() {
        currentPage = AdminSidebar.AdminPage.PRODUITS;
        rebuildSidebar();
        rightPane.setCenter(new AdminProduitsView(client, userData, viewManager, this));
    }

    public void showCategories() {
        currentPage = AdminSidebar.AdminPage.CATEGORIES;
        rebuildSidebar();
        rightPane.setCenter(new AdminCategoriesView(client, userData, viewManager, this));
    }

    public void showCommandes() {
        currentPage = AdminSidebar.AdminPage.COMMANDES;
        rebuildSidebar();
        rightPane.setCenter(buildComingSoon("Commandes"));
    }

    public void showUtilisateurs() {
        currentPage = AdminSidebar.AdminPage.UTILISATEURS;
        rebuildSidebar();
        rightPane.setCenter(new AdminUtilisateursView(client, userData, viewManager, this));
    }

    // ─── Coming soon placeholder ──────────────────────────────────────────────

    private Region buildComingSoon(String pageName) {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(box, Priority.ALWAYS);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setBackground(new Background(new BackgroundFill(
                Color.web(AppTheme.BG), CornerRadii.EMPTY, Insets.EMPTY)));
        Label label = new Label(pageName + " — Bientôt disponible");
        label.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:" + AppTheme.PRIMARY + ";");
        box.getChildren().add(label);
        return box;
    }
}