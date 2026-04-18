package com.chrionline.clientmodule.client.ui.views;

import com.chrionline.clientmodule.client.security.KeyPairManager;
import com.chrionline.core.theme.AppTheme;
import com.chrionline.network.protocol.AppRequest;
import com.chrionline.network.protocol.AppResponse;
import com.chrionline.network.tcp.TCPClient;
import com.chrionline.shared.models.UserDevice;
import com.google.gson.Gson;
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
 * Displays in the Admin Settings screen (matching the Excalidraw diagram):
 *
 *  ┌────────────────────────────────────────────────────────┐
 *  │  ADMIN SETTINGS                                        │
 *  │  user@email.com                          ─────────    │
 *  │                                                        │
 *  │  [ Key-Based Authentication ]                          │
 *  │                                                        │
 *  │  Status: [ NOT CONFIGURED ] / [ CONFIGURED ]           │
 *  │  [ Generate Key Pair ]                                 │
 *  │  ──────────────────────────────────                   │
 *  │  [ Registered Keys ]                                   │
 *  │                                                        │
 *  │  ┌─────────────────────────────────────────────────┐  │
 *  │  │ Device: Laptop 1                                 │  │
 *  │  │ Fingerprint: SHA256:AB:34:9F:…                   │  │
 *  │  │ Created: 2026-04-18         [ Remove ]           │  │
 *  │  │ Other Devices                                    │  │
 *  │  │  - Work-PC                                       │  │
 *  │  │    Fingerprint: SHA256:91:FF:…                   │  │
 *  │  │    Last Used: 2026-04-15    [ Remove ]           │  │
 *  │  └─────────────────────────────────────────────────┘  │
 *  └────────────────────────────────────────────────────────┘
 *
 * Usage:
 *   KeyAuthSettingsView panel = new KeyAuthSettingsView(client, userEmail, authToken);
 *   // Embed in ProfileView or AdminView as a card
 */
public class KeyAuthSettingsView extends VBox {

    // ── Palette (matches AdminView warm brown theme) ──────────────────────
    private static final String BG          = AppTheme.BG;
    private static final String CARD_BG     = AppTheme.CARD_BG;
    private static final String PRIMARY     = AppTheme.PRIMARY;
    private static final String TEXT_MAIN   = AppTheme.TEXT_MAIN;
    private static final String TEXT_MUTED  = AppTheme.TEXT_MUTED;
    private static final String BORDER      = AppTheme.FIELD_BORDER;
    private static final String ERROR       = AppTheme.ERROR_COLOR;
    private static final String SUCCESS_CLR = "#16a34a";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FMT_LONG = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ── State ─────────────────────────────────────────────────────────────
    private final TCPClient client;
    private final String    userEmail;
    private final String    authToken;

    // The device name for *this* machine — derived from computer name
    private final String thisDeviceName;

    private Label       statusLabel;
    private VBox        devicesContainer;
    private Label       feedbackLabel;
    private Button      generateBtn;

    private List<UserDevice> registeredDevices = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * @param client    TCP client connected to the server
     * @param userEmail Authenticated user's email
     * @param authToken Session auth token
     */
    public KeyAuthSettingsView(TCPClient client, String userEmail, String authToken) {
        this.client         = client;
        this.userEmail      = userEmail;
        this.authToken      = authToken;
        this.thisDeviceName = resolveDeviceName();

        buildUI();
        loadDevices();
    }

    // ── UI Construction ───────────────────────────────────────────────────

    private void buildUI() {
        setSpacing(0);
        setStyle(
                "-fx-background-color:" + CARD_BG + ";" +
                        "-fx-background-radius:16;-fx-border-radius:16;" +
                        "-fx-border-color:" + BORDER + ";-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(100,60,30,0.09),14,0,0,3);"
        );
        setPadding(new Insets(0));
        setMaxWidth(Double.MAX_VALUE);

        // ── Header ────────────────────────────────────────────────────────
        VBox header = buildHeader();

        // ── Separator ─────────────────────────────────────────────────────
        Region sep1 = new Region();
        sep1.setPrefHeight(1); sep1.setMaxWidth(Double.MAX_VALUE);
        sep1.setStyle("-fx-background-color:" + BORDER + ";");

        // ── Body ──────────────────────────────────────────────────────────
        VBox body = buildBody();

        getChildren().addAll(header, sep1, body);
    }

    private VBox buildHeader() {
        VBox header = new VBox(6);
        header.setPadding(new Insets(22, 24, 18, 24));
        header.setStyle("-fx-background-color:" + CARD_BG + ";-fx-background-radius:16 16 0 0;");

        // Title row
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon lockIcon = new FontIcon(Feather.KEY);
        lockIcon.setIconSize(18);
        lockIcon.setIconColor(Color.web(PRIMARY));

        Label title = new Label("ADMIN SETTINGS");
        title.setStyle(
                "-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + PRIMARY + ";" +
                        "-fx-font-family:'Segoe UI Semibold','Segoe UI',sans-serif;"
        );
        titleRow.getChildren().addAll(lockIcon, title);

        // Email sub-label
        Label emailLabel = new Label(userEmail);
        emailLabel.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_MUTED + ";");

        header.getChildren().addAll(titleRow, emailLabel);
        return header;
    }

    private VBox buildBody() {
        VBox body = new VBox(20);
        body.setPadding(new Insets(20, 24, 24, 24));
        body.setStyle("-fx-background-color:" + BG + ";-fx-background-radius:0 0 16 16;");

        // ── Key-Based Authentication button-style label ───────────────────
        Label sectionBtn = new Label("Key-Based Authentication");
        sectionBtn.setStyle(
                "-fx-background-color:" + CARD_BG + ";" +
                        "-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-background-radius:8;" +
                        "-fx-border-width:1.5;-fx-padding:10 18 10 18;" +
                        "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + PRIMARY + ";"
        );
        sectionBtn.setMaxWidth(Double.MAX_VALUE);

        // ── Status line ────────────────────────────────────────────────────
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        Label statusPrefix = new Label("Status:");
        statusPrefix.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_MUTED + ";");

        statusLabel = new Label("[ NOT CONFIGURED ]");
        statusLabel.setStyle(
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + ERROR + ";"
        );
        statusRow.getChildren().addAll(statusPrefix, statusLabel);

        // ── Generate Key Pair button ───────────────────────────────────────
        generateBtn = buildPrimaryButton("Generate Key Pair", Feather.PLUS_CIRCLE);
        generateBtn.setOnAction(e -> handleGenerateKeyPair());

        // ── Registered Keys section header ────────────────────────────────
        Region sep = new Region();
        sep.setPrefHeight(1); sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color:" + BORDER + ";");

        Label regKeysLabel = new Label("Registered Keys");
        regKeysLabel.setStyle(
                "-fx-background-color:" + CARD_BG + ";" +
                        "-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-background-radius:8;" +
                        "-fx-border-width:1.5;-fx-padding:10 18 10 18;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + PRIMARY + ";"
        );
        regKeysLabel.setMaxWidth(Double.MAX_VALUE);

        // ── Devices list box ──────────────────────────────────────────────
        devicesContainer = new VBox(0);
        devicesContainer.setStyle(
                "-fx-background-color:" + CARD_BG + ";" +
                        "-fx-background-radius:10;-fx-border-radius:10;" +
                        "-fx-border-color:" + BORDER + ";-fx-border-width:1.5;" +
                        "-fx-padding:14 16 14 16;"
        );
        devicesContainer.setMaxWidth(Double.MAX_VALUE);

        Label loadingLbl = new Label("Chargement des clés...");
        loadingLbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_MUTED + ";");
        devicesContainer.getChildren().add(loadingLbl);

        // ── Feedback label ────────────────────────────────────────────────
        feedbackLabel = new Label();
        feedbackLabel.setStyle("-fx-font-size:12px;-fx-text-fill:" + ERROR + ";");
        feedbackLabel.setVisible(false);
        feedbackLabel.setWrapText(true);

        body.getChildren().addAll(
                sectionBtn,
                statusRow,
                generateBtn,
                sep,
                regKeysLabel,
                devicesContainer,
                feedbackLabel
        );

        return body;
    }

    // ── Generate Key Pair ─────────────────────────────────────────────────

    private void handleGenerateKeyPair() {
        // Prompt for device name
        TextInputDialog dialog = new TextInputDialog(thisDeviceName);
        dialog.setTitle("Enregistrer cet appareil");
        dialog.setHeaderText("Entrez le nom de cet appareil :");
        dialog.setContentText("Nom de l'appareil :");
        styleDialog(dialog);

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().trim().isEmpty()) return;

        String deviceName = result.get().trim();
        generateBtn.setDisable(true);
        generateBtn.setText("Génération en cours...");
        showFeedback("Génération de la paire de clés RSA-2048...", false);

        new Thread(() -> {
            try {
                // 1 — Generate RSA key pair and save binary file
                KeyPair kp = KeyPairManager.generateAndSave(deviceName);

                // 2 — Encode public key and compute fingerprint
                String publicKeyB64  = KeyPairManager.encodePublicKey(kp.getPublic());
                String fingerprint   = KeyPairManager.computeFingerprint(kp.getPublic());

                // 3 — Send to server
                Map<String, Object> payload = new HashMap<>();
                payload.put("userEmail",   userEmail);
                payload.put("deviceName",  deviceName);
                payload.put("publicKey",   publicKeyB64);
                payload.put("fingerprint", fingerprint);

                AppRequest req = new AppRequest.Builder()
                        .controller("KeyAuth").action("registerdevice")
                        .payload(new com.chrionline.core.utils.JsonUtils().toJson(payload))
                        .authToken(authToken)
                        .build();

                AppResponse resp = client.sendAndParse(req);

                Platform.runLater(() -> {
                    generateBtn.setDisable(false);
                    generateBtn.setText("Generate Key Pair");

                    if (resp != null && resp.isSuccess()) {
                        String keyPath = KeyPairManager.getKeyFilePath(deviceName).toString();
                        showFeedback("✅ Clé enregistrée ! Fichier binaire sauvegardé : " + keyPath, true);
                        loadDevices(); // refresh list
                    } else {
                        String msg = resp != null ? resp.getMessage() : "Erreur réseau.";
                        showFeedback("❌ " + msg, false);
                        // Clean up the local key file if server rejected
                        KeyPairManager.deleteKeyFile(deviceName);
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    generateBtn.setDisable(false);
                    generateBtn.setText("Generate Key Pair");
                    showFeedback("❌ Erreur : " + ex.getMessage(), false);
                });
            }
        }).start();
    }

    // ── Load / Render Devices ─────────────────────────────────────────────

    private void loadDevices() {
        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("KeyAuth").action("listdevices")
                        .parameter("userEmail", userEmail)
                        .authToken(authToken)
                        .build();

                AppResponse resp = client.sendAndParse(req);

                Platform.runLater(() -> {
                    devicesContainer.getChildren().clear();
                    if (resp != null && resp.isSuccess()) {
                        Type type = new TypeToken<List<UserDevice>>(){}.getType();
                        List<UserDevice> devices = new Gson().fromJson(
                                new Gson().toJson(resp.getData()), type);
                        registeredDevices = devices != null ? devices : new ArrayList<>();
                        renderDevices();
                        updateStatus();
                    } else {
                        Label err = new Label("Impossible de charger les clés enregistrées.");
                        err.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_MUTED + ";");
                        devicesContainer.getChildren().add(err);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    devicesContainer.getChildren().clear();
                    Label err = new Label("Erreur réseau.");
                    err.setStyle("-fx-font-size:13px;-fx-text-fill:" + ERROR + ";");
                    devicesContainer.getChildren().add(err);
                });
            }
        }).start();
    }

    private void renderDevices() {
        devicesContainer.getChildren().clear();

        if (registeredDevices.isEmpty()) {
            Label empty = new Label("Aucune clé enregistrée pour ce compte.");
            empty.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_MUTED + ";-fx-font-style:italic;");
            devicesContainer.getChildren().add(empty);
            return;
        }

        // Separate "this device" from "other devices"
        UserDevice thisDevice = null;
        List<UserDevice> others = new ArrayList<>();

        for (UserDevice d : registeredDevices) {
            if (KeyPairManager.keyFileExists(d.getDeviceName())) {
                if (thisDevice == null) thisDevice = d;
                else others.add(d);
            } else {
                others.add(d);
            }
        }

        // If no local key file matches, show all as "other"
        if (thisDevice == null && !registeredDevices.isEmpty()) {
            thisDevice = registeredDevices.get(0);
            others = registeredDevices.subList(1, registeredDevices.size());
        }

        // Render this device
        if (thisDevice != null) {
            devicesContainer.getChildren().add(buildThisDeviceEntry(thisDevice));
        }

        // Render other devices
        if (!others.isEmpty()) {
            Label otherLabel = new Label("Other Devices");
            otherLabel.setStyle(
                    "-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_MUTED + ";" +
                            "-fx-padding:10 0 4 0;"
            );
            devicesContainer.getChildren().add(otherLabel);

            for (UserDevice d : others) {
                devicesContainer.getChildren().add(buildOtherDeviceEntry(d));
            }
        }
    }

    /** Renders the "this device" block (top of the registered keys box). */
    private VBox buildThisDeviceEntry(UserDevice device) {
        VBox entry = new VBox(3);
        entry.setPadding(new Insets(0, 0, 8, 0));

        Label deviceNameLbl = new Label("Device: " + device.getDeviceName());
        deviceNameLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_MAIN + ";");

        Label fpLbl = new Label("Fingerprint: " + device.getShortFingerprint());
        fpLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_MUTED + ";-fx-font-family:'Courier New',monospace;");

        String dateStr = device.getCreatedAt() != null
                ? device.getCreatedAt().format(FMT)
                : "—";
        Label dateLbl = new Label("Created: " + dateStr);
        dateLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_MUTED + ";");

        Button removeBtn = buildSmallRemoveButton();
        removeBtn.setOnAction(e -> confirmRevoke(device));

        entry.getChildren().addAll(deviceNameLbl, fpLbl, dateLbl, removeBtn);
        return entry;
    }

    /** Renders an "other device" row with bullet point. */
    private VBox buildOtherDeviceEntry(UserDevice device) {
        VBox entry = new VBox(2);
        entry.setPadding(new Insets(4, 0, 4, 12));  // indent

        HBox nameRow = new HBox(6);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label bullet = new Label("•");
        bullet.setStyle("-fx-font-size:14px;-fx-text-fill:" + PRIMARY + ";");
        Label nameLbl = new Label(device.getDeviceName());
        nameLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_MAIN + ";");
        nameRow.getChildren().addAll(bullet, nameLbl);

        Label fpLbl = new Label("  Fingerprint: " + device.getShortFingerprint());
        fpLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_MUTED + ";-fx-font-family:'Courier New',monospace;");

        String lastUsed = "Never";
        if (device.getLastUsedAt() != null) {
            lastUsed = device.getLastUsedAt().format(FMT);
        }
        Label lastLbl = new Label("  Last Used: " + lastUsed);
        lastLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_MUTED + ";");

        Button removeBtn = buildSmallRemoveButton();
        removeBtn.setOnAction(e -> confirmRevoke(device));

        entry.getChildren().addAll(nameRow, fpLbl, lastLbl, removeBtn);
        return entry;
    }

    // ── Revoke ────────────────────────────────────────────────────────────

    private void confirmRevoke(UserDevice device) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Révoquer l'appareil");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Révoquer la clé pour \"" + device.getDeviceName() + "\" ?\n" +
                        "Fingerprint: " + device.getShortFingerprint() + "\n\n" +
                        "L'appareil ne pourra plus s'authentifier avec cette clé."
        );
        styleDialog(confirm);

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                revokeDevice(device);
            }
        });
    }

    private void revokeDevice(UserDevice device) {
        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("KeyAuth").action("revokedevice")
                        .payload(new com.chrionline.core.utils.JsonUtils()
                                .toJson(Map.of("id", device.getId())))
                        .authToken(authToken)
                        .build();

                AppResponse resp = client.sendAndParse(req);

                Platform.runLater(() -> {
                    if (resp != null && resp.isSuccess()) {
                        // Also delete local key file if it's this device
                        KeyPairManager.deleteKeyFile(device.getDeviceName());
                        showFeedback("Appareil révoqué avec succès.", true);
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

    // ── Status Update ─────────────────────────────────────────────────────

    private void updateStatus() {
        boolean configured = !registeredDevices.isEmpty()
                && registeredDevices.stream().anyMatch(d -> KeyPairManager.keyFileExists(d.getDeviceName()));

        if (configured) {
            statusLabel.setText("[ CONFIGURED ]");
            statusLabel.setStyle(
                    "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + SUCCESS_CLR + ";"
            );
        } else if (!registeredDevices.isEmpty()) {
            statusLabel.setText("[ REMOTE KEYS ONLY ]");
            statusLabel.setStyle(
                    "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#D97706;"
            );
        } else {
            statusLabel.setText("[ NOT CONFIGURED ]");
            statusLabel.setStyle(
                    "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + ERROR + ";"
            );
        }
    }

    // ── UI Helpers ────────────────────────────────────────────────────────

    private Button buildPrimaryButton(String text, Feather icon) {
        Button btn = new Button(text);
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(15);
        fi.setIconColor(Color.web(BG));
        btn.setGraphic(fi);
        String base = "-fx-background-color:" + PRIMARY + ";-fx-text-fill:" + BG + ";" +
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:9;" +
                "-fx-padding:10 20 10 20;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(PRIMARY, AppTheme.PRIMARY_LIGHT)));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private Button buildSmallRemoveButton() {
        Button btn = new Button("[ Remove ]");
        String base = "-fx-background-color:transparent;-fx-text-fill:" + ERROR + ";" +
                "-fx-font-size:12px;-fx-cursor:hand;-fx-padding:3 0 0 0;" +
                "-fx-border-color:transparent;-fx-underline:true;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace("transparent;-fx-text-fill:" + ERROR,
                "#FEE2E2;-fx-text-fill:" + ERROR)));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setStyle(
                "-fx-font-size:12px;-fx-text-fill:" + (success ? SUCCESS_CLR : ERROR) + ";"
        );
        feedbackLabel.setVisible(true);
    }

    private String resolveDeviceName() {
        // Try hostname first, fallback to username+os
        try {
            String host = java.net.InetAddress.getLocalHost().getHostName();
            if (host != null && !host.isBlank() && !host.equals("localhost")) return host;
        } catch (Exception ignored) {}
        String user = System.getProperty("user.name", "Device");
        String os   = System.getProperty("os.name", "PC").split(" ")[0];
        return user + "-" + os;
    }

    private void styleDialog(Dialog<?> dialog) {
        try {
            dialog.getDialogPane().setStyle(
                    "-fx-background-color:" + BG + ";"
            );
        } catch (Exception ignored) {}
    }
}