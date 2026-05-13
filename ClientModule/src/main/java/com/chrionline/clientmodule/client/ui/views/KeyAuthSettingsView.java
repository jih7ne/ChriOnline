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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.lang.reflect.Type;
import java.security.KeyPair;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Admin / Profile Settings panel — Key-Based Authentication section.
 *
 * Changes:
 *  - Device name is auto-detected from hostname (not editable by user)
 *  - If the device already has a registered key, a confirmation dialog asks
 *    whether to revoke the old key and generate a new one
 *  - Enhanced UI with status indicators, cleaner card layout, and better feedback
 */
public class KeyAuthSettingsView extends VBox {

    // ── Palette (inherits from AppTheme) ─────────────────────────────────
    private static final String BG          = AppTheme.BG;
    private static final String CARD_BG     = AppTheme.CARD_BG;
    private static final String PRIMARY     = AppTheme.PRIMARY;
    private static final String PRIMARY_L   = AppTheme.PRIMARY_LIGHT;
    private static final String TEXT_MAIN   = AppTheme.TEXT_MAIN;
    private static final String TEXT_MUTED  = AppTheme.TEXT_MUTED;
    private static final String BORDER      = AppTheme.FIELD_BORDER;
    private static final String ERROR       = AppTheme.ERROR_COLOR;
    private static final String SUCCESS_CLR = "#16a34a";
    private static final String WARN_CLR    = "#D97706";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ── State ─────────────────────────────────────────────────────────────
    private final TCPClient client;
    private final String    userEmail;
    private final String    authToken;

    /** The hostname of this machine — used as device name, read-only. */
    private final String    thisDeviceName;

    // ── UI refs ───────────────────────────────────────────────────────────
    private Label  statusLabel;
    private Label  statusDot;
    private VBox   devicesContainer;
    private Label  feedbackLabel;
    private Button generateBtn;

    private List<UserDevice> registeredDevices = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────

    public KeyAuthSettingsView(TCPClient client, String userEmail, String authToken) {
        this.client         = client;
        this.userEmail      = userEmail;
        this.authToken      = authToken;
        this.thisDeviceName = resolveDeviceName();

        buildUI();
        loadDevices();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  UI BUILD
    // ═══════════════════════════════════════════════════════════════════

    private void buildUI() {
        setSpacing(16);
        setPadding(new Insets(4, 0, 4, 0));
        setStyle("-fx-background-color:transparent;");
        getChildren().addAll(
                buildStatusCard(),
                buildDeviceCard(),
                buildRegisteredKeysCard()
        );
    }

    // ── Status card ───────────────────────────────────────────────────────

    private VBox buildStatusCard() {
        VBox card = card();

        // Header row
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon icon = new FontIcon(Feather.SHIELD);
        icon.setIconSize(18); icon.setIconColor(Color.web(PRIMARY));
        Label title = bold("Authentification par clé RSA", 16);
        titleRow.getChildren().addAll(icon, title);

        // Status row
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setPadding(new Insets(10, 14, 10, 14));
        statusRow.setStyle(
                "-fx-background-color:" + BG + ";" +
                        "-fx-background-radius:10;" +
                        "-fx-border-radius:10;" +
                        "-fx-border-color:" + BORDER + ";-fx-border-width:1;"
        );

        statusDot = new Label("●");
        statusDot.setStyle("-fx-font-size:14px;-fx-text-fill:" + ERROR + ";");
        statusLabel = new Label("NON CONFIGURÉ");
        statusLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + ERROR + ";");
        Label statusHint = new Label("— Générez une paire de clés pour activer l'authentification sécurisée");
        statusHint.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_MUTED + ";");
        statusRow.getChildren().addAll(statusDot, statusLabel, statusHint);

        // Device name row (read-only info)
        HBox deviceInfoRow = new HBox(8);
        deviceInfoRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon monitorIcon = new FontIcon(Feather.MONITOR);
        monitorIcon.setIconSize(14); monitorIcon.setIconColor(Color.web(TEXT_MUTED));
        Label deviceLbl = new Label("Cet appareil :");
        deviceLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_MUTED + ";");
        Label deviceNameLbl = new Label(thisDeviceName);
        deviceNameLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_MAIN + ";");
        deviceInfoRow.getChildren().addAll(monitorIcon, deviceLbl, deviceNameLbl);

        card.getChildren().addAll(titleRow, statusRow, deviceInfoRow);
        return card;
    }

    // ── Generate / Manage card ─────────────────────────────────────────────

    private VBox buildDeviceCard() {
        VBox card = card();

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon icon = new FontIcon(Feather.KEY);
        icon.setIconSize(16); icon.setIconColor(Color.web(PRIMARY));
        Label title = bold("Clé pour cet appareil", 15);
        titleRow.getChildren().addAll(icon, title);

        Label hint = new Label(
                "La clé est liée au nom de cet appareil (" + thisDeviceName + ") " +
                        "et stockée localement dans ~/.chrionline/keys/. " +
                        "Si une clé existe déjà pour ce nom, elle sera révoquée et remplacée."
        );
        hint.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_MUTED + ";-fx-wrap-text:true;");
        hint.setWrapText(true);

        generateBtn = primaryBtn("  Générer une paire de clés", Feather.PLUS_CIRCLE);
        generateBtn.setOnAction(e -> handleGenerateKeyPair());

        feedbackLabel = new Label();
        feedbackLabel.setStyle("-fx-font-size:12px;-fx-text-fill:" + ERROR + ";");
        feedbackLabel.setVisible(false);
        feedbackLabel.setWrapText(true);

        card.getChildren().addAll(titleRow, hint, generateBtn, feedbackLabel);
        return card;
    }

    // ── Registered keys card ──────────────────────────────────────────────

    private VBox buildRegisteredKeysCard() {
        VBox card = card();

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon icon = new FontIcon(Feather.LIST);
        icon.setIconSize(16); icon.setIconColor(Color.web(PRIMARY));
        Label title = bold("Appareils enregistrés", 15);
        titleRow.getChildren().addAll(icon, title);

        devicesContainer = new VBox(0);
        devicesContainer.setMaxWidth(Double.MAX_VALUE);

        Label loading = new Label("Chargement…");
        loading.setStyle("-fx-font-size:13px;-fx-text-fill:" + TEXT_MUTED + ";-fx-font-style:italic;");
        devicesContainer.getChildren().add(loading);

        card.getChildren().addAll(titleRow, devicesContainer);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  GENERATE KEY PAIR LOGIC
    // ═══════════════════════════════════════════════════════════════════

    private void handleGenerateKeyPair() {
        // Check if a key already exists locally or on the server for this device name
        boolean localKeyExists  = KeyPairManager.keyFileExists(thisDeviceName);
        boolean serverKeyExists = registeredDevices.stream()
                .anyMatch(d -> thisDeviceName.equals(d.getDeviceName()));

        if (localKeyExists || serverKeyExists) {
            // Show confirmation dialog — user must confirm replacement
            showReplaceKeyDialog(thisDeviceName, localKeyExists, serverKeyExists);
        } else {
            // No existing key — generate directly
            doGenerateAndRegister(thisDeviceName);
        }
    }

    /**
     * Confirmation dialog shown when the device already has a registered key.
     * Warns the user that the existing key will be revoked and replaced.
     */
    private void showReplaceKeyDialog(String deviceName, boolean localExists, boolean serverExists) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.DECORATED);
        dialog.setResizable(false);
        dialog.setTitle("Remplacer la clé existante ?");

        // ── Icon ─────────────────────────────────────────────────────────
        FontIcon warnIcon = new FontIcon(Feather.ALERT_TRIANGLE);
        warnIcon.setIconSize(28); warnIcon.setIconColor(Color.web(WARN_CLR));
        StackPane iconCircle = new StackPane(warnIcon);
        iconCircle.setPrefSize(60, 60); iconCircle.setMinSize(60, 60); iconCircle.setMaxSize(60, 60);
        iconCircle.setStyle("-fx-background-color:#FEF3C7;-fx-background-radius:30;");

        // ── Text block ────────────────────────────────────────────────────
        Label titleLbl = new Label("Remplacer la clé existante ?");
        titleLbl.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_MAIN + ";");

        Label deviceLbl = new Label("Appareil : " + deviceName);
        deviceLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + PRIMARY + ";");

        VBox bullets = new VBox(6);
        if (localExists)  bullets.getChildren().add(bulletRow("Le fichier de clé local sera supprimé"));
        if (serverExists) bullets.getChildren().add(bulletRow("La clé enregistrée sur le serveur sera révoquée"));
        bullets.getChildren().add(bulletRow("Une nouvelle paire de clés sera générée et enregistrée"));
        bullets.getChildren().add(bulletRow("L'ancienne clé ne pourra plus être utilisée pour se connecter"));

        Label warnNote = new Label("⚠  Cette action est irréversible.");
        warnNote.setStyle("-fx-font-size:12px;-fx-text-fill:" + WARN_CLR + ";-fx-font-weight:bold;");

        VBox textBox = new VBox(8, titleLbl, deviceLbl, bullets, warnNote);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox contentBox = new HBox(18, iconCircle, textBox);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setPadding(new Insets(24, 28, 20, 28));
        contentBox.setStyle("-fx-background-color:" + BG + ";");

        // ── Buttons ───────────────────────────────────────────────────────
        Button cancelBtn = outlineBtn("Annuler");
        cancelBtn.setPrefWidth(120);
        cancelBtn.setOnAction(e -> dialog.close());

        Button confirmBtn = warnBtn("Remplacer la clé");
        confirmBtn.setPrefWidth(160);
        confirmBtn.setOnAction(e -> {
            dialog.close();
            // Revoke server-side first if needed, then generate
            if (serverExists) {
                revokeExistingDeviceAndGenerate(deviceName);
            } else {
                // Only local key exists — delete it and generate
                KeyPairManager.deleteKeyFile(deviceName);
                doGenerateAndRegister(deviceName);
            }
        });

        HBox btnBar = new HBox(12, cancelBtn, confirmBtn);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(14, 28, 20, 28));
        btnBar.setStyle(
                "-fx-background-color:" + CARD_BG + ";" +
                        "-fx-border-color:" + BORDER + ";-fx-border-width:1 0 0 0;"
        );

        VBox root = new VBox(0, contentBox, btnBar);
        root.setStyle("-fx-background-color:" + BG + ";");
        dialog.setScene(new Scene(root));
        dialog.showAndWait();
    }

    private HBox bulletRow(String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label dot = new Label("•");
        dot.setStyle("-fx-text-fill:" + PRIMARY + ";-fx-font-size:14px;");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_MAIN + ";");
        row.getChildren().addAll(dot, lbl);
        return row;
    }

    /**
     * Find the existing server-side device entry for this device name,
     * revoke it, then generate a new key.
     */
    private void revokeExistingDeviceAndGenerate(String deviceName) {
        Optional<UserDevice> existing = registeredDevices.stream()
                .filter(d -> deviceName.equals(d.getDeviceName()))
                .findFirst();

        if (existing.isEmpty()) {
            // Nothing to revoke on server — just delete local and generate
            KeyPairManager.deleteKeyFile(deviceName);
            doGenerateAndRegister(deviceName);
            return;
        }

        int deviceId = existing.get().getId();
        setGenerating(true);
        showFeedback("Révocation de l'ancienne clé…", false);

        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("KeyAuth").action("revokedevice")
                        .payload(JsonUtils.toJson(Map.of("id", deviceId)))
                        .authToken(authToken).build();
                AppResponse resp = client.sendAndParse(req);

                Platform.runLater(() -> {
                    if (resp != null && resp.isSuccess()) {
                        // Delete local key file, then generate fresh
                        KeyPairManager.deleteKeyFile(deviceName);
                        doGenerateAndRegister(deviceName);
                    } else {
                        setGenerating(false);
                        showFeedback("❌ Impossible de révoquer l'ancienne clé : " +
                                (resp != null ? resp.getMessage() : "erreur réseau"), false);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setGenerating(false);
                    showFeedback("❌ Erreur réseau : " + ex.getMessage(), false);
                });
            }
        }).start();
    }

    /**
     * Core: generate RSA key pair, save to disk, register with server.
     */
    private void doGenerateAndRegister(String deviceName) {
        setGenerating(true);
        showFeedback("Génération RSA-2048 en cours…", false);

        new Thread(() -> {
            try {
                KeyPair kp          = KeyPairManager.generateAndSave(deviceName);
                String  publicKeyB64 = KeyPairManager.encodePublicKey(kp.getPublic());
                String  fingerprint  = KeyPairManager.computeFingerprint(kp.getPublic());

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
                    setGenerating(false);
                    if (resp != null && resp.isSuccess()) {
                        showFeedback("✅ Clé enregistrée avec succès !", true);
                        loadDevices();
                    } else {
                        showFeedback("❌ " + (resp != null ? resp.getMessage() : "Erreur réseau."), false);
                        KeyPairManager.deleteKeyFile(deviceName);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setGenerating(false);
                    showFeedback("❌ " + ex.getMessage(), false);
                });
            }
        }).start();
    }

    private void setGenerating(boolean generating) {
        generateBtn.setDisable(generating);
        generateBtn.setText(generating ? "Génération en cours…" : "  Générer une paire de clés");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  LOAD & RENDER DEVICES
    // ═══════════════════════════════════════════════════════════════════

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
                        Type type = new TypeToken<List<UserDevice>>(){}.getType();
                        List<UserDevice> devices = JsonUtils.fromJson(
                                JsonUtils.toJson(resp.getData()), type);
                        registeredDevices = devices != null ? devices : new ArrayList<>();
                        renderDevices();
                        updateStatus();
                    } else {
                        addItalic(devicesContainer, "Impossible de charger les appareils.", TEXT_MUTED);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    devicesContainer.getChildren().clear();
                    addItalic(devicesContainer, "Erreur réseau : " + e.getMessage(), ERROR);
                });
            }
        }).start();
    }

    private void renderDevices() {
        devicesContainer.getChildren().clear();

        if (registeredDevices.isEmpty()) {
            addItalic(devicesContainer, "Aucun appareil enregistré pour ce compte.", TEXT_MUTED);
            return;
        }

        // Sort: this device first, then others
        List<UserDevice> thisDevices = new ArrayList<>();
        List<UserDevice> others      = new ArrayList<>();
        for (UserDevice d : registeredDevices) {
            if (thisDeviceName.equals(d.getDeviceName())) thisDevices.add(d);
            else                                           others.add(d);
        }

        if (!thisDevices.isEmpty()) {
            Label sectionLbl = sectionLabel("CET APPAREIL");
            devicesContainer.getChildren().add(sectionLbl);
            for (UserDevice d : thisDevices)
                devicesContainer.getChildren().add(buildDeviceRow(d, true));
        }

        if (!others.isEmpty()) {
            Label sectionLbl = sectionLabel("AUTRES APPAREILS");
            devicesContainer.getChildren().add(sectionLbl);
            for (UserDevice d : others)
                devicesContainer.getChildren().add(buildDeviceRow(d, false));
        }
    }

    private VBox buildDeviceRow(UserDevice d, boolean isCurrentDevice) {
        VBox row = new VBox(6);
        row.setPadding(new Insets(12, 14, 12, 14));
        row.setMaxWidth(Double.MAX_VALUE);

        String rowBg = isCurrentDevice
                ? "-fx-background-color:#EEF6ED;-fx-border-color:#A7D7B2;-fx-border-width:1;"
                : "-fx-background-color:" + BG + ";-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;";
        row.setStyle(rowBg + "-fx-background-radius:0;");

        // ── Top row: icon + name + badge + revoke button ──────────────────
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        // Device avatar
        Circle avatar = new Circle(18);
        avatar.setFill(Color.web(isCurrentDevice ? "#D1FAE5" : CARD_BG));
        FontIcon devIcon = new FontIcon(isCurrentDevice ? Feather.MONITOR : Feather.SMARTPHONE);
        devIcon.setIconSize(14);
        devIcon.setIconColor(Color.web(isCurrentDevice ? SUCCESS_CLR : TEXT_MUTED));
        StackPane avatarPane = new StackPane(avatar, devIcon);
        avatarPane.setPrefSize(36, 36);

        // Device name
        Label nameLbl = new Label(d.getDeviceName());
        nameLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_MAIN + ";");
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        // "This device" badge
        if (isCurrentDevice) {
            Label thisBadge = new Label("Cet appareil");
            thisBadge.setStyle(
                    "-fx-font-size:10px;-fx-font-weight:bold;" +
                            "-fx-text-fill:" + SUCCESS_CLR + ";" +
                            "-fx-background-color:#D1FAE5;" +
                            "-fx-background-radius:20;-fx-padding:2 8 2 8;"
            );
            top.getChildren().addAll(avatarPane, nameLbl, thisBadge);
        } else {
            top.getChildren().addAll(avatarPane, nameLbl);
        }

        // Revoke button
        Button revokeBtn = new Button("Révoquer");
        FontIcon revokeIcon = new FontIcon(Feather.TRASH_2);
        revokeIcon.setIconSize(12); revokeIcon.setIconColor(Color.web(ERROR));
        revokeBtn.setGraphic(revokeIcon);
        revokeBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + ERROR + ";" +
                        "-fx-font-size:12px;-fx-cursor:hand;" +
                        "-fx-border-color:" + ERROR + ";-fx-border-radius:6;-fx-border-width:1;" +
                        "-fx-padding:3 10 3 10;"
        );
        revokeBtn.setOnMouseEntered(e -> revokeBtn.setStyle(
                "-fx-background-color:#FEE2E2;-fx-text-fill:" + ERROR + ";" +
                        "-fx-font-size:12px;-fx-cursor:hand;" +
                        "-fx-border-color:" + ERROR + ";-fx-border-radius:6;-fx-border-width:1;" +
                        "-fx-padding:3 10 3 10;"
        ));
        revokeBtn.setOnMouseExited(e -> revokeBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + ERROR + ";" +
                        "-fx-font-size:12px;-fx-cursor:hand;" +
                        "-fx-border-color:" + ERROR + ";-fx-border-radius:6;-fx-border-width:1;" +
                        "-fx-padding:3 10 3 10;"
        ));
        revokeBtn.setOnAction(e -> confirmRevoke(d));
        top.getChildren().add(revokeBtn);

        // ── Details row: fingerprint + date ───────────────────────────────
        HBox details = new HBox(16);
        details.setAlignment(Pos.CENTER_LEFT);
        details.setPadding(new Insets(0, 0, 0, 46)); // indent past avatar

        Label fpLbl = new Label(d.getShortFingerprint());
        fpLbl.setStyle("-fx-font-size:11px;-fx-font-family:'Courier New',monospace;-fx-text-fill:" + TEXT_MUTED + ";");

        String dateStr = d.getCreatedAt() != null ? "Créé le " + d.getCreatedAt().format(FMT) : "";
        Label dateLbl = new Label(dateStr);
        dateLbl.setStyle("-fx-font-size:11px;-fx-text-fill:" + TEXT_MUTED + ";");

        String lastUsed = d.getLastUsedAt() != null ? "Dernière utilisation : " + d.getLastUsedAt().format(FMT) : "Jamais utilisé";
        Label usedLbl = new Label(lastUsed);
        usedLbl.setStyle("-fx-font-size:11px;-fx-text-fill:" + TEXT_MUTED + ";");

        details.getChildren().addAll(fpLbl, dateLbl, usedLbl);

        row.getChildren().addAll(top, details);
        return row;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  REVOKE
    // ═══════════════════════════════════════════════════════════════════

    private void confirmRevoke(UserDevice device) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.DECORATED);
        dialog.setResizable(false);
        dialog.setTitle("Révoquer l'appareil");

        FontIcon trashIcon = new FontIcon(Feather.TRASH_2);
        trashIcon.setIconSize(26); trashIcon.setIconColor(Color.web(ERROR));
        StackPane iconCircle = new StackPane(trashIcon);
        iconCircle.setPrefSize(56, 56); iconCircle.setMinSize(56, 56); iconCircle.setMaxSize(56, 56);
        iconCircle.setStyle("-fx-background-color:#FEE2E2;-fx-background-radius:28;");

        Label titleLbl = new Label("Révoquer cet appareil ?");
        titleLbl.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_MAIN + ";");
        Label nameLbl = new Label("\"" + device.getDeviceName() + "\"");
        nameLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + PRIMARY + ";");
        Label fpLbl = new Label(device.getShortFingerprint());
        fpLbl.setStyle("-fx-font-size:12px;-fx-font-family:'Courier New',monospace;-fx-text-fill:" + TEXT_MUTED + ";");
        Label warnLbl = new Label("Cet appareil ne pourra plus s'authentifier par clé.");
        warnLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_MUTED + ";");

        VBox textBox = new VBox(6, titleLbl, nameLbl, fpLbl, warnLbl);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox contentBox = new HBox(18, iconCircle, textBox);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setPadding(new Insets(24, 28, 16, 28));
        contentBox.setStyle("-fx-background-color:" + BG + ";");

        Button cancelBtn = outlineBtn("Annuler");
        cancelBtn.setPrefWidth(110);
        cancelBtn.setOnAction(e -> dialog.close());

        Button confirmBtn = dangerBtn("Révoquer");
        confirmBtn.setPrefWidth(120);
        confirmBtn.setOnAction(e -> {
            dialog.close();
            revokeDevice(device);
        });

        HBox btnBar = new HBox(12, cancelBtn, confirmBtn);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(14, 28, 20, 28));
        btnBar.setStyle("-fx-background-color:" + CARD_BG + ";-fx-border-color:" + BORDER + ";-fx-border-width:1 0 0 0;");

        VBox root = new VBox(0, contentBox, btnBar);
        root.setStyle("-fx-background-color:" + BG + ";");
        dialog.setScene(new Scene(root));
        dialog.showAndWait();
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
                        showFeedback("✅ Appareil révoqué.", true);
                        loadDevices();
                    } else {
                        showFeedback("❌ Erreur lors de la révocation.", false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showFeedback("❌ Erreur réseau : " + e.getMessage(), false));
            }
        }).start();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  STATUS
    // ═══════════════════════════════════════════════════════════════════

    private void updateStatus() {
        boolean hasLocal = registeredDevices.stream()
                .anyMatch(d -> KeyPairManager.keyFileExists(d.getDeviceName()));

        if (hasLocal) {
            statusDot.setStyle("-fx-font-size:14px;-fx-text-fill:" + SUCCESS_CLR + ";");
            statusLabel.setText("CONFIGURÉ");
            statusLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + SUCCESS_CLR + ";");
        } else if (!registeredDevices.isEmpty()) {
            statusDot.setStyle("-fx-font-size:14px;-fx-text-fill:" + WARN_CLR + ";");
            statusLabel.setText("CLÉ DISTANTE UNIQUEMENT");
            statusLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + WARN_CLR + ";");
        } else {
            statusDot.setStyle("-fx-font-size:14px;-fx-text-fill:" + ERROR + ";");
            statusLabel.setText("NON CONFIGURÉ");
            statusLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + ERROR + ";");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private VBox card() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20, 22, 20, 22));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color:" + CARD_BG + ";" +
                        "-fx-background-radius:14;-fx-border-radius:14;" +
                        "-fx-border-color:" + BORDER + ";-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(100,60,30,0.08),12,0,0,2);"
        );
        return card;
    }

    private Label bold(String text, int size) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:" + size + "px;-fx-font-weight:bold;-fx-text-fill:" + PRIMARY + ";-fx-font-family:'Segoe UI Semibold','Segoe UI',sans-serif;");
        return l;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setPadding(new Insets(10, 0, 4, 0));
        l.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_MUTED + ";-fx-letter-spacing:1;");
        return l;
    }

    private Button primaryBtn(String text, Feather icon) {
        Button btn = new Button(text);
        FontIcon fi = new FontIcon(icon); fi.setIconSize(15); fi.setIconColor(Color.web(BG));
        btn.setGraphic(fi);
        String base = "-fx-background-color:" + PRIMARY + ";-fx-text-fill:" + BG + ";-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:10;-fx-padding:10 20 10 20;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(PRIMARY, PRIMARY_L)));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private Button outlineBtn(String text) {
        Button btn = new Button(text);
        String base = "-fx-background-color:transparent;-fx-text-fill:" + PRIMARY + ";-fx-border-color:" + BORDER + ";-fx-border-radius:9;-fx-font-size:13px;-fx-padding:9 20 9 20;-fx-cursor:hand;-fx-border-width:1.5;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace("transparent", CARD_BG)));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private Button dangerBtn(String text) {
        Button btn = new Button(text);
        String base = "-fx-background-color:" + ERROR + ";-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:9;-fx-padding:9 20 9 20;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(ERROR, "#A93226")));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private Button warnBtn(String text) {
        Button btn = new Button(text);
        String base = "-fx-background-color:" + WARN_CLR + ";-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:9;-fx-padding:9 20 9 20;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(WARN_CLR, "#B45309")));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private void addItalic(VBox container, String text, String color) {
        Label l = new Label(text);
        l.setPadding(new Insets(8, 0, 4, 0));
        l.setStyle("-fx-font-size:13px;-fx-text-fill:" + color + ";-fx-font-style:italic;");
        container.getChildren().add(l);
    }

    private void showFeedback(String msg, boolean success) {
        feedbackLabel.setText(msg);
        feedbackLabel.setStyle("-fx-font-size:12px;-fx-text-fill:" + (success ? SUCCESS_CLR : ERROR) + ";");
        feedbackLabel.setVisible(true);
    }

    /** Returns the hostname of this machine (used as device name). */
    private String resolveDeviceName() {
        try {
            String host = java.net.InetAddress.getLocalHost().getHostName();
            if (host != null && !host.isBlank() && !host.equalsIgnoreCase("localhost")) return host;
        } catch (Exception ignored) {}
        return System.getProperty("user.name", "Device") + "-PC";
    }
}