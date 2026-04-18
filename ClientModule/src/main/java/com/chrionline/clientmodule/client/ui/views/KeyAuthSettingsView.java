package com.chrionline.clientmodule.client.ui.views;

import com.chrionline.clientmodule.client.security.KeyPairManager;
import com.chrionline.core.theme.AppTheme;
import com.chrionline.core.utils.JsonUtils;
import com.chrionline.network.protocol.AppRequest;
import com.chrionline.network.protocol.AppResponse;
import com.chrionline.network.tcp.TCPClient;
import com.chrionline.shared.models.UserDevice;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.lang.reflect.Type;
import java.security.KeyPair;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Admin / Profile Settings panel — Key-Based Authentication section.
 *
 * FIX: replaced `new Gson().fromJson(...)` with `JsonUtils.fromJson(...)` so that
 * LocalDateTime fields in UserDevice are deserialized via the registered TypeAdapters
 * in JsonUtils (which handle java.time without needing java.base to be open to Gson).
 */
public class KeyAuthSettingsView extends VBox {

    private static final String BG          = AppTheme.BG;
    private static final String CARD_BG     = AppTheme.CARD_BG;
    private static final String PRIMARY     = AppTheme.PRIMARY;
    private static final String TEXT_MAIN   = AppTheme.TEXT_MAIN;
    private static final String TEXT_MUTED  = AppTheme.TEXT_MUTED;
    private static final String BORDER      = AppTheme.FIELD_BORDER;
    private static final String ERROR       = AppTheme.ERROR_COLOR;
    private static final String SUCCESS_CLR = "#16a34a";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TCPClient client;
    private final String    userEmail;
    private final String    authToken;
    private final String    thisDeviceName;

    private Label  statusLabel;
    private VBox   devicesContainer;
    private Label  feedbackLabel;
    private Button generateBtn;

    private List<UserDevice> registeredDevices = new ArrayList<>();

    public KeyAuthSettingsView(TCPClient client, String userEmail, String authToken) {
        this.client         = client;
        this.userEmail      = userEmail;
        this.authToken      = authToken;
        this.thisDeviceName = resolveDeviceName();

        buildUI();
        loadDevices();
    }

    // ── UI ────────────────────────────────────────────────────────────────

    private void buildUI() {
        setSpacing(0);
        setStyle(
                "-fx-background-color:" + CARD_BG + ";" +
                        "-fx-background-radius:16;-fx-border-radius:16;" +
                        "-fx-border-color:" + BORDER + ";-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(100,60,30,0.09),14,0,0,3);"
        );
        setMaxWidth(Double.MAX_VALUE);
        getChildren().addAll(buildHeader(), buildHLine(), buildBody());
    }

    private VBox buildHeader() {
        VBox header = new VBox(6);
        header.setPadding(new Insets(22, 24, 18, 24));
        header.setStyle("-fx-background-color:" + CARD_BG + ";-fx-background-radius:16 16 0 0;");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon icon = new FontIcon(Feather.KEY);
        icon.setIconSize(18); icon.setIconColor(Color.web(PRIMARY));
        Label title = new Label("ADMIN SETTINGS");
        title.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + PRIMARY + ";-fx-font-family:'Segoe UI Semibold','Segoe UI',sans-serif;");
        titleRow.getChildren().addAll(icon, title);

        Label emailLbl = new Label(userEmail);
        emailLbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_MUTED + ";");
        header.getChildren().addAll(titleRow, emailLbl);
        return header;
    }

    private Region buildHLine() {
        Region r = new Region();
        r.setPrefHeight(1); r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color:" + BORDER + ";");
        return r;
    }

    private VBox buildBody() {
        VBox body = new VBox(20);
        body.setPadding(new Insets(20, 24, 24, 24));
        body.setStyle("-fx-background-color:" + BG + ";-fx-background-radius:0 0 16 16;");

        Label sectionBtn = new Label("Key-Based Authentication");
        sectionBtn.setStyle("-fx-background-color:" + CARD_BG + ";-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-background-radius:8;-fx-border-width:1.5;-fx-padding:10 18 10 18;-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + PRIMARY + ";");
        sectionBtn.setMaxWidth(Double.MAX_VALUE);

        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        Label statusPrefix = new Label("Status:");
        statusPrefix.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_MUTED + ";");
        statusLabel = new Label("[ NOT CONFIGURED ]");
        statusLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + ERROR + ";");
        statusRow.getChildren().addAll(statusPrefix, statusLabel);

        generateBtn = buildPrimaryButton("Generate Key Pair", Feather.PLUS_CIRCLE);
        generateBtn.setOnAction(e -> handleGenerateKeyPair());

        Label regKeysLabel = new Label("Registered Keys");
        regKeysLabel.setStyle("-fx-background-color:" + CARD_BG + ";-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-background-radius:8;-fx-border-width:1.5;-fx-padding:10 18 10 18;-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + PRIMARY + ";");
        regKeysLabel.setMaxWidth(Double.MAX_VALUE);

        devicesContainer = new VBox(0);
        devicesContainer.setStyle("-fx-background-color:" + CARD_BG + ";-fx-background-radius:10;-fx-border-radius:10;-fx-border-color:" + BORDER + ";-fx-border-width:1.5;-fx-padding:14 16 14 16;");
        devicesContainer.setMaxWidth(Double.MAX_VALUE);
        Label loading = new Label("Chargement des clés...");
        loading.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_MUTED + ";");
        devicesContainer.getChildren().add(loading);

        feedbackLabel = new Label();
        feedbackLabel.setStyle("-fx-font-size:12px;-fx-text-fill:" + ERROR + ";");
        feedbackLabel.setVisible(false);
        feedbackLabel.setWrapText(true);

        body.getChildren().addAll(sectionBtn, statusRow, generateBtn, buildHLine(), regKeysLabel, devicesContainer, feedbackLabel);
        return body;
    }

    // ── Generate ──────────────────────────────────────────────────────────

    private void handleGenerateKeyPair() {
        TextInputDialog dialog = new TextInputDialog(thisDeviceName);
        dialog.setTitle("Enregistrer cet appareil");
        dialog.setHeaderText("Entrez le nom de cet appareil :");
        dialog.setContentText("Nom :");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().trim().isEmpty()) return;

        String deviceName = result.get().trim();
        generateBtn.setDisable(true);
        generateBtn.setText("Génération...");
        showFeedback("Génération RSA-2048...", false);

        new Thread(() -> {
            try {
                KeyPair kp = KeyPairManager.generateAndSave(deviceName);
                String publicKeyB64 = KeyPairManager.encodePublicKey(kp.getPublic());
                String fingerprint  = KeyPairManager.computeFingerprint(kp.getPublic());

                Map<String, Object> payload = new HashMap<>();
                payload.put("userEmail",   userEmail);
                payload.put("deviceName",  deviceName);
                payload.put("publicKey",   publicKeyB64);
                payload.put("fingerprint", fingerprint);

                AppRequest req = new AppRequest.Builder()
                        .controller("KeyAuth").action("registerdevice")
                        .payload(JsonUtils.toJson(payload))
                        .authToken(authToken).build();

                AppResponse resp = client.sendAndParse(req);

                Platform.runLater(() -> {
                    generateBtn.setDisable(false);
                    generateBtn.setText("Generate Key Pair");
                    if (resp != null && resp.isSuccess()) {
                        showFeedback("✅ Clé enregistrée ! Fichier : " +
                                KeyPairManager.getKeyFilePath(deviceName), true);
                        loadDevices();
                    } else {
                        showFeedback("❌ " + (resp != null ? resp.getMessage() : "Erreur réseau."), false);
                        KeyPairManager.deleteKeyFile(deviceName);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    generateBtn.setDisable(false);
                    generateBtn.setText("Generate Key Pair");
                    showFeedback("❌ " + ex.getMessage(), false);
                });
            }
        }).start();
    }

    // ── Load Devices ──────────────────────────────────────────────────────

    private void loadDevices() {
        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("KeyAuth").action("listdevices")
                        .parameter("userEmail", userEmail)
                        .authToken(authToken).build();

                AppResponse resp = client.sendAndParse(req);

                Platform.runLater(() -> {
                    devicesContainer.getChildren().clear();
                    if (resp != null && resp.isSuccess()) {
                        // KEY FIX: use JsonUtils (has LocalDateTime TypeAdapters) instead of new Gson()
                        Type type = new TypeToken<List<UserDevice>>(){}.getType();
                        List<UserDevice> devices = JsonUtils.fromJson(
                                JsonUtils.toJson(resp.getData()), type);

                        registeredDevices = devices != null ? devices : new ArrayList<>();
                        renderDevices();
                        updateStatus();
                    } else {
                        addLabel(devicesContainer, "Impossible de charger les clés.", TEXT_MUTED);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    devicesContainer.getChildren().clear();
                    addLabel(devicesContainer, "Erreur réseau : " + e.getMessage(), ERROR);
                });
            }
        }).start();
    }

    // ── Render ────────────────────────────────────────────────────────────

    private void renderDevices() {
        devicesContainer.getChildren().clear();

        if (registeredDevices.isEmpty()) {
            addLabel(devicesContainer, "Aucune clé enregistrée pour ce compte.", TEXT_MUTED);
            return;
        }

        UserDevice thisDevice = null;
        List<UserDevice> others = new ArrayList<>();
        for (UserDevice d : registeredDevices) {
            if (thisDevice == null && KeyPairManager.keyFileExists(d.getDeviceName())) {
                thisDevice = d;
            } else {
                others.add(d);
            }
        }
        if (thisDevice == null) {
            thisDevice = registeredDevices.get(0);
            others = new ArrayList<>(registeredDevices.subList(1, registeredDevices.size()));
        }

        devicesContainer.getChildren().add(buildThisDeviceEntry(thisDevice));

        if (!others.isEmpty()) {
            Label otherLbl = new Label("Other Devices");
            otherLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_MUTED + ";-fx-padding:10 0 4 0;");
            devicesContainer.getChildren().add(otherLbl);
            for (UserDevice d : others) devicesContainer.getChildren().add(buildOtherDeviceEntry(d));
        }
    }

    private VBox buildThisDeviceEntry(UserDevice d) {
        VBox v = new VBox(3);
        v.setPadding(new Insets(0, 0, 8, 0));
        v.getChildren().addAll(
                styledLabel("Device: " + d.getDeviceName(), TEXT_MAIN, true, false),
                styledLabel("Fingerprint: " + d.getShortFingerprint(), TEXT_MUTED, false, true),
                styledLabel("Created: " + (d.getCreatedAt() != null ? d.getCreatedAt().format(FMT) : "—"), TEXT_MUTED, false, false),
                removeBtn(d)
        );
        return v;
    }

    private VBox buildOtherDeviceEntry(UserDevice d) {
        VBox v = new VBox(2);
        v.setPadding(new Insets(4, 0, 4, 12));
        HBox nameRow = new HBox(6);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label bullet = new Label("•");
        bullet.setStyle("-fx-font-size:14px;-fx-text-fill:" + PRIMARY + ";");
        Label nameLbl = styledLabel(d.getDeviceName(), TEXT_MAIN, true, false);
        nameRow.getChildren().addAll(bullet, nameLbl);
        String lastUsed = d.getLastUsedAt() != null ? d.getLastUsedAt().format(FMT) : "Never";
        v.getChildren().addAll(
                nameRow,
                styledLabel("  Fingerprint: " + d.getShortFingerprint(), TEXT_MUTED, false, true),
                styledLabel("  Last Used: " + lastUsed, TEXT_MUTED, false, false),
                removeBtn(d)
        );
        return v;
    }

    // ── Revoke ────────────────────────────────────────────────────────────

    private void confirmRevoke(UserDevice device) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Révoquer l'appareil");
        confirm.setHeaderText(null);
        confirm.setContentText("Révoquer \"" + device.getDeviceName() + "\" ?\n" +
                device.getShortFingerprint() + "\n\nCet appareil ne pourra plus s'authentifier.");
        confirm.showAndWait().ifPresent(btn -> { if (btn == ButtonType.OK) revokeDevice(device); });
    }

    private void revokeDevice(UserDevice device) {
        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("KeyAuth").action("revokedevice")
                        .payload(JsonUtils.toJson(Map.of("id", device.getId())))
                        .authToken(authToken).build();
                AppResponse resp = client.sendAndParse(req);
                Platform.runLater(() -> {
                    if (resp != null && resp.isSuccess()) {
                        KeyPairManager.deleteKeyFile(device.getDeviceName());
                        showFeedback("Appareil révoqué.", true);
                        loadDevices();
                    } else {
                        showFeedback("Erreur lors de la révocation.", false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showFeedback("Erreur réseau : " + e.getMessage(), false));
            }
        }).start();
    }

    // ── Status ────────────────────────────────────────────────────────────

    private void updateStatus() {
        boolean hasLocal = registeredDevices.stream()
                .anyMatch(d -> KeyPairManager.keyFileExists(d.getDeviceName()));
        if (hasLocal) {
            statusLabel.setText("[ CONFIGURED ]");
            statusLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + SUCCESS_CLR + ";");
        } else if (!registeredDevices.isEmpty()) {
            statusLabel.setText("[ REMOTE KEYS ONLY ]");
            statusLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#D97706;");
        } else {
            statusLabel.setText("[ NOT CONFIGURED ]");
            statusLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + ERROR + ";");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Button buildPrimaryButton(String text, Feather icon) {
        Button btn = new Button(text);
        FontIcon fi = new FontIcon(icon); fi.setIconSize(15); fi.setIconColor(Color.web(BG));
        btn.setGraphic(fi);
        String base = "-fx-background-color:" + PRIMARY + ";-fx-text-fill:" + BG + ";-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:9;-fx-padding:10 20 10 20;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(PRIMARY, AppTheme.PRIMARY_LIGHT)));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private Button removeBtn(UserDevice d) {
        Button btn = new Button("[ Revoke ]");
        String base = "-fx-background-color:transparent;-fx-text-fill:" + ERROR + ";-fx-font-size:12px;-fx-cursor:hand;-fx-padding:3 0 0 0;-fx-border-color:transparent;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base + "-fx-underline:true;"));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        btn.setOnAction(e -> confirmRevoke(d));
        return btn;
    }

    private Label styledLabel(String text, String color, boolean bold, boolean mono) {
        Label l = new Label(text);
        String style = "-fx-font-size:13px;-fx-text-fill:" + color + ";";
        if (bold) style += "-fx-font-weight:bold;";
        if (mono) style += "-fx-font-family:'Courier New',monospace;-fx-font-size:12px;";
        l.setStyle(style);
        return l;
    }

    private void addLabel(VBox container, String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:13px;-fx-text-fill:" + color + ";-fx-font-style:italic;");
        container.getChildren().add(l);
    }

    private void showFeedback(String msg, boolean success) {
        feedbackLabel.setText(msg);
        feedbackLabel.setStyle("-fx-font-size:12px;-fx-text-fill:" + (success ? SUCCESS_CLR : ERROR) + ";");
        feedbackLabel.setVisible(true);
    }

    private String resolveDeviceName() {
        try {
            String host = java.net.InetAddress.getLocalHost().getHostName();
            if (host != null && !host.isBlank() && !host.equalsIgnoreCase("localhost")) return host;
        } catch (Exception ignored) {}
        return System.getProperty("user.name", "Device") + "-PC";
    }
}