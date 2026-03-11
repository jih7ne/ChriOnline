package com.chrionline.chrionline.core.theme;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class AppTheme {


    public static final String BG             = "#EDE0D4";   // fond beige principal
    public static final String CARD_BG        = "#F5EDE3";   // fond carte
    public static final String PRIMARY        = "#7B5B3A";   // marron foncé (bouton, toggle actif)
    public static final String PRIMARY_LIGHT  = "#A07850";   // marron clair (hover)
    public static final String TOGGLE_INACTIVE= "#E8D5C0";   // toggle inactif
    public static final String FIELD_BG       = "#EAD8C5";   // fond champ
    public static final String FIELD_BORDER   = "#C8A882";   // bordure champ
    public static final String TEXT_MAIN      = "#3D2B1A";   // texte principal
    public static final String TEXT_MUTED     = "#9C7A5B";   // texte secondaire
    public static final String ERROR_COLOR    = "#C0392B";   // rouge erreur
    public static final String WHITE          = "#FFFFFF";

    private static boolean isDarkMode = false;

    // ─── BUTTON STYLES ─────────────────────────────────────────────────────

    public static void stylePrimaryButton(Button button) {
        button.setStyle(
                "-fx-background-color: " + PRIMARY + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 14px 24px;" +
                        "-fx-background-radius: 30px;" +
                        "-fx-cursor: hand;"
        );
        button.setMaxWidth(Double.MAX_VALUE);

        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: " + PRIMARY_LIGHT + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 14px 24px;" +
                        "-fx-background-radius: 30px;" +
                        "-fx-cursor: hand;"
        ));
        button.setOnMouseExited(e -> stylePrimaryButton(button));
    }

    public static void styleOutlineButton(Button button) {
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: " + PRIMARY + ";" +
                        "-fx-border-width: 1.5px;" +
                        "-fx-border-radius: 30px;" +
                        "-fx-text-fill: " + PRIMARY + ";" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12px 24px;" +
                        "-fx-cursor: hand;"
        );
        button.setMaxWidth(Double.MAX_VALUE);
    }

    // ─── TOGGLE BUTTON STYLES ──────────────────────────────────────────────

    public static void styleToggleActive(Button btn) {
        btn.setStyle(
                "-fx-background-color: " + PRIMARY + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 10px 24px;" +
                        "-fx-cursor: hand;"
        );
    }

    public static void styleToggleInactive(Button btn) {
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 10px 24px;" +
                        "-fx-cursor: hand;"
        );
    }

    // ─── TEXT FIELD STYLES ─────────────────────────────────────────────────

    public static void styleTextField(TextField f) {
        f.setStyle(
                "-fx-background-color: " + FIELD_BG + ";" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-radius: 30px;" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 12px 16px 12px 40px;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: " + TEXT_MAIN + ";" +
                        "-fx-prompt-text-fill: " + TEXT_MUTED + ";"
        );
    }

    public static void styleTextField(PasswordField f) {
        f.setStyle(
                "-fx-background-color: " + FIELD_BG + ";" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-radius: 30px;" +
                        "-fx-background-radius: 30px;" +
                        "-fx-padding: 12px 16px 12px 40px;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: " + TEXT_MAIN + ";" +
                        "-fx-prompt-text-fill: " + TEXT_MUTED + ";"
        );
    }

    public static void styleFocusedTextField(TextField f) {
        f.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) {
                f.setStyle(
                        "-fx-background-color: " + FIELD_BG + ";" +
                                "-fx-border-color: " + PRIMARY + ";" +
                                "-fx-border-width: 1.5px;" +
                                "-fx-border-radius: 30px;" +
                                "-fx-background-radius: 30px;" +
                                "-fx-padding: 12px 16px 12px 40px;" +
                                "-fx-font-size: 14px;" +
                                "-fx-text-fill: " + TEXT_MAIN + ";"
                );
            } else {
                styleTextField(f);
            }
        });
    }

    public static void styleFocusedTextField(PasswordField f) {
        f.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) {
                f.setStyle(
                        "-fx-background-color: " + FIELD_BG + ";" +
                                "-fx-border-color: " + PRIMARY + ";" +
                                "-fx-border-width: 1.5px;" +
                                "-fx-border-radius: 30px;" +
                                "-fx-background-radius: 30px;" +
                                "-fx-padding: 12px 16px 12px 40px;" +
                                "-fx-font-size: 14px;" +
                                "-fx-text-fill: " + TEXT_MAIN + ";"
                );
            } else {
                styleTextField(f);
            }
        });
    }

    // ─── CARD STYLES ───────────────────────────────────────────────────────

    public static void styleCard(VBox card) {
        card.setStyle(
                "-fx-background-color: " + CARD_BG + ";" +
                        "-fx-background-radius: 24px;" +
                        "-fx-padding: 32px;"
        );
        card.setEffect(new javafx.scene.effect.DropShadow(30, 0, 8, Color.rgb(0, 0, 0, 0.12)));
    }

    // ─── MISC ──────────────────────────────────────────────────────────────

    public static Separator createDivider() {
        Separator s = new Separator();
        s.setStyle("-fx-background-color: " + FIELD_BORDER + ";");
        return s;
    }

    public static boolean isDarkMode() { return isDarkMode; }
    public static void toggleTheme() { isDarkMode = !isDarkMode; }

    // Legacy support
    public static class LightTheme {
        public static String getBackgroundStyle() { return "-fx-background-color: " + BG + ";"; }
    }
    public static class DarkTheme {
        public static String getBackgroundStyle() { return "-fx-background-color: #2C2C2C;"; }
    }
    public static String getBackgroundStyle() {
        return isDarkMode ? DarkTheme.getBackgroundStyle() : LightTheme.getBackgroundStyle();
    }
}