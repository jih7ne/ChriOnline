package com.chrionline.chrionline.core.theme;


import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;


public class AppTheme {

    private static boolean isDarkMode = false;

    // ===== LIGHT THEME =====
    public static class LightTheme {

        public static Color getBackground() {
            return AppColors.LIGHT_BACKGROUND;
        }

        public static Color getSurface() {
            return AppColors.LIGHT_SURFACE;
        }

        public static Color getSurfaceVariant() {
            return AppColors.LIGHT_SURFACE_VARIANT;
        }

        public static Color getTextPrimary() {
            return AppColors.TEXT_PRIMARY_LIGHT;
        }

        public static Color getTextSecondary() {
            return AppColors.TEXT_SECONDARY_LIGHT;
        }

        public static Color getTextTertiary() {
            return AppColors.TEXT_TERTIARY_LIGHT;
        }

        public static String getBackgroundStyle() {
            return "-fx-background-color: #FFFFFF;";
        }

        public static String getSurfaceStyle() {
            return "-fx-background-color: #F7F7F7;";
        }
    }

    // ===== DARK THEME =====
    public static class DarkTheme {

        public static Color getBackground() {
            return AppColors.DARK_BACKGROUND;
        }

        public static Color getSurface() {
            return AppColors.DARK_SURFACE;
        }

        public static Color getSurfaceVariant() {
            return AppColors.DARK_SURFACE_VARIANT;
        }

        public static Color getTextPrimary() {
            return AppColors.TEXT_PRIMARY_DARK;
        }

        public static Color getTextSecondary() {
            return AppColors.TEXT_SECONDARY_DARK;
        }

        public static Color getTextTertiary() {
            return AppColors.TEXT_TERTIARY_DARK;
        }

        public static String getBackgroundStyle() {
            return "-fx-background-color: #222222;";
        }

        public static String getSurfaceStyle() {
            return "-fx-background-color: #2C2C2C;";
        }
    }

    // ===== BUTTON STYLES =====
    public static void stylePrimaryButton(Button button) {
        button.setStyle(
                "-fx-background-color: #FF385C;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 16px 24px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-cursor: hand;"
        );
        button.setMaxWidth(Double.MAX_VALUE);
    }

    public static void styleOutlineButton(Button button) {
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #FF385C;" +
                        "-fx-border-width: 1.5px;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-text-fill: #FF385C;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 16px 24px;" +
                        "-fx-cursor: hand;"
        );
        button.setMaxWidth(Double.MAX_VALUE);
    }

    // ===== TEXT FIELD STYLES =====
    public static void styleTextField(TextField textField) {
        textField.setStyle(
                "-fx-background-color: " + (isDarkMode ? "#2C2C2C" : "#F7F7F7") + ";" +
                        "-fx-border-color: #DDDDDD;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-padding: 12px 16px;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: " + (isDarkMode ? "#FFFFFF" : "#222222") + ";"
        );
    }

    public static void styleFocusedTextField(TextField textField) {
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                textField.setStyle(
                        "-fx-background-color: " + (isDarkMode ? "#2C2C2C" : "#F7F7F7") + ";" +
                                "-fx-border-color: #FF385C;" +
                                "-fx-border-width: 2px;" +
                                "-fx-border-radius: 8px;" +
                                "-fx-background-radius: 8px;" +
                                "-fx-padding: 12px 16px;" +
                                "-fx-font-size: 14px;" +
                                "-fx-text-fill: " + (isDarkMode ? "#FFFFFF" : "#222222") + ";"
                );
            } else {
                styleTextField(textField);
            }
        });
    }

    // ===== CARD STYLES =====
    public static void styleCard(VBox card) {
        card.setStyle(
                "-fx-background-color: " + (isDarkMode ? "#2C2C2C" : "#FFFFFF") + ";" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-color: " + (isDarkMode ? "#DDDDDD" : "#EBEBEB") + ";" +
                        "-fx-border-radius: 12px;" +
                        "-fx-padding: 16px;"
        );

        // Add shadow effect
        card.setEffect(new javafx.scene.effect.DropShadow(5, 0, 2, Color.rgb(0, 0, 0, 0.1)));
    }

    // ===== CHIP STYLES =====
    public static void styleChip(Button chip) {
        chip.setStyle(
                "-fx-background-color: " + (isDarkMode ? "#383838" : "#EBEBEB") + ";" +
                        "-fx-background-radius: 4px;" +
                        "-fx-text-fill: " + (isDarkMode ? "#FFFFFF" : "#222222") + ";" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: 500;" +
                        "-fx-padding: 8px 12px;" +
                        "-fx-cursor: hand;"
        );
    }

    // ===== DIVIDER =====
    public static Separator createDivider() {
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: " + (isDarkMode ? "#DDDDDD" : "#EBEBEB") + ";");
        return separator;
    }

    // ===== TOGGLE THEME =====
    public static void toggleTheme() {
        isDarkMode = !isDarkMode;
    }

    public static boolean isDarkMode() {
        return isDarkMode;
    }

    // ===== THEME AWARE COLOR GETTERS =====
    public static Color getBackgroundColor() {
        return isDarkMode ? DarkTheme.getBackground() : LightTheme.getBackground();
    }

    public static Color getSurfaceColor() {
        return isDarkMode ? DarkTheme.getSurface() : LightTheme.getSurface();
    }

    public static Color getSurfaceVariantColor() {
        return isDarkMode ? DarkTheme.getSurfaceVariant() : LightTheme.getSurfaceVariant();
    }

    public static Color getTextPrimaryColor() {
        return isDarkMode ? DarkTheme.getTextPrimary() : LightTheme.getTextPrimary();
    }

    public static Color getTextSecondaryColor() {
        return isDarkMode ? DarkTheme.getTextSecondary() : LightTheme.getTextSecondary();
    }

    public static String getBackgroundStyle() {
        return isDarkMode ? DarkTheme.getBackgroundStyle() : LightTheme.getBackgroundStyle();
    }

    public static String getSurfaceStyle() {
        return isDarkMode ? DarkTheme.getSurfaceStyle() : LightTheme.getSurfaceStyle();
    }
}
