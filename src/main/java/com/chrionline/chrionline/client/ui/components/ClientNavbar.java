package com.chrionline.chrionline.client.ui.components;

import com.chrionline.chrionline.core.interfaces.ViewManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Map;
import java.util.function.Consumer;

public class ClientNavbar extends HBox {

    private Label cartCountLabel;

    public ClientNavbar(int cartCount,
                        Map<String, Object> userData,
                        ViewManager viewManager,
                        Consumer<String> onSearch) {

        setStyle(
                "-fx-background-color: #EDE0D4;" +
                        "-fx-border-color: #DDB892;" +
                        "-fx-border-width: 0 0 1 0;"
        );
        setPadding(new Insets(12, 32, 12, 60));
        setAlignment(Pos.CENTER_LEFT);

        // LOGO
        HBox logoBox = new HBox(8);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        logoBox.setPrefWidth(220);
        logoBox.setMinWidth(220);

        FontIcon logoIcon = new FontIcon(Feather.SHOPPING_CART);
        logoIcon.setIconSize(22);
        logoIcon.setIconColor(Color.web("#7F5539"));

        Label logoText = new Label("ChriOnline");
        logoText.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #7F5539;"
        );
        logoBox.getChildren().addAll(logoIcon, logoText);

        // CENTRE — recherche
        HBox centerBox = new HBox();
        centerBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(centerBox, Priority.ALWAYS);

        StackPane searchContainer = new StackPane();
        searchContainer.setPrefWidth(440);
        searchContainer.setMaxWidth(440);

        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher des produits...");
        searchField.setMaxWidth(Double.MAX_VALUE);
        searchField.setStyle(
                "-fx-background-color: #E6CCB2;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-radius: 20;" +
                        "-fx-padding: 9 16 9 38;" +
                        "-fx-font-size: 13px;" +
                        "-fx-text-fill: #3B1F0E;" +
                        "-fx-prompt-text-fill: #B08968;"
        );
        searchField.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) {
                searchField.setStyle(
                        "-fx-background-color: #E6CCB2;" +
                                "-fx-background-radius: 20;" +
                                "-fx-border-color: #7F5539;" +
                                "-fx-border-radius: 20;" +
                                "-fx-border-width: 1.5;" +
                                "-fx-padding: 9 16 9 38;" +
                                "-fx-font-size: 13px;" +
                                "-fx-text-fill: #3B1F0E;"
                );
            } else {
                searchField.setStyle(
                        "-fx-background-color: #E6CCB2;" +
                                "-fx-background-radius: 20;" +
                                "-fx-border-color: transparent;" +
                                "-fx-border-radius: 20;" +
                                "-fx-padding: 9 16 9 38;" +
                                "-fx-font-size: 13px;" +
                                "-fx-text-fill: #3B1F0E;" +
                                "-fx-prompt-text-fill: #B08968;"
                );
            }
        });

        if (onSearch != null) {
            searchField.textProperty().addListener((obs, old, val) -> onSearch.accept(val));
        }

        FontIcon searchIcon = new FontIcon(Feather.SEARCH);
        searchIcon.setIconSize(14);
        searchIcon.setIconColor(Color.web("#B08968"));
        StackPane.setAlignment(searchIcon, Pos.CENTER_LEFT);
        searchIcon.setTranslateX(14);

        searchContainer.getChildren().addAll(searchField, searchIcon);
        centerBox.getChildren().add(searchContainer);

        HBox iconsBox = new HBox(6);
        iconsBox.setAlignment(Pos.CENTER_RIGHT);
        iconsBox.setPrefWidth(180);
        iconsBox.setMinWidth(180);
        HBox.setMargin(iconsBox, new Insets(0, 43, 0, 0));

        StackPane panierBtn = createIconButton(Feather.SHOPPING_CART);

        cartCountLabel = new Label(String.valueOf(cartCount));
        cartCountLabel.setStyle(
                "-fx-background-color: #7F5539;" +
                        "-fx-text-fill: #EDE0D4;" +
                        "-fx-font-size: 9px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-min-width: 16;" +
                        "-fx-min-height: 16;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 1 3 1 3;"
        );
        cartCountLabel.setVisible(cartCount > 0);
        StackPane.setAlignment(cartCountLabel, Pos.TOP_RIGHT);
        cartCountLabel.setTranslateX(6);
        cartCountLabel.setTranslateY(-6);
        panierBtn.getChildren().add(cartCountLabel);
        panierBtn.setOnMouseClicked(e -> viewManager.showPanierView(userData));

        StackPane commandesBtn = createIconButton(Feather.PACKAGE);
        StackPane compteBtn = createIconButton(Feather.USER);

        iconsBox.getChildren().addAll(panierBtn, commandesBtn, compteBtn);

        getChildren().addAll(logoBox, centerBox, iconsBox);

        Platform.runLater(() -> {
            logoBox.requestFocus();
        });
    }

    private StackPane createIconButton(Ikon icon) {
        StackPane btn = new StackPane();
        btn.setPrefSize(38, 38);
        btn.setMinSize(38, 38);
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(21);
        fontIcon.setIconColor(Color.web("#7F5539"));
        btn.getChildren().add(fontIcon);
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #E6CCB2;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        ));
        return btn;
    }

    public void updateCartCount(int count) {
        cartCountLabel.setText(String.valueOf(count));
        cartCountLabel.setVisible(count > 0);
    }
}