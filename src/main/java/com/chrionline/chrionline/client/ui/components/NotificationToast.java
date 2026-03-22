package com.chrionline.chrionline.client.ui.components;

import com.chrionline.chrionline.core.theme.AppTheme;
import com.chrionline.chrionline.network.protocol.AppNotification;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

public class NotificationToast {

    private static final int    DISPLAY_MS  = 4500;
    private static final double TOAST_WIDTH = 300;

    public static void show(javafx.scene.Parent root, AppNotification notification) {
        if (!(root instanceof StackPane stackRoot)) return;

        // Parse "title | message" format
        String raw     = notification.getMessage() != null ? notification.getMessage() : "";
        String title   = raw.contains(" | ") ? raw.substring(0, raw.indexOf(" | "))  : raw;
        String message = raw.contains(" | ") ? raw.substring(raw.indexOf(" | ") + 3) : "";

        // Colors
        String[] colors = resolveColors(notification);
        String bgColor     = colors[0];
        String borderColor = colors[1];
        String titleColor  = colors[2];
        String iconColor   = colors[3];

        // Icon
        FontIcon icon = new FontIcon(resolveIcon(notification));
        icon.setIconSize(14);
        icon.setIconColor(Color.web(iconColor));

        // Title
        Label titleLbl = new Label(title);
        titleLbl.setStyle(
                "-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-text-fill:" + titleColor + ";" +
                        "-fx-wrap-text:true;"
        );
        titleLbl.setMaxWidth(TOAST_WIDTH - 70);

        // Message (optional)
        Label messageLbl = new Label(message);
        messageLbl.setStyle(
                "-fx-font-size:11px;" +
                        "-fx-text-fill:" + titleColor + "99;" + // slightly transparent
                        "-fx-wrap-text:true;"
        );
        messageLbl.setMaxWidth(TOAST_WIDTH - 70);
        messageLbl.setVisible(!message.isEmpty());
        messageLbl.setManaged(!message.isEmpty());

        VBox textBox = new VBox(2, titleLbl, messageLbl);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        // Close ×
        Label closeBtn = new Label("✕");
        closeBtn.setStyle(
                "-fx-font-size:10px;" +
                        "-fx-text-fill:" + titleColor + "88;" +
                        "-fx-cursor:hand;" +
                        "-fx-padding:0 0 0 4;"
        );

        // Toast card
        HBox toast = new HBox(8, icon, textBox, closeBtn);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setPadding(new Insets(10, 12, 10, 12));
        toast.setPrefWidth(TOAST_WIDTH);
        toast.setMaxWidth(TOAST_WIDTH);
        toast.setStyle(
                "-fx-background-color:" + bgColor + ";" +
                        "-fx-background-radius:8;" +
                        "-fx-border-color:" + borderColor + ";" +
                        "-fx-border-radius:8;" +
                        "-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),8,0,0,3);"
        );

        // Position top-right
        StackPane.setAlignment(toast, Pos.TOP_RIGHT);
        StackPane.setMargin(toast, new Insets(12, 12, 0, 0));

        stackRoot.getChildren().add(toast);

        // Animate in
        toast.setOpacity(0);
        toast.setTranslateX(40);
        FadeTransition fi = new FadeTransition(Duration.millis(200), toast);
        fi.setFromValue(0); fi.setToValue(1); fi.play();
        TranslateTransition si = new TranslateTransition(Duration.millis(200), toast);
        si.setFromX(40); si.setToX(0); si.play();

        // Dismiss
        Runnable dismiss = () -> {
            FadeTransition fo = new FadeTransition(Duration.millis(200), toast);
            fo.setFromValue(1); fo.setToValue(0);
            fo.setOnFinished(e -> stackRoot.getChildren().remove(toast));
            fo.play();
        };

        closeBtn.setOnMouseClicked(e -> dismiss.run());
        toast.setOnMouseClicked(e -> dismiss.run());

        new Thread(() -> {
            try { Thread.sleep(DISPLAY_MS); } catch (InterruptedException ignored) {}
            Platform.runLater(dismiss);
        }).start();
    }

    private static String[] resolveColors(AppNotification n) {
        if (n.getSeverity() == null) return successColors();
        return switch (n.getSeverity()) {
            case ERROR, CRITICAL -> new String[]{ "#FEF2F2", "#FCA5A5", "#B91C1C", "#B91C1C" };
            case WARNING         -> new String[]{ "#FFFBEB", "#FDE68A", "#92400E", "#D97706" };
            default              -> successColors();
        };
    }

    private static String[] successColors() {
        return new String[]{ "#F0FDF4", "#86EFAC", "#166534", "#16a34a" };
    }

    private static Feather resolveIcon(AppNotification n) {
        if (n.getSeverity() == null) return Feather.BELL;
        return switch (n.getSeverity()) {
            case ERROR, CRITICAL -> Feather.ALERT_CIRCLE;
            case WARNING         -> Feather.ALERT_TRIANGLE;
            default              -> Feather.BELL;
        };
    }
}