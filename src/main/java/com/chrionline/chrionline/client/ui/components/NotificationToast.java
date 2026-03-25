package com.chrionline.chrionline.client.ui.components;

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

/**
 * Notification toast aligned with the ChriOnline design palette.
 *
 * Type        → BG / Border / Text         Icon
 * SUCCESS     → cream / brown-light / brown  BELL
 * WARNING     → warm-yellow / amber / amber  ALERT_TRIANGLE
 * ERROR       → soft-red / red / dark-red    ALERT_CIRCLE
 * INFO        → light-brown / brown / brown  INFO
 */
public class NotificationToast {

    // App palette
    private static final String APP_BROWN       = "#7F5539";
    private static final String APP_BROWN_DARK  = "#5C3D2E";
    private static final String APP_BROWN_LIGHT = "#C4A882";
    private static final String APP_BG_CARD     = "#FAF3EC";

    private static final int    DISPLAY_MS  = 4000;
    private static final double TOAST_WIDTH = 240;

    public static void show(javafx.scene.Parent root, AppNotification notification) {
        if (!(root instanceof StackPane stackRoot)) return;

        Platform.runLater(() -> buildAndShow(stackRoot, notification));
    }

    private static void buildAndShow(StackPane stackRoot, AppNotification notification) {
        // ── Parse message ──────────────────────────────────────────────────────
        String raw     = notification.getMessage() != null ? notification.getMessage() : "";
        String title   = raw.contains(" | ") ? raw.substring(0, raw.indexOf(" | "))  : raw;
        String message = raw.contains(" | ") ? raw.substring(raw.indexOf(" | ") + 3) : "";

        // ── Color theme ────────────────────────────────────────────────────────
        String[] theme = resolveTheme(notification);
        String bgColor     = theme[0];
        String borderColor = theme[1];
        String textColor   = theme[2];
        String accentColor = theme[3];

        // ── Left color bar (Region, not Rectangle — sizes to content height) ──────
        Region colorBar = new Region();
        colorBar.setPrefWidth(3);
        colorBar.setMaxWidth(3);
        colorBar.setStyle("-fx-background-color:" + accentColor + ";-fx-background-radius:3 0 0 3;");

        // ── Icon ────────────────────────────────────────────────────────────────
        FontIcon icon = new FontIcon(resolveIcon(notification));
        icon.setIconSize(13);
        icon.setIconColor(Color.web(accentColor));

        // ── Title ───────────────────────────────────────────────────────────────
        Label titleLbl = new Label(title);
        titleLbl.setStyle(
                "-fx-font-size:11px;" +
                "-fx-font-weight:bold;" +
                "-fx-text-fill:" + textColor + ";" +
                "-fx-wrap-text:true;"
        );
        titleLbl.setMaxWidth(TOAST_WIDTH - 60);

        // ── Sub-message (optional) ───────────────────────────────────────────────
        Label messageLbl = new Label(message);
        messageLbl.setStyle(
                "-fx-font-size:10px;" +
                "-fx-text-fill:" + textColor + "BB;" +
                "-fx-wrap-text:true;"
        );
        messageLbl.setMaxWidth(TOAST_WIDTH - 60);
        messageLbl.setVisible(!message.isEmpty());
        messageLbl.setManaged(!message.isEmpty());

        VBox textBox = new VBox(2, titleLbl, messageLbl);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        // ── Close button (icon, never truncated) ─────────────────────────────────
        FontIcon closeBtn = new FontIcon(Feather.X);
        closeBtn.setIconSize(11);
        closeBtn.setIconColor(Color.web(textColor + "88"));
        closeBtn.setStyle("-fx-cursor:hand;");

        // ── Inner content (icon + text + close) ──────────────────────────────────
        HBox content = new HBox(6, icon, textBox, closeBtn);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(8, 10, 8, 10));
        HBox.setHgrow(content, Priority.ALWAYS);

        // ── Full toast row (colorBar + content) — constrain height! ──────────────
        HBox toast = new HBox(0, colorBar, content);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setPrefWidth(TOAST_WIDTH);
        toast.setMaxWidth(TOAST_WIDTH);
        toast.setMaxHeight(Region.USE_PREF_SIZE);   // ← critical: prevents vertical stretch

        toast.setStyle(
                "-fx-background-color:" + bgColor + ";" +
                "-fx-background-radius:8;" +
                "-fx-border-color:" + borderColor + ";" +
                "-fx-border-radius:8;" +
                "-fx-border-width:1;" +
                "-fx-effect:dropshadow(gaussian,rgba(127,85,57,0.15),8,0,0,3);"
        );

        // ── Position: top-right ───────────────────────────────────────────────────
        StackPane.setAlignment(toast, Pos.TOP_RIGHT);
        StackPane.setMargin(toast, new Insets(55, 12, 0, 0));

        stackRoot.getChildren().add(toast);

        // ── Slide + Fade in ────────────────────────────────────────────────────
        toast.setOpacity(0);
        toast.setTranslateX(50);
        FadeTransition fi = new FadeTransition(Duration.millis(220), toast);
        fi.setFromValue(0); fi.setToValue(1); fi.play();
        TranslateTransition si = new TranslateTransition(Duration.millis(220), toast);
        si.setFromX(50); si.setToX(0); si.play();

        // ── Dismiss ────────────────────────────────────────────────────────────
        Runnable dismiss = () -> {
            FadeTransition fo = new FadeTransition(Duration.millis(200), toast);
            fo.setFromValue(1); fo.setToValue(0);
            TranslateTransition so = new TranslateTransition(Duration.millis(200), toast);
            so.setFromX(0); so.setToX(50);
            fo.setOnFinished(e -> stackRoot.getChildren().remove(toast));
            fo.play(); so.play();
        };

        closeBtn.setOnMouseClicked(e -> dismiss.run());
        toast.setOnMouseClicked(e -> dismiss.run());

        new Thread(() -> {
            try { Thread.sleep(DISPLAY_MS); } catch (InterruptedException ignored) {}
            Platform.runLater(dismiss);
        }).start();
    }

    // ── Theme resolution ────────────────────────────────────────────────────────
    // Returns { bgColor, borderColor, textColor, accentColor }

    private static String[] resolveTheme(AppNotification n) {
        // Priority: check type first, then severity as fallback
        if (n.getType() != null) {
            return switch (n.getType()) {
                case ORDER_CONFIRMED -> themeSuccess();
                case PAYMENT_FAILED  -> themeError();
                case ORDER_CANCELLED -> themeWarning();
                case STOCK_UPDATE    -> themeInfo();
                case ERROR           -> themeError();
                default              -> themeDefault();
            };
        }
        if (n.getSeverity() != null) {
            return switch (n.getSeverity()) {
                case ERROR, CRITICAL -> themeError();
                case WARNING         -> themeWarning();
                default              -> themeDefault();
            };
        }
        return themeDefault();
    }

    // ✅ Green confirmation — very slightly green
    private static String[] themeSuccess() {
        return new String[]{ "#F2F9F4", "#A7D7B2", "#1B5E35", "#27AE60" };
    }

    // ❌ Red error
    private static String[] themeError() {
        return new String[]{ "#FEF2F2", "#F5B8B8", "#9B1C1C", "#E74C3C" };
    }

    // ⚠️ Amber warning — matches app amber tones
    private static String[] themeWarning() {
        return new String[]{ "#FFF8ED", "#E6CCB2", APP_BROWN_DARK, APP_BROWN };
    }

    // 📦 Info — uses app brown
    private static String[] themeInfo() {
        return new String[]{ APP_BG_CARD, APP_BROWN_LIGHT, APP_BROWN_DARK, APP_BROWN };
    }

    // Default — app tan background
    private static String[] themeDefault() {
        return new String[]{ APP_BG_CARD, APP_BROWN_LIGHT, APP_BROWN_DARK, APP_BROWN };
    }

    private static Feather resolveIcon(AppNotification n) {
        if (n.getType() != null) {
            return switch (n.getType()) {
                case ORDER_CONFIRMED -> Feather.CHECK_CIRCLE;
                case PAYMENT_FAILED  -> Feather.ALERT_CIRCLE;
                case ORDER_CANCELLED -> Feather.X_CIRCLE;
                case STOCK_UPDATE    -> Feather.PACKAGE;
                default              -> Feather.BELL;
            };
        }
        if (n.getSeverity() != null) {
            return switch (n.getSeverity()) {
                case ERROR, CRITICAL -> Feather.ALERT_CIRCLE;
                case WARNING         -> Feather.ALERT_TRIANGLE;
                default              -> Feather.BELL;
            };
        }
        return Feather.BELL;
    }
}