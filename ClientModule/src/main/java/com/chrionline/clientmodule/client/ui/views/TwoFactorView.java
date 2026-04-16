package com.chrionline.clientmodule.client.ui.views;

import com.chrionline.core.theme.AppTheme;
import com.chrionline.core.utils.JsonUtils;
import com.chrionline.network.protocol.AppRequest;
import com.chrionline.network.protocol.AppResponse;
import com.chrionline.network.tcp.TCPClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TwoFactorView extends StackPane {

    private final TextField                     codeField;
    private final Button                        verifyButton;
    private final Label                         errorLabel;
    private final String                        tempToken;
    private final TCPClient                     tcpClient;
    private final Consumer<Map<String, Object>> onSuccess;
    private final Runnable                      onCancel;

    public TwoFactorView(TCPClient tcpClient,
                         String tempToken,
                         Consumer<Map<String, Object>> onSuccess,
                         Runnable onCancel) {
        this.tcpClient  = tcpClient;
        this.tempToken  = tempToken;
        this.onSuccess  = onSuccess;
        this.onCancel   = onCancel;

        this.setStyle("-fx-background-color: " + AppTheme.BG + ";");

        VBox card = new VBox(0);
        card.setMaxWidth(420);
        AppTheme.styleCard(card);
        card.setPadding(new Insets(40));

        // Icon + titre
        Label icon = new Label("🔐");
        icon.setStyle("-fx-font-size: 40px;");
        VBox iconBox = new VBox(icon);
        iconBox.setAlignment(Pos.CENTER);
        VBox.setMargin(iconBox, new Insets(0, 0, 12, 0));

        Label title = new Label("Vérification en 2 étapes");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: "
                + AppTheme.TEXT_MAIN + ";");
        Label subtitle = new Label("Ouvrez Google Authenticator et saisissez le code à 6 chiffres.");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: " + AppTheme.TEXT_MUTED
                + "; -fx-wrap-text: true;");
        subtitle.setWrapText(true);
        VBox titleBox = new VBox(6, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);
        VBox.setMargin(titleBox, new Insets(0, 0, 24, 0));

        // Champ code
        codeField = new TextField();
        codeField.setPromptText("000000");
        codeField.setMaxWidth(200);
        codeField.setStyle(
                "-fx-font-size: 28px; -fx-font-weight: bold;"
                        + "-fx-alignment: center; -fx-letter-spacing: 8px;"
                        + "-fx-background-color: " + AppTheme.FIELD_BG + ";"
                        + "-fx-border-color: " + AppTheme.FIELD_BORDER + ";"
                        + "-fx-border-radius: 12px; -fx-background-radius: 12px;"
                        + "-fx-padding: 12px;"
        );
        // Limiter à 6 chiffres
        codeField.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) codeField.setText(n.replaceAll("[^\\d]", ""));
            if (n.length() > 6)    codeField.setText(n.substring(0, 6));
        });
        codeField.setOnAction(e -> handleVerify());

        HBox codeBox = new HBox(codeField);
        codeBox.setAlignment(Pos.CENTER);
        VBox.setMargin(codeBox, new Insets(0, 0, 16, 0));

        // Erreur
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: " + AppTheme.ERROR_COLOR
                + "; -fx-font-size: 13px;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        VBox.setMargin(errorLabel, new Insets(0, 0, 12, 0));

        // Bouton vérifier
        verifyButton = new Button("Vérifier");
        AppTheme.stylePrimaryButton(verifyButton);
        verifyButton.setOnAction(e -> handleVerify());

        // Lien annuler
        Hyperlink cancel = new Hyperlink("← Retour à la connexion");
        cancel.setStyle("-fx-text-fill: " + AppTheme.TEXT_MUTED
                + "; -fx-font-size: 12px; -fx-border-color: transparent;");
        cancel.setOnAction(e -> onCancel.run());
        HBox cancelBox = new HBox(cancel);
        cancelBox.setAlignment(Pos.CENTER);
        VBox.setMargin(cancelBox, new Insets(8, 0, 0, 0));

        card.getChildren().addAll(
                iconBox, titleBox, codeBox,
                errorLabel, verifyButton, cancelBox
        );

        StackPane.setAlignment(card, Pos.CENTER);
        this.getChildren().add(card);
    }

    private void handleVerify() {
        String code = codeField.getText().trim();
        if (code.length() != 6) {
            showError("Le code doit contenir 6 chiffres.");
            return;
        }

        verifyButton.setDisable(true);
        verifyButton.setText("Vérification...");
        errorLabel.setVisible(false);

        new Thread(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("tempToken", tempToken);
                payload.put("code",      code);

                AppRequest request = new AppRequest.Builder()
                        .controller("Auth").action("twofactorverify")
                        .payload(JsonUtils.toJson(payload))
                        .build();

                AppResponse response = tcpClient.sendAndParse(request);

                Platform.runLater(() -> {
                    verifyButton.setDisable(false);
                    verifyButton.setText("Vérifier");
                    if (response != null && response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = response.getDataAs(Map.class);
                        if (data != null) onSuccess.accept(data);
                    } else {
                        showError(response != null ? response.getMessage()
                                : "Code invalide. Réessayez.");
                        codeField.clear();
                        codeField.requestFocus();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    verifyButton.setDisable(false);
                    verifyButton.setText("Vérifier");
                    showError("Erreur réseau : " + e.getMessage());
                });
            }
        }).start();
    }

    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
}