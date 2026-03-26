package com.chrionline.clientmodule.client.ui.views;

import com.chrionline.core.interfaces.ViewManager;
import com.chrionline.core.theme.AppTheme;
import com.chrionline.core.utils.JsonUtils;
import com.chrionline.network.protocol.AppRequest;
import com.chrionline.network.protocol.AppResponse;
import com.chrionline.network.tcp.TCPClient;
import com.google.gson.Gson;
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
import java.util.*;
import java.util.stream.Collectors;

public class AdminUtilisateursView extends BorderPane {

    private final TCPClient           client;
    @SuppressWarnings("unused")
    private final Map<String, Object> userData;
    @SuppressWarnings("unused")
    private final ViewManager         viewManager;
    @SuppressWarnings("unused")
    private final AdminView           adminView;

    // ── State ──────────────────────────────────────────────────────────────────
    // Only clients are shown — admins are excluded from the list
    private List<Map<String, Object>> allClients      = new ArrayList<>();
    private List<Map<String, Object>> filteredClients = new ArrayList<>();

    private String searchText   = "";
    private String filterStatut = "Tous";
    private String sortBy       = "nom";

    // ── UI refs ────────────────────────────────────────────────────────────────
    private VBox  tableContainer;
    private Label totalLabel;

    public AdminUtilisateursView(TCPClient client, Map<String, Object> userData,
                                 ViewManager viewManager, AdminView adminView) {
        this.client      = client;
        this.userData    = userData;
        this.viewManager = viewManager;
        this.adminView   = adminView;

        setBackground(new Background(new BackgroundFill(
                Color.web(AppTheme.BG), CornerRadii.EMPTY, Insets.EMPTY)));
        buildUI();
        chargerUtilisateurs();
    }

    // ─── Build UI ──────────────────────────────────────────────────────────────

    private void buildUI() {
        ScrollPane scroll = new ScrollPane(buildContent());
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle(
                "-fx-background-color:" + AppTheme.BG + ";" +
                        "-fx-background:"       + AppTheme.BG + ";" +
                        "-fx-border-color:transparent;"
        );
        setCenter(scroll);
    }

    private VBox buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(36, 40, 40, 40));
        content.setBackground(new Background(new BackgroundFill(
                Color.web(AppTheme.BG), CornerRadii.EMPTY, Insets.EMPTY)));

        // Header
        VBox titreBox = new VBox(4);
        Label titre = new Label("Gestion des Clients");
        titre.setStyle(
                "-fx-font-size:32px;-fx-font-weight:bold;" +
                        "-fx-text-fill:" + AppTheme.PRIMARY + ";" +
                        "-fx-font-family:'Segoe UI Semibold','Segoe UI',sans-serif;"
        );
        totalLabel = new Label("Chargement…");
        totalLabel.setStyle("-fx-font-size:13px;-fx-text-fill:" + AppTheme.TEXT_MUTED + ";");
        titreBox.getChildren().addAll(titre, totalLabel);

        // Toolbar
        HBox toolbar = buildToolbar();

        // Table
        tableContainer = new VBox(0);
        tableContainer.setMaxWidth(Double.MAX_VALUE);
        tableContainer.setStyle(
                "-fx-background-color:" + AppTheme.CARD_BG + ";" +
                        "-fx-background-radius:14;-fx-border-radius:14;" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(100,60,30,0.08),16,0,0,3);"
        );

        content.getChildren().addAll(titreBox, toolbar, tableContainer);
        return content;
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);

        String fs =
                "-fx-background-color:" + AppTheme.CARD_BG + ";" +
                        "-fx-border-color:"     + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius:9;-fx-background-radius:9;-fx-border-width:1.5;" +
                        "-fx-padding:10 14 10 14;-fx-font-size:13px;" +
                        "-fx-text-fill:"        + AppTheme.TEXT_MAIN + ";" +
                        "-fx-prompt-text-fill:" + AppTheme.TEXT_MUTED + ";";

        // Search
        TextField searchField = new TextField();
        searchField.setPromptText("🔍  Rechercher par nom ou email…");
        searchField.setStyle(fs);
        searchField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((obs, o, n) -> {
            searchText = n.toLowerCase().trim();
            appliquerFiltres();
        });

        // Statut filter
        ComboBox<String> statutFilter = fixedCombo(
                new String[]{"Tous les statuts", "actif", "inactif"},
                new String[]{"Tous les statuts", "Actif", "Inactif"},
                fs, 170);
        statutFilter.setOnAction(e -> {
            String v = statutFilter.getValue();
            filterStatut = (v == null || v.equals("Tous les statuts")) ? "Tous" : v;
            appliquerFiltres();
        });

        // Sort
        ComboBox<String> sortBox = new ComboBox<>();
        sortBox.getItems().addAll("Nom", "Email", "Statut");
        sortBox.setValue("Nom");
        sortBox.setPrefWidth(150); sortBox.setMinWidth(150); sortBox.setMaxWidth(150);
        sortBox.setStyle(fs);
        sortBox.setOnAction(e -> {
            sortBy = switch (sortBox.getValue()) {
                case "Email"  -> "email";
                case "Statut" -> "statut";
                default       -> "nom";
            };
            appliquerFiltres();
        });

        // Reset
        Button resetBtn = new Button("✕  Réinitialiser");
        resetBtn.setStyle(
                "-fx-background-color:transparent;" +
                        "-fx-text-fill:" + AppTheme.TEXT_MUTED + ";" +
                        "-fx-font-size:13px;-fx-cursor:hand;" +
                        "-fx-padding:10 14 10 14;" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius:9;-fx-border-width:1.5;"
        );
        resetBtn.setOnAction(e -> {
            searchField.clear();
            statutFilter.setValue("Tous les statuts");
            sortBox.setValue("Nom");
            searchText = ""; filterStatut = "Tous"; sortBy = "nom";
            appliquerFiltres();
        });

        bar.getChildren().addAll(searchField, statutFilter, sortBox, resetBtn);
        return bar;
    }

    // ─── Table ─────────────────────────────────────────────────────────────────

    private HBox buildTableHeader() {
        HBox h = new HBox();
        h.setPadding(new Insets(13, 24, 13, 24));
        h.setMaxWidth(Double.MAX_VALUE);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setStyle(
                "-fx-background-color:" + AppTheme.TOGGLE_INACTIVE + ";" +
                        "-fx-background-radius:13 13 0 0;" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-width:0 0 1 0;"
        );
        Label hUser = hCell("Client");
        HBox.setHgrow(hUser, Priority.ALWAYS);
        hUser.setMaxWidth(Double.MAX_VALUE);
        h.getChildren().addAll(
                hUser,
                hCell("Email",   220),
                hCell("Statut",  200),
                hCell("Actions", 110)
        );
        return h;
    }

    private HBox buildClientRow(Map<String, Object> user, boolean isEven) {
        HBox row = new HBox();
        row.setPadding(new Insets(14, 24, 14, 24));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        String bg    = isEven ? AppTheme.CARD_BG : "#F8F0E8";
        String hover = "#EDD9C8";
        applyBg(row, bg);
        row.setStyle("-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-width:0 0 1 0;");
        row.setOnMouseEntered(e -> applyBg(row, hover));
        row.setOnMouseExited(e  -> applyBg(row, bg));

        String  nom    = str(user.get("nom"));
        String  prenom = str(user.get("prenom"));
        String  email  = str(user.get("email"));
        boolean actif  = "actif".equals(str(user.get("statut")));

        // ── Avatar + name (no ID) ─────────────────────────────────────────
        String initiales =
                (prenom.isEmpty() ? "?" : String.valueOf(prenom.charAt(0)).toUpperCase()) +
                        (nom.isEmpty()    ? "?" : String.valueOf(nom.charAt(0)).toUpperCase());

        Circle circle = new Circle(18);
        circle.setFill(Color.web("#B08968"));
        Label initLbl = new Label(initiales);
        initLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:white;");
        StackPane avatar = new StackPane(circle, initLbl);
        avatar.setPrefSize(36, 36);

        Label fullName = new Label(prenom + " " + nom);
        fullName.setStyle(
                "-fx-font-size:13px;-fx-font-weight:600;" +
                        "-fx-text-fill:" + AppTheme.TEXT_MAIN + ";");

        HBox userCell = new HBox(10, avatar, fullName);
        userCell.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(userCell, Priority.ALWAYS);
        userCell.setMaxWidth(Double.MAX_VALUE);

        // ── Email ────────────────────────────────────────────────────────
        Label emailLbl = new Label(email);
        emailLbl.setPrefWidth(220); emailLbl.setMinWidth(220);
        emailLbl.setStyle("-fx-font-size:13px;-fx-text-fill:" + AppTheme.TEXT_MUTED + ";");

        // ── Statut: badge + toggle ────────────────────────────────────────
        Label statutBadge = makeBadge(
                actif ? "Actif" : "Inactif",
                actif ? "#166534" : AppTheme.ERROR_COLOR,
                actif ? "#DCFCE7" : "#FEE2E2");

        ToggleButton toggleBtn = new ToggleButton(actif ? "ON" : "OFF");
        toggleBtn.setSelected(actif);
        styleToggle(toggleBtn, actif);
        toggleBtn.setOnAction(e -> handleToggleStatut(user, toggleBtn, statutBadge));

        HBox statutBox = new HBox(10, statutBadge, toggleBtn);
        statutBox.setPrefWidth(200); statutBox.setMinWidth(200);
        statutBox.setAlignment(Pos.CENTER_LEFT);

        // ── Actions: custom notification only ────────────────────────────
        Button notifBtn = iconBtn(Feather.SEND, AppTheme.PRIMARY, "#E6CCB2",
                "Envoyer une notification personnalisée");
        notifBtn.setOnAction(e -> ouvrirDialogNotifPersonnalisee(user));

        HBox actBox = new HBox(notifBtn);
        actBox.setPrefWidth(110); actBox.setMinWidth(110);
        actBox.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(userCell, emailLbl, statutBox, actBox);
        return row;
    }

    // ─── Toggle statut + automatic predefined notification ────────────────────

    private void handleToggleStatut(Map<String, Object> user,
                                    ToggleButton toggleBtn,
                                    Label statutBadge) {
        boolean nowActive = toggleBtn.isSelected();
        String  action    = nowActive ? "unblockuser" : "blockuser";
        String  newStatut = nowActive ? "actif"       : "inactif";

        // Optimistic UI
        styleToggle(toggleBtn, nowActive);
        updateBadge(statutBadge,
                nowActive ? "Actif"   : "Inactif",
                nowActive ? "#166534" : AppTheme.ERROR_COLOR,
                nowActive ? "#DCFCE7" : "#FEE2E2");

        new Thread(() -> {
            try {
                // 1 — Update status via TCP
                Map<String, Object> payload = new HashMap<>();
                payload.put("id", ((Number) user.get("id")).intValue());

                AppRequest req = new AppRequest.Builder()
                        .controller("Auth").action(action)
                        .payload(JsonUtils.toJson(payload))
                        .authToken(client.getAuthToken())
                        .build();

                AppResponse resp = client.sendAndParse(req);

                if (resp != null && resp.isSuccess()) {
                    user.put("statut", newStatut);

                    // 2 — Send predefined UDP notification automatically
                    String notifTitle = nowActive
                            ? "Votre compte a été activé"
                            : "Votre compte a été désactivé";
                    String notifMsg = nowActive
                            ? "Bonjour " + str(user.get("prenom")) + ", votre compte ChriOnline est maintenant actif. Vous pouvez vous connecter et passer des commandes."
                            : "Bonjour " + str(user.get("prenom")) + ", votre compte a été temporairement désactivé. Contactez le support pour plus d'informations.";

                    envoyerNotification(user, nowActive ? "SUCCESS" : "WARNING",
                            notifTitle, notifMsg);

                    Platform.runLater(this::appliquerFiltres);

                } else {
                    // Revert
                    Platform.runLater(() -> {
                        toggleBtn.setSelected(!nowActive);
                        styleToggle(toggleBtn, !nowActive);
                        updateBadge(statutBadge,
                                !nowActive ? "Actif"   : "Inactif",
                                !nowActive ? "#166534" : AppTheme.ERROR_COLOR,
                                !nowActive ? "#DCFCE7" : "#FEE2E2");
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    toggleBtn.setSelected(!nowActive);
                    styleToggle(toggleBtn, !nowActive);
                });
            }
        }).start();
    }

    // ─── Custom notification dialog ────────────────────────────────────────────

    private void ouvrirDialogNotifPersonnalisee(Map<String, Object> user) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setResizable(false);
        stage.setTitle("Notification personnalisée");

        Label recipientLbl = new Label("👤  " +
                str(user.get("prenom")) + " " + str(user.get("nom")) +
                " — " + str(user.get("email")));
        recipientLbl.setStyle(
                "-fx-font-size:13px;-fx-text-fill:" + AppTheme.TEXT_MUTED + ";" +
                        "-fx-padding:10 14 10 14;" +
                        "-fx-background-color:" + AppTheme.FIELD_BG + ";" +
                        "-fx-background-radius:9;" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius:9;-fx-border-width:1.5;"
        );
        recipientLbl.setMaxWidth(Double.MAX_VALUE);
        recipientLbl.setWrapText(true);

        TextField titleField = new TextField();
        titleField.setPromptText("Ex: Offre spéciale pour vous !");
        titleField.setStyle(fieldStyle());
        titleField.setMaxWidth(Double.MAX_VALUE);

        TextArea msgArea = new TextArea();
        msgArea.setPromptText("Rédigez votre message ici…");
        msgArea.setPrefRowCount(5);
        msgArea.setWrapText(true);
        msgArea.setStyle(
                "-fx-background-color:" + AppTheme.FIELD_BG + ";" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius:9;-fx-background-radius:9;-fx-border-width:1.5;" +
                        "-fx-font-size:13px;-fx-text-fill:" + AppTheme.TEXT_MAIN + ";"
        );

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + AppTheme.ERROR_COLOR + ";");
        errorLbl.setVisible(false);

        VBox body = new VBox(14,
                fBox("Destinataire", recipientLbl),
                fBox("Titre *",      titleField),
                fBox("Message *",    msgArea),
                errorLbl
        );
        body.setPadding(new Insets(20, 28, 12, 28));

        FontIcon sendIcon = new FontIcon(Feather.SEND);
        sendIcon.setIconSize(18);
        sendIcon.setIconColor(Color.web(AppTheme.PRIMARY));
        Label headerLbl = new Label("  Notification personnalisée");
        headerLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:" + AppTheme.PRIMARY + ";");
        HBox formHeader = new HBox(6, sendIcon, headerLbl);
        formHeader.setPadding(new Insets(20, 28, 14, 28));
        formHeader.setStyle("-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-width:0 0 1 0;");

        Button cancelBtn = outlineBtn("Annuler");
        cancelBtn.setOnAction(e -> stage.close());

        Button sendBtn = primaryBtn("Envoyer");
        sendBtn.setOnAction(e -> {
            String t = titleField.getText().trim();
            String m = msgArea.getText().trim();
            if (t.isEmpty()) { errorLbl.setText("Le titre est obligatoire."); errorLbl.setVisible(true); return; }
            if (m.isEmpty()) { errorLbl.setText("Le message est obligatoire."); errorLbl.setVisible(true); return; }
            stage.close();
            envoyerNotification(user, "INFO", t, m);
        });

        HBox btnBar = new HBox(10, cancelBtn, sendBtn);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(14, 28, 20, 28));
        btnBar.setStyle(
                "-fx-background-color:" + AppTheme.CARD_BG + ";" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-width:1 0 0 0;");

        VBox root = new VBox(0, formHeader, body, btnBar);
        root.setStyle("-fx-background-color:" + AppTheme.BG + ";");
        root.setPrefWidth(460);

        stage.setScene(new Scene(root));
        stage.showAndWait();
    }

    // ─── Send UDP notification via server TCP ──────────────────────────────────

    private void envoyerNotification(Map<String, Object> user, String type,
                                     String title, String message) {
        new Thread(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("type",          type);
                payload.put("title",         title);
                payload.put("message",       message);
                payload.put("source",        "admin");
                payload.put("idUtilisateur", ((Number) user.get("id")).intValue());

                AppRequest req = new AppRequest.Builder()
                        .controller("Notification").action("sendnotification")
                        .payload(JsonUtils.toJson(payload))
                        .authToken(client.getAuthToken())
                        .build();

                client.sendAndParse(req);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    // ─── Filters & display ─────────────────────────────────────────────────────

    private void appliquerFiltres() {
        filteredClients = allClients.stream()
                .filter(u -> {
                    if (searchText.isEmpty()) return true;
                    return str(u.get("nom")).toLowerCase().contains(searchText)
                            || str(u.get("prenom")).toLowerCase().contains(searchText)
                            || str(u.get("email")).toLowerCase().contains(searchText);
                })
                .filter(u -> "Tous".equals(filterStatut) || filterStatut.equals(str(u.get("statut"))))
                .sorted((a, b) -> {
                    String va = str(a.getOrDefault(sortBy, a.getOrDefault("nom", "")));
                    String vb = str(b.getOrDefault(sortBy, b.getOrDefault("nom", "")));
                    return va.compareToIgnoreCase(vb);
                })
                .collect(Collectors.toList());

        afficherClients();
    }

    private void afficherClients() {
        tableContainer.getChildren().clear();
        tableContainer.getChildren().add(buildTableHeader());

        if (filteredClients.isEmpty()) {
            Label empty = new Label("Aucun client trouvé");
            empty.setStyle("-fx-font-size:15px;-fx-text-fill:" + AppTheme.TEXT_MUTED + ";-fx-padding:40;");
            tableContainer.getChildren().add(empty);
            totalLabel.setText("0 client");
            return;
        }

        for (int i = 0; i < filteredClients.size(); i++) {
            tableContainer.getChildren().add(buildClientRow(filteredClients.get(i), i % 2 == 0));
        }

        long actifs   = filteredClients.stream().filter(u -> "actif".equals(str(u.get("statut")))).count();
        long inactifs = filteredClients.size() - actifs;
        String lbl = filteredClients.size() + " client" + (filteredClients.size() > 1 ? "s" : "");
        if (filteredClients.size() < allClients.size())
            lbl += " (filtrés sur " + allClients.size() + ")";
        lbl += "  ·  " + actifs   + " actif"   + (actifs   > 1 ? "s" : "")
                + "  ·  " + inactifs + " inactif" + (inactifs > 1 ? "s" : "");
        totalLabel.setText(lbl);
    }

    // ─── Network ───────────────────────────────────────────────────────────────

    private void chargerUtilisateurs() {
        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("Auth").action("listusers")
                        .authToken(client.getAuthToken()).build();
                AppResponse resp = client.sendAndParse(req);
                Platform.runLater(() -> {
                    if (resp != null && resp.isSuccess()) {
                        Type type = new TypeToken<List<Map<String, Object>>>() {}.getType();
                        List<Map<String, Object>> all =
                                new Gson().fromJson(new Gson().toJson(resp.getData()), type);
                        // Keep only clients — filter out admins
                        allClients = all != null
                                ? all.stream()
                                .filter(u -> "client".equals(str(u.get("role"))))
                                .collect(Collectors.toList())
                                : new ArrayList<>();
                        filteredClients = new ArrayList<>(allClients);
                        appliquerFiltres();
                    }
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    // ─── UI helpers ────────────────────────────────────────────────────────────

    private Label hCell(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" + AppTheme.TEXT_MUTED + ";");
        return l;
    }

    private Label hCell(String t, double w) {
        Label l = hCell(t); l.setPrefWidth(w); l.setMinWidth(w); return l;
    }

    private Label makeBadge(String text, String textColor, String bg) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-text-fill:" + textColor + ";" +
                        "-fx-background-color:" + bg + ";" +
                        "-fx-background-radius:20;-fx-padding:3 10 3 10;"
        );
        return l;
    }

    private void updateBadge(Label b, String text, String textColor, String bg) {
        b.setText(text);
        b.setStyle(
                "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-text-fill:" + textColor + ";" +
                        "-fx-background-color:" + bg + ";" +
                        "-fx-background-radius:20;-fx-padding:3 10 3 10;"
        );
    }

    private void styleToggle(ToggleButton btn, boolean on) {
        btn.setText(on ? "ON" : "OFF");
        btn.setStyle(
                "-fx-background-color:" + (on ? AppTheme.PRIMARY : AppTheme.FIELD_BORDER) + ";" +
                        "-fx-background-radius:20;-fx-padding:4 14 4 14;" +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-text-fill:white;-fx-cursor:hand;"
        );
    }

    private Button iconBtn(Feather icon, String color, String hoverBg, String tip) {
        Button btn = new Button();
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(15); fi.setIconColor(Color.web(color));
        btn.setGraphic(fi);
        btn.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:7;-fx-background-radius:8;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color:" + hoverBg + ";-fx-cursor:hand;-fx-padding:7;-fx-background-radius:8;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:7;-fx-background-radius:8;"));
        btn.setTooltip(new Tooltip(tip));
        return btn;
    }

    private Button primaryBtn(String label) {
        Button btn = new Button(label);
        btn.setPrefWidth(130);
        String s = "-fx-background-color:" + AppTheme.PRIMARY + ";-fx-text-fill:" + AppTheme.BG + ";" +
                "-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-background-radius:9;-fx-padding:9 20 9 20;-fx-cursor:hand;";
        btn.setStyle(s);
        btn.setOnMouseEntered(e -> btn.setStyle(s.replace(AppTheme.PRIMARY, AppTheme.PRIMARY_LIGHT)));
        btn.setOnMouseExited(e  -> btn.setStyle(s));
        return btn;
    }

    private Button outlineBtn(String label) {
        Button btn = new Button(label);
        btn.setPrefWidth(110);
        btn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + AppTheme.PRIMARY + ";" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-radius:9;" +
                        "-fx-font-size:13px;-fx-padding:9 20 9 20;-fx-cursor:hand;-fx-border-width:1.5;");
        return btn;
    }

    private VBox fBox(String labelTxt, javafx.scene.Node ctrl) {
        Label l = new Label(labelTxt);
        l.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + AppTheme.PRIMARY + ";");
        return new VBox(6, l, ctrl);
    }

    private String fieldStyle() {
        return  "-fx-background-color:" + AppTheme.FIELD_BG + ";" +
                "-fx-border-color:"     + AppTheme.FIELD_BORDER + ";" +
                "-fx-border-radius:9;-fx-background-radius:9;-fx-border-width:1.5;" +
                "-fx-padding:10 14 10 14;-fx-font-size:13px;" +
                "-fx-text-fill:"        + AppTheme.TEXT_MAIN + ";" +
                "-fx-prompt-text-fill:" + AppTheme.TEXT_MUTED + ";";
    }

    private ComboBox<String> fixedCombo(String[] values, String[] labels, String style, double width) {
        ComboBox<String> c = new ComboBox<>();
        c.getItems().addAll(values);
        c.setValue(values[0]);
        c.setPrefWidth(width); c.setMinWidth(width); c.setMaxWidth(width);
        c.setStyle(style);
        c.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(labels[0]); }
                else {
                    int idx = Arrays.asList(values).indexOf(item);
                    setText(idx >= 0 ? labels[idx] : item);
                }
                setStyle("-fx-text-fill:" + AppTheme.TEXT_MAIN + ";-fx-background-color:transparent;");
            }
        });
        c.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else {
                    int idx = Arrays.asList(values).indexOf(item);
                    setText(idx >= 0 ? labels[idx] : item);
                }
            }
        });
        return c;
    }

    private void applyBg(HBox node, String color) {
        node.setBackground(new Background(new BackgroundFill(
                Color.web(color), CornerRadii.EMPTY, Insets.EMPTY)));
    }

    private String str(Object o) { return o == null ? "" : String.valueOf(o); }
}