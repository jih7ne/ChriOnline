package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.core.enums.StatutCommande;
import com.chrionline.chrionline.core.interfaces.ViewManager;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.network.tcp.TCPClient;
import com.chrionline.chrionline.shared.models.OrderSummary;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminCommandesView extends BorderPane {

    private final TCPClient client;

    private List<OrderSummary> allOrders      = new ArrayList<>();
    private List<OrderSummary> filteredOrders = new ArrayList<>();

    private final VBox  tableContainer = new VBox(0);
    private final Label totalLabel     = new Label();

    private TextField       searchField;
    private ComboBox<String> statusFilter;
    private ComboBox<String> sortCombo;

    // ── Shared styles (same palette as Produits) ──────────────────────────────────
    private static final String BG            = "#EDE0D4";
    private static final String CARD_BG       = "#F5EBE0";
    private static final String TXT_DARK      = "#3B1F0E";
    private static final String TXT_PRIMARY   = "#7F5539";
    private static final String TXT_MUTED     = "#9C6644";
    private static final String TXT_MID       = "#5C3D20";
    private static final String BORDER        = "#DDB892";

    private static final String FS =
            "-fx-background-color:#F5EBE0;" +
                    "-fx-border-color:#DDB892;" +
                    "-fx-border-radius:9;-fx-background-radius:9;-fx-border-width:1.5;" +
                    "-fx-padding:0 16 0 16;-fx-font-size:13px;" +
                    "-fx-text-fill:#3B1F0E;-fx-prompt-text-fill:#9C6644;";

    public AdminCommandesView(TCPClient client, ViewManager viewManager) {
        this.client = client;
        setBackground(new Background(new BackgroundFill(Color.web(BG), CornerRadii.EMPTY, Insets.EMPTY)));
        buildUI();
        loadOrders();
    }

    // ── Build UI ──────────────────────────────────────────────────────────────────

    private void buildUI() {
        ScrollPane scrollPane = new ScrollPane(buildContent());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color:#EDE0D4;-fx-background:#EDE0D4;-fx-border-color:transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        setCenter(scrollPane);
    }

    private VBox buildContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(36, 40, 40, 40));
        content.setBackground(new Background(new BackgroundFill(Color.web(BG), CornerRadii.EMPTY, Insets.EMPTY)));
        content.setMaxWidth(Double.MAX_VALUE);

        // Header
        VBox titreBox = new VBox(4);
        Label titre = new Label("Gestion des Commandes");
        titre.setStyle("-fx-font-size:32px;-fx-font-weight:bold;-fx-text-fill:#7F5539;" +
                "-fx-font-family:'Segoe UI Semibold','Segoe UI',sans-serif;");
        totalLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#9C6644;");
        totalLabel.setText("Chargement...");
        titreBox.getChildren().addAll(titre, totalLabel);

        // Toolbar
        HBox toolbar = buildToolbar();

        // Table
        tableContainer.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);
        tableContainer.setStyle(
                "-fx-background-color:#F5EBE0;-fx-background-radius:14;-fx-border-radius:14;" +
                "-fx-border-color:#DDB892;-fx-border-width:1;" +
                "-fx-effect:dropshadow(gaussian,rgba(100,60,30,0.08),16,0,0,3);"
        );

        content.getChildren().addAll(titreBox, toolbar, tableContainer);
        return content;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────────

    private HBox buildToolbar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMaxWidth(Double.MAX_VALUE);

        // ── Champ de recherche ──────────────────────────────────────────────
        searchField = new TextField();
        searchField.setPromptText("🔍  Rechercher par ID, client, email…");
        searchField.setStyle(FS);
        searchField.setPrefHeight(44);
        searchField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((obs, o, n) -> applyFiltersAndSort());

        // ── Filtre statut ───────────────────────────────────────────────────
        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("Tous les statuts", "En attente", "Validée", "Annulée");
        statusFilter.setValue("Tous les statuts");
        statusFilter.setPrefWidth(210); statusFilter.setMinWidth(210); statusFilter.setMaxWidth(210);
        statusFilter.setPrefHeight(44);
        statusFilter.setStyle(FS);
        statusFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.equals("Tous les statuts")) {
                    setText("Tous les statuts");
                    setStyle("-fx-text-fill:#9C6644;-fx-background-color:transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill:#3B1F0E;-fx-background-color:transparent;");
                }
            }
        });
        statusFilter.setOnAction(e -> applyFiltersAndSort());

        // ── Tri ─────────────────────────────────────────────────────────────
        sortCombo = new ComboBox<>();
        sortCombo.setPromptText("Trier par…");
        sortCombo.getItems().addAll("Date ↓ (récent)", "Date ↑ (ancien)", "Total ↓", "Total ↑");
        sortCombo.setPrefWidth(210); sortCombo.setMinWidth(210); sortCombo.setMaxWidth(210);
        sortCombo.setPrefHeight(44);
        sortCombo.setStyle(FS);
        sortCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Trier par…");
                    setStyle("-fx-text-fill:#9C6644;-fx-background-color:transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill:#3B1F0E;-fx-background-color:transparent;");
                }
            }
        });
        sortCombo.setOnAction(e -> applyFiltersAndSort());

        // ── Bouton Réinitialiser ─────────────────────────────────────────────
        FontIcon xIcon = new FontIcon(Feather.X);
        xIcon.setIconSize(13);
        xIcon.setIconColor(Color.web(TXT_PRIMARY));

        Button resetBtn = new Button("  Réinitialiser");
        resetBtn.setGraphic(xIcon);
        resetBtn.setPrefHeight(44);
        String resetStyle =
                "-fx-background-color:transparent;" +
                        "-fx-text-fill:#9C6644;-fx-font-size:13px;" +
                        "-fx-cursor:hand;-fx-padding:0 20 0 16;" +
                        "-fx-border-color:#DDB892;" +
                        "-fx-border-radius:9;-fx-background-radius:9;" +
                        "-fx-border-width:1.5;";
        resetBtn.setStyle(resetStyle);
        resetBtn.setOnMouseEntered(e -> resetBtn.setStyle(
                resetStyle.replace("-fx-background-color:transparent;", "-fx-background-color:#F5EBE0;")));
        resetBtn.setOnMouseExited(e -> resetBtn.setStyle(resetStyle));
        resetBtn.setTooltip(new Tooltip("Réinitialiser les filtres"));
        resetBtn.setOnAction(e -> {
            searchField.clear();
            statusFilter.setValue("Tous les statuts");
            // Reset sortCombo complètement comme dans Produits
            sortCombo.getItems().clear();
            sortCombo.getItems().addAll("Date ↓ (récent)", "Date ↑ (ancien)", "Total ↓", "Total ↑");
            sortCombo.setPromptText("Trier par…");
            sortCombo.getSelectionModel().clearSelection();
            sortCombo.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Trier par…");
                        setStyle("-fx-text-fill:#9C6644;-fx-background-color:transparent;");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill:#3B1F0E;-fx-background-color:transparent;");
                    }
                }
            });
            applyFiltersAndSort();
        });

        bar.getChildren().addAll(searchField, statusFilter, sortCombo, resetBtn);
        return bar;
    }

    /** Utility: creates a button cell that shows a placeholder when nothing is selected */
    private ListCell<String> placeholderCell(String placeholder) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(placeholder);
                    setStyle("-fx-text-fill:#9C6644;-fx-background-color:transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill:#3B1F0E;-fx-background-color:transparent;");
                }
            }
        };
    }

    // ── Filter + Sort ─────────────────────────────────────────────────────────────

    private void applyFiltersAndSort() {
        String query     = searchField  != null ? searchField.getText().trim().toLowerCase()  : "";
        String statusSel = statusFilter != null ? statusFilter.getValue()                     : "Tous les statuts";
        String sortSel   = sortCombo    != null ? sortCombo.getValue()                        : "Date ↓ (récent)";

        List<OrderSummary> result = allOrders.stream().filter(o -> {
            boolean statusMatch = switch (statusSel == null ? "" : statusSel) {
                case "En attente" -> o.getStatus() == StatutCommande.EN_ATTENTE;
                case "Validée"    -> o.getStatus() == StatutCommande.VALIDEE;
                case "Annulée"    -> o.getStatus() == StatutCommande.ANNULEE;
                default           -> true;
            };
            if (!statusMatch) return false;
            if (query.isBlank()) return true;
            String uuid  = o.getUuid()     != null ? o.getUuid().toLowerCase()     : "";
            String name  = o.getUsername() != null ? o.getUsername().toLowerCase() : "";
            String email = o.getEmail()    != null ? o.getEmail().toLowerCase()    : "";
            String idStr = String.valueOf(o.getOrderId());
            return uuid.contains(query) || name.contains(query) || email.contains(query) || idStr.contains(query);
        }).collect(Collectors.toList());

        Comparator<OrderSummary> cmp = switch (sortSel == null ? "" : sortSel) {
            case "Date ↑ (ancien)" -> Comparator.comparing(
                    o -> o.getDate() != null ? o.getDate() : LocalDateTime.MIN);
            case "Total ↓" -> Comparator.comparing(
                    (OrderSummary o) -> o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO).reversed();
            case "Total ↑" -> Comparator.comparing(
                    o -> o.getTotal() != null ? o.getTotal() : BigDecimal.ZERO);
            default -> Comparator.comparing(
                    (OrderSummary o) -> o.getDate() != null ? o.getDate() : LocalDateTime.MIN).reversed();
        };
        result.sort(cmp);

        filteredOrders = result;
        renderTable();
    }

    // ── Table ─────────────────────────────────────────────────────────────────────

    private void renderTable() {
        tableContainer.getChildren().clear();
        tableContainer.getChildren().add(buildTableHeader());

        if (filteredOrders.isEmpty()) {
            Label empty = new Label("Aucune commande trouvée");
            empty.setStyle("-fx-font-size:15px;-fx-text-fill:#B08968;-fx-padding:40;");
            tableContainer.getChildren().add(empty);
            updateSubtitle();
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        for (int i = 0; i < filteredOrders.size(); i++)
            tableContainer.getChildren().add(buildRow(filteredOrders.get(i), i % 2 == 0, fmt));

        updateSubtitle();
    }

    private void updateSubtitle() {
        String lbl = filteredOrders.size() + " commande" + (filteredOrders.size() > 1 ? "s" : "");
        if (filteredOrders.size() < allOrders.size())
            lbl += " (filtrées sur " + allOrders.size() + ")";
        totalLabel.setText(lbl);
    }

    private HBox buildTableHeader() {
        HBox h = new HBox();
        h.setPadding(new Insets(14, 24, 14, 24));
        h.setMaxWidth(Double.MAX_VALUE);
        h.setStyle("-fx-background-color:#D4B896;-fx-background-radius:13 13 0 0;" +
                "-fx-border-radius:13 13 0 0;-fx-border-color:#C4A882;-fx-border-width:0 0 1 0;");
        h.setAlignment(Pos.CENTER_LEFT);

        Label hId     = hCell("ID Commande", 160);
        Label hClient = hCell("Client");      HBox.setHgrow(hClient, Priority.ALWAYS); hClient.setMaxWidth(Double.MAX_VALUE);
        Label hDate   = hCell("Date",   160);
        Label hTotal  = hCell("Total",  130);
        Label hStatut = hCell("Statut", 150);
        Label hAct    = hCell("Actions", 70);

        h.getChildren().addAll(hId, hClient, hDate, hTotal, hStatut, hAct);
        return h;
    }

    private Label hCell(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#7B5B3A;" +
                "-fx-font-family:'Segoe UI Semibold','Segoe UI',sans-serif;");
        return l;
    }

    private Label hCell(String t, double w) {
        Label l = hCell(t);
        l.setPrefWidth(w); l.setMinWidth(w);
        return l;
    }

    private HBox buildRow(OrderSummary order, boolean isEven, DateTimeFormatter fmt) {
        HBox row = new HBox();
        row.setPadding(new Insets(12, 24, 12, 24));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        String rowBg    = isEven ? CARD_BG : "#FBF6F2";
        String rowHover = "#EDD9C8";
        row.setBackground(new Background(new BackgroundFill(Color.web(rowBg), CornerRadii.EMPTY, Insets.EMPTY)));
        row.setStyle("-fx-border-color:#DDB892;-fx-border-width:0 0 1 0;");
        row.setOnMouseEntered(e -> row.setBackground(new Background(new BackgroundFill(Color.web(rowHover), CornerRadii.EMPTY, Insets.EMPTY))));
        row.setOnMouseExited(e  -> row.setBackground(new Background(new BackgroundFill(Color.web(rowBg),    CornerRadii.EMPTY, Insets.EMPTY))));

        // ── ID
        String idText = order.getUuid() != null
                ? order.getUuid().split("-")[0] + "-" + order.getOrderId()
                : "CH-" + order.getOrderId();
        Label idLabel = new Label(idText);
        idLabel.setStyle("-fx-font-size:13px;-fx-font-weight:600;-fx-text-fill:" + TXT_DARK + ";");
        idLabel.setPrefWidth(160); idLabel.setMinWidth(160);

        // ── Client
        VBox clientBox = new VBox(2);
        clientBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(clientBox, Priority.ALWAYS);
        Label clientName = new Label(order.getUsername() != null ? order.getUsername() : "Client Inconnu");
        clientName.setStyle("-fx-font-size:13px;-fx-font-weight:600;-fx-text-fill:" + TXT_DARK + ";");
        Label clientEmail = new Label(order.getEmail() != null ? order.getEmail() : "—");
        clientEmail.setStyle("-fx-font-size:12px;-fx-text-fill:" + TXT_MUTED + ";");
        clientBox.getChildren().addAll(clientName, clientEmail);

        // ── Date
        Label dateLabel = new Label(order.getDate() != null ? order.getDate().format(fmt) : "N/A");
        dateLabel.setStyle("-fx-font-size:13px;-fx-text-fill:" + TXT_MID + ";");
        dateLabel.setPrefWidth(160); dateLabel.setMinWidth(160);

        // ── Total
        Label totalLabel = new Label(String.format("%.2f MAD", order.getTotal() != null ? order.getTotal().doubleValue() : 0.0));
        totalLabel.setStyle("-fx-font-size:13px;-fx-font-weight:600;-fx-text-fill:" + TXT_MID + ";");
        totalLabel.setPrefWidth(130); totalLabel.setMinWidth(130);

        // ── Statut
        HBox statutBox = new HBox();
        statutBox.setPrefWidth(150); statutBox.setMinWidth(150);
        statutBox.setAlignment(Pos.CENTER_LEFT);

        if (order.getStatus() == StatutCommande.EN_ATTENTE) {
            // Editable combo for pending orders
            ComboBox<StatutCommande> combo = new ComboBox<>();
            combo.getItems().addAll(StatutCommande.values());
            combo.setValue(StatutCommande.EN_ATTENTE);
            combo.setCellFactory(lv -> new StatusListCell());
            combo.setButtonCell(new StatusListCell());
            combo.setStyle(
                "-fx-background-color:#FEF3C7;" +
                "-fx-border-color:#D97706;" +
                "-fx-border-radius:20;-fx-background-radius:20;-fx-border-width:1.5;"
            );
            combo.setPrefWidth(140); combo.setMaxWidth(140);
            combo.setOnAction(e -> {
                StatutCommande sel = combo.getValue();
                if (sel == null || sel == StatutCommande.EN_ATTENTE) return;
                if (sel != StatutCommande.VALIDEE && sel != StatutCommande.ANNULEE) {
                    combo.setValue(StatutCommande.EN_ATTENTE);
                    return;
                }
                updateOrderStatus(order.getOrderId(), sel);
            });
            statutBox.getChildren().add(combo);
        } else {
            // Static simple rounded badge
            Label badge = createStatusBadge(order.getStatus());
            statutBox.getChildren().add(badge);
        }

        // ── Actions
        HBox actBox = new HBox(6);
        actBox.setPrefWidth(70); actBox.setMinWidth(70);
        actBox.setAlignment(Pos.CENTER_LEFT);
        Button eyeBtn = iconBtn(Feather.EYE, TXT_PRIMARY, "#E6CCB2");
        eyeBtn.setOnAction(e -> showOrderDetailsDialog(
                order.getOrderId(), idText,
                clientName.getText(), clientEmail.getText(),
                order.getDate(), order.getTotal(), order.getStatus()));
        actBox.getChildren().add(eyeBtn);

        row.getChildren().addAll(idLabel, clientBox, dateLabel, totalLabel, statutBox, actBox);
        return row;
    }

    // ── Status badge / cell ───────────────────────────────────────────────────────

    private class StatusListCell extends ListCell<StatutCommande> {
        @Override
        protected void updateItem(StatutCommande item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setText(null); setGraphic(null); }
            else { setText(null); setGraphic(createStatusBadge(item)); }
        }
    }

    /**
     * Simple, compact rounded badge.
     * EN_ATTENTE = amber,  VALIDEE = green,  ANNULEE = red.
     * No icon prefix, short text.
     */
    private Label createStatusBadge(StatutCommande status) {
        String bgColor, textColor, text;
        switch (status) {
            case EN_ATTENTE -> { bgColor = "#FEF3C7"; textColor = "#92400E"; text = "En attente"; }
            case VALIDEE    -> { bgColor = "#D1FAE5"; textColor = "#065F46"; text = "Validée";    }
            case ANNULEE    -> { bgColor = "#FEE2E2"; textColor = "#991B1B"; text = "Annulée";    }
            default         -> { bgColor = "#F3F4F6"; textColor = "#374151"; text = status.name(); }
        }
        Label lbl = new Label(text);
        lbl.setStyle(
            "-fx-font-size:12px;-fx-font-weight:bold;" +
            "-fx-text-fill:" + textColor + ";" +
            "-fx-background-color:" + bgColor + ";" +
            "-fx-background-radius:20;" +
            "-fx-padding:4 12 4 12;"
        );
        return lbl;
    }

    // ── Icon button (same helper as Produits) ─────────────────────────────────────

    private Button iconBtn(Feather icon, String color, String hoverBg) {
        Button btn = new Button();
        FontIcon fi = new FontIcon(icon); fi.setIconSize(15); fi.setIconColor(Color.web(color));
        btn.setGraphic(fi);
        btn.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:6;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color:" + hoverBg + ";-fx-background-radius:7;-fx-cursor:hand;-fx-padding:6;"));
        btn.setOnMouseExited(e  -> btn.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:6;"));
        return btn;
    }

    // ── Order Detail Dialog ───────────────────────────────────────────────────────

    private void showOrderDetailsDialog(Long orderId, String displayId, String clientName, String clientEmail,
                                        LocalDateTime date, BigDecimal total, StatutCommande currentStatus) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);
        stage.setTitle("Détails de la commande");

        VBox root = new VBox(24);
        root.setStyle("-fx-background-color:" + BG + ";");
        root.setPadding(new Insets(24));
        root.setPrefWidth(500);

        Label title = new Label("Détails de la commande");
        title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#7F5539;" +
                "-fx-font-family:'Segoe UI Semibold','Segoe UI',sans-serif;");

        VBox infoBox = new VBox(8);
        Label infoTitle = new Label("Informations");
        infoTitle.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#7F5539;");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        String dateStr = date != null ? date.format(fmt) : "N/A";
        infoBox.getChildren().addAll(
            infoTitle,
            detailRow("Commande:", displayId),
            detailRow("Date:", dateStr),
            detailRowNode("Statut:", createStatusBadge(currentStatus))
        );

        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color:" + BORDER + ";");

        VBox clientBox = new VBox(4);
        Label clientTitle = new Label("Client");
        clientTitle.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#7F5539;");
        Label cName = new Label(clientName);
        cName.setStyle("-fx-font-size:13px;-fx-text-fill:" + TXT_DARK + ";-fx-font-weight:600;");
        Label cEmail = new Label(clientEmail);
        cEmail.setStyle("-fx-font-size:13px;-fx-text-fill:" + TXT_MUTED + ";");
        clientBox.getChildren().addAll(clientTitle, cName, cEmail);

        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color:" + BORDER + ";");

        VBox articlesBox = new VBox(12);
        Label articlesTitle = new Label("Articles");
        articlesTitle.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#7F5539;");
        Label loadingLignes = new Label("Chargement des articles…");
        loadingLignes.setStyle("-fx-text-fill:#9C6644;-fx-font-size:13px;");
        articlesBox.getChildren().addAll(articlesTitle, loadingLignes);

        Separator sep3 = new Separator();
        sep3.setStyle("-fx-background-color:" + BORDER + ";");

        HBox totalBox = new HBox();
        totalBox.setAlignment(Pos.CENTER_LEFT);
        Label totalLbl = new Label("Total");
        totalLbl.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#7F5539;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label totalVal = new Label(String.format("%.2f MAD", total != null ? total.doubleValue() : 0.0));
        totalVal.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#7F5539;");
        totalBox.getChildren().addAll(totalLbl, spacer, totalVal);

        root.getChildren().addAll(title, infoBox, sep1, clientBox, sep2, articlesBox, sep3, totalBox);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background: transparent; -fx-background-color:" + BG + ";");

        Scene scene = new Scene(sp, 520, 520);
        stage.setScene(scene);
        stage.show();

        fetchOrderLines(orderId, articlesBox, loadingLignes);
    }

    private HBox detailRow(String lbl, String val) {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(lbl);
        l.setStyle("-fx-font-size:13px;-fx-text-fill:#7F5539;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(val);
        v.setStyle("-fx-font-size:13px;-fx-font-weight:600;-fx-text-fill:" + TXT_DARK + ";");
        box.getChildren().addAll(l, sp, v);
        return box;
    }

    private HBox detailRowNode(String lbl, javafx.scene.Node node) {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(lbl);
        l.setStyle("-fx-font-size:13px;-fx-text-fill:#7F5539;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        box.getChildren().addAll(l, sp, node);
        return box;
    }

    // ── Network ───────────────────────────────────────────────────────────────────

    private void fetchOrderLines(Long idCommande, VBox articlesBox, Label loadingLabel) {
        new Thread(() -> {
            try {
                AppRequest request = new AppRequest.Builder()
                        .controller("Commande").action("details")
                        .payload(Map.of("idCommande", idCommande))
                        .build();
                AppResponse response = client.sendAndParse(request);

                Platform.runLater(() -> {
                    articlesBox.getChildren().remove(loadingLabel);
                    if (response != null && response.isSuccess()) {
                        Map<?, ?> result = response.getDataAs(Map.class);
                        if (result != null && result.containsKey("lignes")) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> lignes = (List<Map<String, Object>>) result.get("lignes");
                            for (Map<String, Object> ligne : lignes) {
                                String nom     = ligne.containsKey("nom_produit") ? (String) ligne.get("nom_produit") : "Produit Inconnu";
                                int    qte     = ((Number) ligne.get("quantite")).intValue();
                                double prix    = ((Number) ligne.get("prix_unitaire")).doubleValue();

                                HBox row = new HBox();
                                row.setAlignment(Pos.CENTER_LEFT);
                                VBox left = new VBox(2);
                                Label nameLbl = new Label(nom);
                                nameLbl.setStyle("-fx-font-size:13px;-fx-font-weight:600;-fx-text-fill:" + TXT_DARK + ";");
                                Label qtyLbl  = new Label("Qté: " + qte);
                                qtyLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + TXT_MUTED + ";");
                                left.getChildren().addAll(nameLbl, qtyLbl);

                                Region sp = new Region();
                                HBox.setHgrow(sp, Priority.ALWAYS);

                                Label pxLbl = new Label(String.format("%.2f MAD", prix * qte));
                                pxLbl.setStyle("-fx-font-size:13px;-fx-font-weight:600;-fx-text-fill:" + TXT_MID + ";");

                                row.getChildren().addAll(left, sp, pxLbl);
                                articlesBox.getChildren().add(row);
                            }
                        }
                    } else {
                        Label err = new Label("Erreur de chargement des articles");
                        err.setStyle("-fx-text-fill:#C0392B;-fx-font-size:13px;");
                        articlesBox.getChildren().add(err);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadOrders() {
        new Thread(() -> {
            try {
                AppRequest request = new AppRequest.Builder()
                        .controller("Admin").action("getAllOrders").build();
                AppResponse response = client.sendAndParse(request);

                if (response != null && response.isSuccess()) {
                    OrderSummary[] arr = response.getDataAs(OrderSummary[].class);
                    List<OrderSummary> fetched = arr != null
                            ? java.util.Arrays.asList(arr)
                            : new ArrayList<>();
                    Platform.runLater(() -> {
                        allOrders = fetched;
                        applyFiltersAndSort();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateOrderStatus(Long orderId, StatutCommande newStatus) {
        new Thread(() -> {
            try {
                AppRequest request = new AppRequest.Builder()
                        .controller("Admin").action("updateOrderStatus")
                        .payload(Map.of("idCommande", orderId, "statut", newStatus.name()))
                        .build();
                AppResponse response = client.sendAndParse(request);

                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) loadOrders();
                    else renderTable();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
