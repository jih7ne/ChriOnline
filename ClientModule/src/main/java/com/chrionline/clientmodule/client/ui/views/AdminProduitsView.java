package com.chrionline.clientmodule.client.ui.views;

import com.chrionline.core.interfaces.ViewManager;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.core.network.protocol.AppResponse;
import com.chrionline.network.tcp.TCPClient;
import com.chrionline.shared.models.*;
import com.chrionline.core.theme.AppTheme;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import javafx.stage.FileChooser;

public class AdminProduitsView extends BorderPane {

    private final TCPClient client;
    @SuppressWarnings("unused") private final Map<String, Object> userData;
    @SuppressWarnings("unused") private final ViewManager viewManager;
    @SuppressWarnings("unused") private final AdminView adminView;

    private VBox   tableContainer;
    private HBox   toolbar;
    private List<Produit>   produits        = new ArrayList<>();
    private List<Produit>   produitsFiltres = new ArrayList<>();
    private List<Categorie> categories      = new ArrayList<>();
    private Label totalLabel;

    private String  searchText        = "";
    private int     filterCategorieId = -1;
    private String  sortColumn        = "date";
    private boolean sortAsc           = false;
    private ComboBox<String> sortBox  = new ComboBox<>();

    public AdminProduitsView(TCPClient client, Map<String, Object> userData,
                             ViewManager viewManager, AdminView adminView) {
        this.client      = client;
        this.userData    = userData;
        this.viewManager = viewManager;
        this.adminView   = adminView;
        setBackground(new Background(new BackgroundFill(Color.web(AppTheme.BG), CornerRadii.EMPTY, Insets.EMPTY)));
        buildUI();
        chargerCategories(() -> chargerProduits());
    }

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
        content.setBackground(new Background(new BackgroundFill(Color.web(AppTheme.BG), CornerRadii.EMPTY, Insets.EMPTY)));
        content.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titreBox = new VBox(4);
        Label titre = new Label("Gestion des Produits");
        titre.setStyle("-fx-font-size:32px;-fx-font-weight:bold;-fx-text-fill:#7F5539;-fx-font-family:'Segoe UI Semibold','Segoe UI',sans-serif;");
        totalLabel = new Label("Chargement...");
        totalLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#9C6644;");
        titreBox.getChildren().addAll(titre, totalLabel);
        HBox.setHgrow(titreBox, Priority.ALWAYS);

        Button ajouterBtn = new Button("  Ajouter un produit");
        FontIcon plusIcon = new FontIcon(Feather.PLUS);
        plusIcon.setIconSize(17); plusIcon.setIconColor(Color.web(AppTheme.BG));
        ajouterBtn.setGraphic(plusIcon);
        String styleBtn = "-fx-background-color:#7F5539;-fx-text-fill:#EDE0D4;-fx-font-size:15px;-fx-font-weight:bold;-fx-background-radius:10;-fx-padding:14 24 14 24;-fx-cursor:hand;";
        ajouterBtn.setStyle(styleBtn);
        ajouterBtn.setOnMouseEntered(e -> ajouterBtn.setStyle(styleBtn.replace(AppTheme.PRIMARY, AppTheme.PRIMARY_LIGHT)));
        ajouterBtn.setOnMouseExited(e  -> ajouterBtn.setStyle(styleBtn));
        ajouterBtn.setOnAction(e -> showDialogAjouter());
        header.getChildren().addAll(titreBox, ajouterBtn);

        toolbar = buildToolbar();

        tableContainer = new VBox(0);
        tableContainer.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);
        tableContainer.setStyle(
                "-fx-background-color:#F5EBE0;-fx-background-radius:14;-fx-border-radius:14;" +
                        "-fx-border-color:#DDB892;-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(100,60,30,0.08),16,0,0,3);"
        );

        content.getChildren().addAll(header, toolbar, tableContainer);
        return content;
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMaxWidth(Double.MAX_VALUE);

        // Style commun pour tous les contrôles — hauteur unifiée via setPrefHeight
        String fs =
                "-fx-background-color:#F5EBE0;-fx-border-color:#DDB892;" +
                        "-fx-border-radius:9;-fx-background-radius:9;-fx-border-width:1.5;" +
                        "-fx-padding:0 16 0 16;-fx-font-size:13px;-fx-text-fill:#3B1F0E;" +
                        "-fx-prompt-text-fill:#9C6644;";

        // ── Champ de recherche ──────────────────────────────────────────────────
        TextField searchField = new TextField();
        // Placeholder précisant les deux champs de recherche
        searchField.setPromptText("🔍  Rechercher par nom ou description...");
        searchField.setStyle(fs);
        searchField.setPrefHeight(44);   // hauteur unifiée
        searchField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setText(searchText);
        searchField.textProperty().addListener((obs, o, n) -> {
            searchText = n.toLowerCase().trim();
            appliquerFiltres();
        });

        // ── Filtre par catégorie ────────────────────────────────────────────────
        ComboBox<String> catFilter = new ComboBox<>();
        catFilter.setPromptText("Toutes les catégories");
        catFilter.setPrefWidth(220);
        catFilter.setMinWidth(220);
        catFilter.setMaxWidth(220);
        catFilter.setPrefHeight(44);     // hauteur unifiée
        catFilter.setStyle(fs);
        catFilter.getItems().add("Toutes les catégories");
        for (Categorie c : categories) catFilter.getItems().add(c.getNom());
        catFilter.setValue("Toutes les catégories");
        catFilter.setButtonCell(new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.equals("Toutes les catégories")) {
                    setText("Toutes les catégories");
                    setStyle("-fx-text-fill:#9C6644;-fx-background-color:transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill:#3B1F0E;-fx-background-color:transparent;");
                }
            }
        });
        catFilter.setOnAction(e -> {
            String sel = catFilter.getValue();
            filterCategorieId = (sel == null || sel.equals("Toutes les catégories")) ? -1
                    : categories.stream().filter(c -> c.getNom().equals(sel))
                    .mapToInt(Categorie::getId).findFirst().orElse(-1);
            appliquerFiltres();
        });

        // ── Tri ────────────────────────────────────────────────────────────────
        sortBox = new ComboBox<>();
        sortBox.setPromptText("Trier par...");
        sortBox.setPrefWidth(180);
        sortBox.setMinWidth(180);
        sortBox.setMaxWidth(180);
        sortBox.setPrefHeight(44);       // hauteur unifiée
        sortBox.setStyle(fs);
        sortBox.getItems().addAll("Prix ↓", "Stock ↓");
        sortBox.setButtonCell(new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Trier par...");
                    setStyle("-fx-text-fill:#9C6644;-fx-background-color:transparent;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill:#3B1F0E;-fx-background-color:transparent;");
                }
            }
        });
        sortBox.setOnAction(e -> {
            if (sortBox.getValue() == null) return;
            switch (sortBox.getValue()) {
                case "Prix ↓"  -> { sortColumn = "prix";  sortAsc = false; }
                case "Stock ↓" -> { sortColumn = "stock"; sortAsc = false; }
            }
            appliquerFiltres();
        });

        // ── Bouton Réinitialiser — style pill (inspiré de la maquette) ─────────
        FontIcon xIcon = new FontIcon(Feather.X);
        xIcon.setIconSize(13);
        xIcon.setIconColor(Color.web(AppTheme.PRIMARY));

        Button resetBtn = new Button("  Réinitialiser");
        resetBtn.setGraphic(xIcon);
        resetBtn.setPrefHeight(44);
        String resetStyle =
                "-fx-background-color:transparent;" +
                        "-fx-text-fill:#9C6644;-fx-font-size:13px;" +
                        "-fx-cursor:hand;-fx-padding:0 20 0 16;" +
                        "-fx-border-color:#DDB892;" +
                        "-fx-border-radius:9;" +
                        "-fx-background-radius:9;" +
                        "-fx-border-width:1.5;";
        resetBtn.setStyle(resetStyle);
        resetBtn.setOnMouseEntered(e -> resetBtn.setStyle(resetStyle
                .replace("-fx-background-color:transparent;", "-fx-background-color:#F5EBE0;")));
        resetBtn.setOnMouseExited(e  -> resetBtn.setStyle(resetStyle));
        resetBtn.setTooltip(new Tooltip("Réinitialiser les filtres"));
        resetBtn.setOnAction(e -> {
            searchField.clear();
            catFilter.setValue("Toutes les catégories");
            sortBox.getItems().clear();
            sortBox.getItems().addAll("Prix ↓", "Stock ↓");
            sortBox.setPromptText("Trier par...");
            sortBox.getSelectionModel().clearSelection();
            sortBox.setButtonCell(new ListCell<String>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Trier par...");
                        setStyle("-fx-text-fill:#9C6644;-fx-background-color:transparent;");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill:#3B1F0E;-fx-background-color:transparent;");
                    }
                }
            });
            searchText = ""; filterCategorieId = -1; sortColumn = "date"; sortAsc = false;
            appliquerFiltres();
        });

        bar.getChildren().addAll(searchField, catFilter, sortBox, resetBtn);
        return bar;
    }

    private void appliquerFiltres() {
        produitsFiltres = produits.stream()
                .filter(p -> searchText.isEmpty()
                        || p.getNom().toLowerCase().contains(searchText)
                        || (p.getDescription() != null && p.getDescription().toLowerCase().contains(searchText)))
                .filter(p -> filterCategorieId == -1 || p.getIdCategorie() == filterCategorieId)
                .sorted((a, b) -> {
                    int cmp = switch (sortColumn) {
                        case "prix"  -> Double.compare(a.getPrix(), b.getPrix());
                        case "stock" -> Integer.compare(a.getStock(), b.getStock());
                        default      -> 0;
                    };
                    return sortAsc ? cmp : -cmp;
                })
                .collect(Collectors.toList());
        afficherProduits();
    }

    private HBox buildTableHeader() {
        HBox h = new HBox();
        h.setPadding(new Insets(14, 24, 14, 24));
        h.setMaxWidth(Double.MAX_VALUE);
        h.setStyle("-fx-background-color:#D4B896;-fx-background-radius:13 13 0 0;-fx-border-radius:13 13 0 0;-fx-border-color:#C4A882;-fx-border-width:0 0 1 0;");
        h.setAlignment(Pos.CENTER_LEFT);
        Label hProd = hCell("Produit");
        HBox.setHgrow(hProd, Priority.ALWAYS);
        hProd.setMaxWidth(Double.MAX_VALUE);
        Label hDesc = hCell("Description", 300);
        h.getChildren().addAll(hProd, hCell("Catégorie", 160), hCell("Prix", 120), hCell("Stock", 130), hDesc, hCell("Actions", 80));
        return h;
    }

    private Label hCell(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#7B5B3A;-fx-font-family:'Segoe UI Semibold','Segoe UI',sans-serif;");
        return l;
    }

    private Label hCell(String t, double w) {
        Label l = hCell(t);
        l.setPrefWidth(w); l.setMinWidth(w);
        return l;
    }

    private HBox buildProduitRow(Produit produit, boolean isEven) {
        HBox row = new HBox();
        row.setPadding(new Insets(11, 24, 11, 24));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        boolean rupture = produit.getStock() == 0;
        String rowBg    = rupture ? "#FFF0F0" : (isEven ? AppTheme.CARD_BG : "#FBF6F2");
        String rowHover = rupture ? "#FFE0E0" : "#EDD9C8";
        row.setBackground(new Background(new BackgroundFill(Color.web(rowBg), CornerRadii.EMPTY, Insets.EMPTY)));
        row.setStyle("-fx-border-color:#DDB892;-fx-border-width:0 0 1 0;");
        row.setOnMouseEntered(e -> row.setBackground(new Background(new BackgroundFill(Color.web(rowHover), CornerRadii.EMPTY, Insets.EMPTY))));
        row.setOnMouseExited(e  -> row.setBackground(new Background(new BackgroundFill(Color.web(rowBg),    CornerRadii.EMPTY, Insets.EMPTY))));

        HBox produitCell = new HBox(10);
        produitCell.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(produitCell, Priority.ALWAYS);
        produitCell.setMaxWidth(Double.MAX_VALUE);

        StackPane imgC = new StackPane();
        imgC.setPrefSize(46, 46); imgC.setMinSize(46, 46); imgC.setMaxSize(46, 46);
        imgC.setStyle("-fx-background-color:#DDB892;-fx-background-radius:10;");
        FontIcon ph = new FontIcon(Feather.IMAGE); ph.setIconSize(17); ph.setIconColor(Color.web(AppTheme.BG));
        imgC.getChildren().add(ph);
        if (produit.getUrlImage() != null && !produit.getUrlImage().isEmpty()) {
            new Thread(() -> {
                try {
                    Image img = new Image(produit.getUrlImage(), true);
                    Platform.runLater(() -> {
                        if (!img.isError()) {
                            ImageView iv = new ImageView(img);
                            iv.setFitWidth(46); iv.setFitHeight(46); iv.setPreserveRatio(false);
                            Rectangle clip = new Rectangle(46, 46);
                            clip.setArcWidth(12); clip.setArcHeight(12);
                            iv.setClip(clip);
                            imgC.getChildren().setAll(iv);
                        }
                    });
                } catch (Exception ignored) {}
            }).start();
        }

        Label nomLabel = new Label(produit.getNom());
        nomLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#3B1F0E;-fx-font-weight:600;");
        nomLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nomLabel, Priority.ALWAYS);
        produitCell.getChildren().addAll(imgC, nomLabel);

        Label catLabel = new Label(produit.getNomCategorie() != null ? produit.getNomCategorie() : "—");
        catLabel.setPrefWidth(160); catLabel.setMinWidth(160);
        catLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#5C3D20;");

        Label prixLabel = new Label(String.format("%.2f MAD", produit.getPrix()));
        prixLabel.setPrefWidth(120); prixLabel.setMinWidth(120);
        prixLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#5C3D20;-fx-font-weight:600;");

        HBox stockBox = new HBox(5);
        stockBox.setPrefWidth(130); stockBox.setMinWidth(130);
        stockBox.setAlignment(Pos.CENTER_LEFT);
        String sc, sb, st;
        if      (produit.getStock() == 0) { sc = AppTheme.ERROR_COLOR; sb = "#FEE2E2"; st = "✕  0"; }
        else if (produit.getStock() <= 5) { sc = "#D97706"; sb = "#FEF3C7"; st = "⚠  " + produit.getStock(); }
        else                              { sc = "#2D7A47"; sb = "#D1FAE5"; st = String.valueOf(produit.getStock()); }
        Label stockLabel = new Label(st);
        stockLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + sc + ";-fx-background-color:" + sb + ";-fx-background-radius:20;-fx-padding:3 12 3 12;");
        stockBox.getChildren().add(stockLabel);
        if (produit.getStock() == 0) {
            Label badge = new Label("Rupture");
            badge.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:#C0392B;-fx-background-color:#FEE2E2;-fx-background-radius:20;-fx-padding:2 7 2 7;");
            stockBox.getChildren().add(badge);
        }

        String desc = produit.getDescription();
        boolean hasDesc = desc != null && !desc.isBlank();
        String dt = hasDesc
                ? (desc.length() > 60 ? desc.substring(0, 60) + "…" : desc)
                : "—";
        Label descLabel = new Label(dt);
        descLabel.setStyle("-fx-font-size:12px;-fx-text-fill:" + (hasDesc ? AppTheme.TEXT_MUTED : "#C4A882") + ";-fx-font-style:" + (hasDesc ? "italic" : "normal") + ";");
        descLabel.setPrefWidth(300);
        descLabel.setMinWidth(300);
        descLabel.setMaxWidth(300);
        if (hasDesc && desc.length() > 60) {
            Tooltip tip = new Tooltip(desc);
            tip.setWrapText(true);
            tip.setMaxWidth(400);
            Tooltip.install(descLabel, tip);
        }

        HBox actBox = new HBox(6);
        actBox.setPrefWidth(80); actBox.setMinWidth(80);
        actBox.setAlignment(Pos.CENTER_LEFT);
        Button editBtn   = iconBtn(Feather.EDIT_2,  AppTheme.PRIMARY,      "#E6CCB2");
        Button deleteBtn = iconBtn(Feather.TRASH_2, AppTheme.ERROR_COLOR,   "#FEF2F2");
        editBtn.setOnAction(e   -> showDialogModifier(produit));
        deleteBtn.setOnAction(e -> supprimerProduit(produit));
        actBox.getChildren().addAll(editBtn, deleteBtn);

        row.getChildren().addAll(produitCell, catLabel, prixLabel, stockBox, descLabel, actBox);
        return row;
    }

    private Button iconBtn(Feather icon, String color, String hoverBg) {
        Button btn = new Button();
        FontIcon fi = new FontIcon(icon); fi.setIconSize(15); fi.setIconColor(Color.web(color));
        btn.setGraphic(fi);
        btn.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:6;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color:" + hoverBg + ";-fx-background-radius:7;-fx-cursor:hand;-fx-padding:6;"));
        btn.setOnMouseExited(e  -> btn.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:6;"));
        return btn;
    }

    private void afficherProduits() {
        tableContainer.getChildren().clear();
        tableContainer.getChildren().add(buildTableHeader());
        if (produitsFiltres.isEmpty()) {
            Label empty = new Label("Aucun produit trouvé");
            empty.setStyle("-fx-font-size:15px;-fx-text-fill:#B08968;-fx-padding:40;");
            tableContainer.getChildren().add(empty);
            totalLabel.setText("0 produit");
            return;
        }
        for (int i = 0; i < produitsFiltres.size(); i++)
            tableContainer.getChildren().add(buildProduitRow(produitsFiltres.get(i), i % 2 == 0));
        long ruptures = produitsFiltres.stream().filter(p -> p.getStock() == 0).count();
        String lbl = produitsFiltres.size() + " produit" + (produitsFiltres.size() > 1 ? "s" : "");
        if (produitsFiltres.size() < produits.size()) lbl += " (filtrés sur " + produits.size() + ")";
        if (ruptures > 0) lbl += " · " + ruptures + " en rupture";
        totalLabel.setText(lbl);
    }

    private void showDialogAjouter()           { chargerCategories(this::ouvrirDialogAjouter); }
    private void showDialogModifier(Produit p) { chargerCategories(() -> ouvrirDialogModifier(p)); }

    private void ouvrirDialogAjouter() {
        final VBox form = buildForm(null);
        buildFormStage("Ajouter un produit", form, "Ajouter", null).showAndWait();
    }

    private void ouvrirDialogModifier(Produit produit) {
        final VBox form = buildForm(produit);
        buildFormStage("Modifier le produit", form, "Enregistrer", produit).showAndWait();
    }

    private Stage buildFormStage(String titre, VBox form, String okLabel, Produit existant) {
        Stage stage = new Stage();
        stage.setTitle(titre);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setResizable(false);

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setPrefWidth(120);
        cancelBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#7F5539;" +
                        "-fx-border-color:#DDB892;-fx-border-radius:9;-fx-font-size:14px;" +
                        "-fx-padding:10 22 10 22;-fx-cursor:hand;-fx-border-width:1.5;"
        );
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(
                "-fx-background-color:#F5EBE0;-fx-text-fill:#7F5539;" +
                        "-fx-border-color:#DDB892;-fx-border-radius:9;-fx-font-size:14px;" +
                        "-fx-padding:10 22 10 22;-fx-cursor:hand;-fx-border-width:1.5;"
        ));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#7F5539;" +
                        "-fx-border-color:#DDB892;-fx-border-radius:9;-fx-font-size:14px;" +
                        "-fx-padding:10 22 10 22;-fx-cursor:hand;-fx-border-width:1.5;"
        ));
        cancelBtn.setOnAction(e -> stage.close());

        Button okBtn = new Button(okLabel);
        okBtn.setPrefWidth(140);
        String okBase =
                "-fx-background-color:#7F5539;-fx-text-fill:#EDE0D4;" +
                        "-fx-font-size:14px;-fx-font-weight:bold;" +
                        "-fx-background-radius:9;-fx-padding:10 22 10 22;-fx-cursor:hand;";
        okBtn.setStyle(okBase);
        okBtn.setOnMouseEntered(e -> okBtn.setStyle(okBase.replace(AppTheme.PRIMARY, AppTheme.PRIMARY_LIGHT)));
        okBtn.setOnMouseExited(e  -> okBtn.setStyle(okBase));
        okBtn.setOnAction(e -> lireForms(form, existant, stage));

        HBox btnBar = new HBox(12, cancelBtn, okBtn);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(18, 28, 20, 28));
        btnBar.setStyle(
                "-fx-background-color:#F5EBE0;" +
                        "-fx-border-color:#DDB892;-fx-border-width:1 0 0 0;"
        );

        VBox root = new VBox(0, form, btnBar);
        root.setStyle("-fx-background-color:#EDE0D4;");
        stage.setScene(new Scene(root));
        stage.setOnShown(e -> {
            TextField nom = (TextField) root.lookup("#nomField");
            if (nom != null) {
                nom.deselect();
                nom.positionCaret(nom.getText().length());
                nom.getParent().requestFocus();
            }
        });
        return stage;
    }

    private void lireForms(VBox form, Produit existant, Stage stage) {
        TextField nomField   = (TextField) form.lookup("#nomField");
        TextField prixField  = (TextField) form.lookup("#prixField");
        TextField stockField = (TextField) form.lookup("#stockField");
        TextField imageField = (TextField) form.lookup("#imageField");
        TextArea  descField  = (TextArea)  form.lookup("#descField");
        @SuppressWarnings("unchecked")
        ComboBox<Categorie> combo = (ComboBox<Categorie>) form.lookup("#categorieCombo");

        if (combo == null || combo.getValue() == null) {
            showWarn("Veuillez sélectionner une catégorie."); return;
        }
        if (nomField == null || nomField.getText().trim().isEmpty()) {
            showWarn("Veuillez saisir le nom du produit."); return;
        }
        try {
            Produit p = existant != null ? existant : new Produit();
            p.setNom(nomField.getText().trim());
            p.setDescription(descField != null ? descField.getText() : "");
            p.setPrix(prixField != null && !prixField.getText().isEmpty()
                    ? Double.parseDouble(prixField.getText().replace(",", ".")) : 0);
            p.setStock(stockField != null && !stockField.getText().isEmpty()
                    ? Integer.parseInt(stockField.getText().trim()) : 0);
            p.setUrlImage(imageField != null ? imageField.getText() : "");
            p.setIdCategorie(combo.getValue().getId());
            stage.close();
            if (existant != null) modifierProduit(p); else sauvegarderProduit(p);
        } catch (NumberFormatException ex) {
            showError("Prix et Stock doivent être des nombres valides.");
        }
    }

    private void showWarn(String msg)  { new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait(); }
    private void showError(String msg) { new Alert(Alert.AlertType.ERROR,   msg, ButtonType.OK).showAndWait(); }

    private void supprimerProduit(Produit produit) {
        Stage stage = new Stage();
        stage.setTitle("Confirmation");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setResizable(false);

        FontIcon trashIcon = new FontIcon(Feather.TRASH_2);
        trashIcon.setIconSize(26); trashIcon.setIconColor(Color.web(AppTheme.ERROR_COLOR));
        StackPane iconCircle = new StackPane(trashIcon);
        iconCircle.setPrefSize(56, 56); iconCircle.setMinSize(56, 56); iconCircle.setMaxSize(56, 56);
        iconCircle.setStyle("-fx-background-color:#FEE2E2;-fx-background-radius:28;");

        Label titreL = new Label("Supprimer ce produit ?");
        titreL.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:#3B1F0E;");
        Label nomL = new Label("\"" + produit.getNom() + "\"");
        nomL.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#7F5539;");
        Label warnL = new Label("Cette action est irréversible.");
        warnL.setStyle("-fx-font-size:12px;-fx-text-fill:#9C6644;");

        VBox textBox = new VBox(6, titreL, nomL, warnL);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox contentBox = new HBox(18, iconCircle, textBox);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setPadding(new Insets(24, 28, 16, 28));
        contentBox.setStyle("-fx-background-color:#EDE0D4;");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setPrefWidth(110);
        cancelBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#7F5539;-fx-border-color:#DDB892;-fx-border-radius:9;-fx-font-size:13px;-fx-padding:9 20 9 20;-fx-cursor:hand;-fx-border-width:1.5;");
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle("-fx-background-color:#F5EBE0;-fx-text-fill:#7F5539;-fx-border-color:#DDB892;-fx-border-radius:9;-fx-font-size:13px;-fx-padding:9 20 9 20;-fx-cursor:hand;-fx-border-width:1.5;"));
        cancelBtn.setOnMouseExited(e  -> cancelBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#7F5539;-fx-border-color:#DDB892;-fx-border-radius:9;-fx-font-size:13px;-fx-padding:9 20 9 20;-fx-cursor:hand;-fx-border-width:1.5;"));
        cancelBtn.setOnAction(e -> stage.close());

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.setPrefWidth(130);
        String ds = "-fx-background-color:#C0392B;-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:9;-fx-padding:9 20 9 20;-fx-cursor:hand;";
        deleteBtn.setStyle(ds);
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(ds.replace(AppTheme.ERROR_COLOR, "#A93226")));
        deleteBtn.setOnMouseExited(e  -> deleteBtn.setStyle(ds));
        deleteBtn.setOnAction(e -> {
            stage.close();
            new Thread(() -> {
                try {
                    AppRequest req = new AppRequest.Builder()
                            .controller("Produit").action("supprimer")
                            .parameter("id", produit.getId())
                            .authToken(client.getAuthToken()).build();
                    AppResponse res = client.sendAndParse(req);
                    Platform.runLater(() -> { if (res.isSuccess()) chargerProduits(); });
                } catch (Exception ex) { ex.printStackTrace(); }
            }).start();
        });

        HBox btnBar = new HBox(12, cancelBtn, deleteBtn);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(14, 28, 20, 28));
        btnBar.setStyle("-fx-background-color:#F5EBE0;-fx-border-color:#DDB892;-fx-border-width:1 0 0 0;");

        VBox root = new VBox(0, contentBox, btnBar);
        root.setStyle("-fx-background-color:#EDE0D4;");
        stage.setScene(new Scene(root));
        stage.showAndWait();
    }

    private VBox buildForm(Produit p) {
        VBox form = new VBox(0);
        form.setPrefWidth(540);
        form.setStyle("-fx-background-color:#EDE0D4;");
        Label formTitre = new Label(p == null ? "Nouveau produit" : "Modifier : " + p.getNom());
        formTitre.setStyle(
                "-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#7F5539;" +
                        "-fx-font-family:'Segoe UI Semibold','Segoe UI',sans-serif;"
        );
        HBox formHeader = new HBox(formTitre);
        formHeader.setPadding(new Insets(22, 28, 16, 28));
        formHeader.setStyle("-fx-background-color:#EDE0D4;-fx-border-color:#DDB892;-fx-border-width:0 0 1 0;");

        VBox body = new VBox(16);
        body.setPadding(new Insets(20, 28, 8, 28));
        body.setStyle("-fx-background-color:#EDE0D4;");

        VBox nomBox = fBox("Nom du produit *");
        TextField nomField = fText(p != null ? p.getNom() : "", "Ex: Café Artisan Premium");
        nomField.setId("nomField");
        nomBox.getChildren().add(nomField);

        VBox descBox = fBox("Description");
        TextArea descField = new TextArea(p != null ? p.getDescription() : "");
        descField.setId("descField");
        descField.setPromptText("Décrivez le produit...");
        descField.setPrefRowCount(3);
        descField.setStyle(
                "-fx-background-color:#F5EBE0;-fx-background-radius:9;" +
                        "-fx-border-color:#DDB892;-fx-border-radius:9;-fx-border-width:1.5;" +
                        "-fx-font-size:13px;-fx-text-fill:#3B1F0E;-fx-prompt-text-fill:#B08968;"
        );
        descBox.getChildren().add(descField);

        HBox rowBox = new HBox(16);
        VBox prixBox = fBox("Prix (MAD) *");
        TextField prixField = fText(p != null ? String.valueOf(p.getPrix()) : "", "0.00");
        prixField.setId("prixField");
        prixBox.getChildren().add(prixField);
        HBox.setHgrow(prixBox, Priority.ALWAYS);

        VBox stockBox2 = fBox("Stock *");
        TextField stockField = fText(p != null ? String.valueOf(p.getStock()) : "", "0");
        stockField.setId("stockField");
        stockBox2.getChildren().add(stockField);
        HBox.setHgrow(stockBox2, Priority.ALWAYS);
        rowBox.getChildren().addAll(prixBox, stockBox2);

        VBox catBox = fBox("Catégorie *");
        ComboBox<Categorie> combo = new ComboBox<>();
        combo.setId("categorieCombo");
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setPromptText("Sélectionner une catégorie...");
        combo.setStyle(
                "-fx-background-color:#F5EBE0;-fx-border-color:#DDB892;" +
                        "-fx-border-radius:9;-fx-border-width:1.5;-fx-font-size:13px;"
        );
        combo.getItems().addAll(categories);
        combo.setCellFactory(lv -> new ListCell<Categorie>() {
            @Override protected void updateItem(Categorie item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });
        combo.setButtonCell(new ListCell<Categorie>() {
            @Override protected void updateItem(Categorie item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Sélectionner une catégorie...");
                    setStyle("-fx-text-fill:#B08968;-fx-background-color:transparent;");
                } else {
                    setText(item.getNom());
                    setStyle("-fx-text-fill:#3B1F0E;-fx-background-color:transparent;");
                }
            }
        });
        if (p != null && p.getIdCategorie() > 0)
            categories.stream().filter(c -> c.getId() == p.getIdCategorie()).findFirst().ifPresent(combo::setValue);
        catBox.getChildren().add(combo);

        VBox imageBox = fBox("Image du produit");
        TextField imageField = fText(
                p != null && p.getUrlImage() != null ? p.getUrlImage() : "",
                "https://... ou choisir un fichier"
        );
        imageField.setId("imageField");
        imageField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(imageField, Priority.ALWAYS);

        Button parcourirBtn = new Button("📁  Parcourir");
        String sp = "-fx-background-color:#7F5539;-fx-text-fill:#EDE0D4;-fx-font-size:13px;-fx-background-radius:9;-fx-padding:10 16 10 16;-fx-cursor:hand;";
        parcourirBtn.setStyle(sp);
        parcourirBtn.setOnMouseEntered(e -> parcourirBtn.setStyle(sp.replace(AppTheme.PRIMARY, AppTheme.PRIMARY_LIGHT)));
        parcourirBtn.setOnMouseExited(e  -> parcourirBtn.setStyle(sp));
        parcourirBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir une image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));
            File f2 = fc.showOpenDialog(parcourirBtn.getScene() != null ? parcourirBtn.getScene().getWindow() : null);
            if (f2 != null) imageField.setText(f2.toURI().toString());
        });

        HBox imageRow = new HBox(10, imageField, parcourirBtn);
        imageRow.setAlignment(Pos.CENTER_LEFT);
        imageBox.getChildren().add(imageRow);

        Label noteLabel = new Label("* Champs obligatoires");
        noteLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#B08968;");

        body.getChildren().addAll(nomBox, descBox, rowBox, catBox, imageBox, noteLabel);
        form.getChildren().addAll(formHeader, body);
        return form;
    }

    private VBox fBox(String label) {
        VBox b = new VBox(6);
        Label l = new Label(label);
        l.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#7B5B3A;");
        b.getChildren().add(l);
        return b;
    }

    private TextField fText(String val, String ph) {
        TextField f = new TextField(val);
        f.setPromptText(ph);
        f.setStyle(
                "-fx-background-color:#F5EBE0;-fx-background-radius:9;" +
                        "-fx-border-color:#DDB892;-fx-border-radius:9;-fx-border-width:1.5;" +
                        "-fx-padding:10 14 10 14;-fx-font-size:13px;" +
                        "-fx-text-fill:#3B1F0E;-fx-prompt-text-fill:#B08968;"
        );
        f.focusedProperty().addListener((obs, old, focused) -> {
            String base = focused
                    ? "-fx-background-color:#F5EBE0;-fx-background-radius:9;-fx-border-color:#7F5539;-fx-border-radius:9;-fx-border-width:2;-fx-padding:10 14 10 14;-fx-font-size:13px;-fx-text-fill:#3B1F0E;"
                    : "-fx-background-color:#F5EBE0;-fx-background-radius:9;-fx-border-color:#DDB892;-fx-border-radius:9;-fx-border-width:1.5;-fx-padding:10 14 10 14;-fx-font-size:13px;-fx-text-fill:#3B1F0E;-fx-prompt-text-fill:#B08968;";
            f.setStyle(base);
        });
        return f;
    }

    private void chargerCategories(Runnable onDone) {
        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("Categorie").action("lister")
                        .authToken(client.getAuthToken()).build();
                AppResponse res = client.sendAndParse(req);
                Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        Type type = new TypeToken<List<Categorie>>(){}.getType();
                        List<Categorie> result = new Gson().fromJson(new Gson().toJson(res.getData()), type);
                        categories = result != null ? result : new ArrayList<>();
                        System.out.println("Catégories chargées: " + categories.size());
                        if (toolbar != null) {
                            VBox parent = (VBox) toolbar.getParent();
                            if (parent != null) {
                                int idx = parent.getChildren().indexOf(toolbar);
                                toolbar = buildToolbar();
                                parent.getChildren().set(idx, toolbar);
                            }
                        }
                    } else System.err.println("Erreur catégories: " + res.getMessage());
                    if (onDone != null) onDone.run();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> { if (onDone != null) onDone.run(); });
            }
        }).start();
    }

    private void chargerProduits() {
        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("Produit").action("lister")
                        .authToken(client.getAuthToken()).build();
                AppResponse res = client.sendAndParse(req);
                Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        Type type = new TypeToken<List<Produit>>(){}.getType();
                        produits = new Gson().fromJson(new Gson().toJson(res.getData()), type);
                        if (produits == null) produits = new ArrayList<>();
                        produitsFiltres = new ArrayList<>(produits);
                        appliquerFiltres();
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void sauvegarderProduit(Produit produit) {
        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("Produit").action("ajouter")
                        .payload(produit).authToken(client.getAuthToken()).build();
                AppResponse res = client.sendAndParse(req);
                Platform.runLater(() -> {
                    if (res.isSuccess()) chargerProduits();
                    else { Alert a = new Alert(Alert.AlertType.ERROR, res.getMessage(), ButtonType.OK); a.setHeaderText("Impossible d'ajouter le produit"); a.showAndWait(); }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void modifierProduit(Produit produit) {
        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("Produit").action("modifier")
                        .parameter("id", produit.getId())
                        .payload(produit).authToken(client.getAuthToken()).build();
                AppResponse res = client.sendAndParse(req);
                Platform.runLater(() -> {
                    if (res.isSuccess()) chargerProduits();
                    else { Alert a = new Alert(Alert.AlertType.ERROR, res.getMessage(), ButtonType.OK); a.setHeaderText("Impossible de modifier le produit"); a.showAndWait(); }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
}
