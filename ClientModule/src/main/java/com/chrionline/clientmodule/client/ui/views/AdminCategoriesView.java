package com.chrionline.clientmodule.client.ui.views;

import com.chrionline.core.interfaces.ViewManager;
import com.chrionline.core.theme.AppTheme;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.core.network.protocol.AppResponse;
import com.chrionline.network.tcp.TCPClient;
import com.chrionline.shared.models.Categorie;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminCategoriesView extends BorderPane {

    private final TCPClient client;
    @SuppressWarnings("unused") private final Map<String, Object> userData;
    @SuppressWarnings("unused") private final ViewManager viewManager;
    @SuppressWarnings("unused") private final AdminView adminView;

    private List<Categorie> categories         = new ArrayList<>();
    private List<Categorie> categoriesFiltrees = new ArrayList<>();
    private Label totalLabel;
    private GridPane grid;
    private String searchText = "";

    private int nbCols = 3;

    public AdminCategoriesView(TCPClient client, Map<String, Object> userData,
                               ViewManager viewManager, AdminView adminView) {
        this.client      = client;
        this.userData    = userData;
        this.viewManager = viewManager;
        this.adminView   = adminView;
        setBackground(new Background(new BackgroundFill(Color.web(AppTheme.BG), CornerRadii.EMPTY, Insets.EMPTY)));
        buildUI();
        chargerCategories();
    }

    private void buildUI() {
        VBox content = buildContent();
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle(
                "-fx-background-color:" + AppTheme.BG + ";" +
                        "-fx-background:" + AppTheme.BG + ";" +
                        "-fx-border-color:transparent;"
        );
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        setCenter(scrollPane);

        scrollPane.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue();
            int cols;
            if      (w < 600)  cols = 1;
            else if (w < 960)  cols = 2;
            else               cols = 3;

            if (cols != nbCols) {
                nbCols = cols;
                updateColumnConstraints();
                afficherCategories();
            } else {
                updateColumnConstraints();
            }
        });
    }

    private void updateColumnConstraints() {
        grid.getColumnConstraints().clear();
        for (int i = 0; i < nbCols; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / nbCols);
            cc.setFillWidth(true);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }
    }

    private VBox buildContent() {
        VBox content = new VBox(24);
        content.setPadding(new Insets(36, 40, 40, 40));
        content.setBackground(new Background(new BackgroundFill(Color.web(AppTheme.BG), CornerRadii.EMPTY, Insets.EMPTY)));
        content.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(Double.MAX_VALUE);

        VBox titreBox = new VBox(4);
        Label titre = new Label("Gestion des Catégories");
        titre.setStyle(
                "-fx-font-size:32px;-fx-font-weight:bold;-fx-text-fill:" + AppTheme.PRIMARY + ";" +
                        "-fx-font-family:'Segoe UI Semibold','Segoe UI',sans-serif;"
        );
        totalLabel = new Label("Chargement...");
        totalLabel.setStyle("-fx-font-size:13px;-fx-text-fill:" + AppTheme.TEXT_MUTED + ";");
        titreBox.getChildren().addAll(titre, totalLabel);
        HBox.setHgrow(titreBox, Priority.ALWAYS);

        Button ajouterBtn = new Button("  Ajouter une catégorie");
        FontIcon plusIcon = new FontIcon(Feather.PLUS);
        plusIcon.setIconSize(17);
        plusIcon.setIconColor(Color.web(AppTheme.BG));
        ajouterBtn.setGraphic(plusIcon);
        String styleBtn =
                "-fx-background-color:" + AppTheme.PRIMARY + ";" +
                        "-fx-text-fill:" + AppTheme.BG + ";" +
                        "-fx-font-size:15px;-fx-font-weight:bold;" +
                        "-fx-background-radius:10;-fx-padding:14 24 14 24;-fx-cursor:hand;";
        ajouterBtn.setStyle(styleBtn);
        ajouterBtn.setOnMouseEntered(e -> ajouterBtn.setStyle(styleBtn.replace(AppTheme.PRIMARY, AppTheme.PRIMARY_LIGHT)));
        ajouterBtn.setOnMouseExited(e  -> ajouterBtn.setStyle(styleBtn));
        ajouterBtn.setOnAction(e -> ouvrirDialogAjouter());
        header.getChildren().addAll(titreBox, ajouterBtn);

        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setMaxWidth(560);
        searchBar.setPrefWidth(460);
        searchBar.setStyle(
                "-fx-background-color:" + AppTheme.CARD_BG + ";" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius:10;-fx-background-radius:10;-fx-border-width:1.5;" +
                        "-fx-padding:0 16 0 16;"
        );

        FontIcon searchIcon = new FontIcon(Feather.SEARCH);
        searchIcon.setIconSize(15);
        searchIcon.setIconColor(Color.web(AppTheme.TEXT_MUTED));

        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher une catégorie...");
        searchField.setMaxWidth(Double.MAX_VALUE);
        searchField.setPrefHeight(44);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setStyle(
                "-fx-background-color:transparent;" +
                        "-fx-border-color:transparent;" +
                        "-fx-font-size:14px;" +
                        "-fx-text-fill:" + AppTheme.TEXT_MAIN + ";" +
                        "-fx-prompt-text-fill:" + AppTheme.TEXT_MUTED + ";"
        );
        searchField.focusedProperty().addListener((obs, old, focused) ->
                searchBar.setStyle(
                        "-fx-background-color:" + AppTheme.CARD_BG + ";" +
                                "-fx-border-color:" + (focused ? AppTheme.PRIMARY : AppTheme.FIELD_BORDER) + ";" +
                                "-fx-border-radius:10;-fx-background-radius:10;" +
                                "-fx-border-width:" + (focused ? "2" : "1.5") + ";" +
                                "-fx-padding:0 16 0 16;"
                )
        );
        searchField.textProperty().addListener((obs, o, n) -> {
            searchText = n.toLowerCase().trim();
            appliquerFiltres();
        });
        searchBar.getChildren().addAll(searchIcon, searchField);

        grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setMaxWidth(Double.MAX_VALUE);
        updateColumnConstraints();

        content.getChildren().addAll(header, searchBar, grid);
        return content;
    }

    private void appliquerFiltres() {
        categoriesFiltrees = categories.stream()
                .filter(c -> searchText.isEmpty()
                        || c.getNom().toLowerCase().contains(searchText)
                        || (c.getDescription() != null && c.getDescription().toLowerCase().contains(searchText)))
                .collect(Collectors.toList());
        afficherCategories();
    }

    private void afficherCategories() {
        grid.getChildren().clear();

        if (categoriesFiltrees.isEmpty()) {
            Label empty = new Label("Aucune catégorie trouvée");
            empty.setStyle("-fx-font-size:15px;-fx-text-fill:" + AppTheme.TEXT_MUTED + ";-fx-padding:40;");
            GridPane.setColumnSpan(empty, nbCols);
            grid.add(empty, 0, 0);
            totalLabel.setText("0 catégorie");
            return;
        }

        int col = 0, row = 0;
        for (Categorie cat : categoriesFiltrees) {
            VBox card = buildCard(cat);
            card.setMaxWidth(Double.MAX_VALUE);
            GridPane.setFillWidth(card, true);
            grid.add(card, col, row);
            col++;
            if (col == nbCols) { col = 0; row++; }
        }

        totalLabel.setText(categoriesFiltrees.size() + " catégorie"
                + (categoriesFiltrees.size() > 1 ? "s" : "")
                + (categoriesFiltrees.size() < categories.size()
                ? " (filtrées sur " + categories.size() + ")"
                : " au total"));
    }

    private VBox buildCard(Categorie cat) {
        String cardStyle =
                "-fx-background-color:" + AppTheme.CARD_BG + ";" +
                        "-fx-background-radius:14;-fx-border-radius:14;" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(100,60,30,0.08),12,0,0,2);" +
                        "-fx-cursor:default;";
        String cardHover =
                "-fx-background-color:" + AppTheme.CARD_BG + ";" +
                        "-fx-background-radius:14;-fx-border-radius:14;" +
                        "-fx-border-color:" + AppTheme.PRIMARY + ";-fx-border-width:1.5;" +
                        "-fx-effect:dropshadow(gaussian,rgba(100,60,30,0.16),16,0,0,4);" +
                        "-fx-cursor:default;";

        VBox card = new VBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefWidth(USE_COMPUTED_SIZE);
        card.setStyle(cardStyle);
        card.setOnMouseEntered(e -> card.setStyle(cardHover));
        card.setOnMouseExited(e  -> card.setStyle(cardStyle));

        BorderPane cardHeader = new BorderPane();
        cardHeader.setPadding(new Insets(20, 16, 14, 20));

        Label nomLabel = new Label(cat.getNom());
        nomLabel.setStyle(
                "-fx-font-size:18px;-fx-font-weight:bold;" +
                        "-fx-text-fill:" + AppTheme.PRIMARY + ";" +
                        "-fx-font-family:'Segoe UI Semibold','Segoe UI',sans-serif;"
        );
        BorderPane.setAlignment(nomLabel, Pos.CENTER_LEFT);

        Button editBtn   = iconBtn(Feather.EDIT_2,  AppTheme.TEXT_MUTED, AppTheme.FIELD_BORDER);
        Button deleteBtn = iconBtn(Feather.TRASH_2, AppTheme.ERROR_COLOR, "#FEE2E2");
        editBtn.setOnAction(e   -> ouvrirDialogModifier(cat));
        deleteBtn.setOnAction(e -> supprimerCategorie(cat));

        HBox actions = new HBox(2, editBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        BorderPane.setAlignment(actions, Pos.CENTER_RIGHT);

        cardHeader.setLeft(nomLabel);
        cardHeader.setRight(actions);

        String desc = cat.getDescription();
        boolean hasDesc = desc != null && !desc.isBlank();
        Label descLabel = new Label(hasDesc ? desc : "Aucune description");
        descLabel.setStyle(
                "-fx-font-size:13px;" +
                        "-fx-text-fill:" + AppTheme.TEXT_MUTED + ";" +
                        (hasDesc ? "" : "-fx-font-style:italic;")
        );
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(Double.MAX_VALUE);
        VBox descBox = new VBox(descLabel);
        descBox.setPadding(new Insets(0, 20, 16, 20));

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color:#E8D9CC;");

        BorderPane footer = new BorderPane();
        footer.setPadding(new Insets(12, 20, 14, 20));

        Label prodLabel = new Label("Produits");
        prodLabel.setStyle("-fx-font-size:13px;-fx-text-fill:" + AppTheme.TEXT_MUTED + ";");
        BorderPane.setAlignment(prodLabel, Pos.CENTER_LEFT);

        Label countLabel = new Label(String.valueOf(cat.getNbProduits()));
        countLabel.setStyle(
                "-fx-font-size:16px;-fx-font-weight:bold;" +
                        "-fx-text-fill:" + AppTheme.PRIMARY + ";"
        );
        BorderPane.setAlignment(countLabel, Pos.CENTER_RIGHT);

        footer.setLeft(prodLabel);
        footer.setRight(countLabel);

        card.getChildren().addAll(cardHeader, descBox, sep, footer);
        return card;
    }

    private Button iconBtn(Feather icon, String color, String hoverBg) {
        Button btn = new Button();
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(15);
        fi.setIconColor(Color.web(color));
        btn.setGraphic(fi);
        btn.setStyle("-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:7;-fx-background-radius:8;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color:" + hoverBg + ";-fx-cursor:hand;-fx-padding:7;-fx-background-radius:8;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color:transparent;-fx-cursor:hand;-fx-padding:7;-fx-background-radius:8;"
        ));
        return btn;
    }

    private void ouvrirDialogAjouter() {
        final VBox form = buildForm(null);
        buildFormStage("Ajouter une catégorie", form, "Ajouter", null).showAndWait();
    }

    private void ouvrirDialogModifier(Categorie cat) {
        final VBox form = buildForm(cat);
        buildFormStage("Modifier la catégorie", form, "Enregistrer", cat).showAndWait();
    }

    private Stage buildFormStage(String titre, VBox form, String okLabel, Categorie existant) {
        Stage stage = new Stage();
        stage.setTitle(titre);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setResizable(false);

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setPrefWidth(120);
        cancelBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + AppTheme.PRIMARY + ";" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-radius:9;" +
                        "-fx-font-size:14px;-fx-padding:10 22 10 22;-fx-cursor:hand;-fx-border-width:1.5;"
        );
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(
                "-fx-background-color:" + AppTheme.CARD_BG + ";-fx-text-fill:" + AppTheme.PRIMARY + ";" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-radius:9;" +
                        "-fx-font-size:14px;-fx-padding:10 22 10 22;-fx-cursor:hand;-fx-border-width:1.5;"
        ));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:" + AppTheme.PRIMARY + ";" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-radius:9;" +
                        "-fx-font-size:14px;-fx-padding:10 22 10 22;-fx-cursor:hand;-fx-border-width:1.5;"
        ));
        cancelBtn.setOnAction(e -> stage.close());

        Button okBtn = new Button(okLabel);
        okBtn.setPrefWidth(140);
        String okBase =
                "-fx-background-color:" + AppTheme.PRIMARY + ";-fx-text-fill:" + AppTheme.BG + ";" +
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
                "-fx-background-color:" + AppTheme.CARD_BG + ";" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-width:1 0 0 0;"
        );

        stage.setOnShown(e -> {
            TextField nom = (TextField) form.lookup("#nomField");
            if (nom != null) {
                nom.deselect();
                nom.positionCaret(nom.getText().length());
                form.requestFocus();
            }
        });

        VBox root = new VBox(0, form, btnBar);
        root.setStyle("-fx-background-color:" + AppTheme.BG + ";");
        stage.setScene(new Scene(root));
        return stage;
    }

    private void lireForms(VBox form, Categorie existant, Stage stage) {
        TextField nomField  = (TextField) form.lookup("#nomField");
        TextArea  descField = (TextArea)  form.lookup("#descField");

        if (nomField == null || nomField.getText().trim().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez saisir le nom de la catégorie.", ButtonType.OK).showAndWait();
            return;
        }
        Categorie c = existant != null ? existant : new Categorie();
        c.setNom(nomField.getText().trim());
        c.setDescription(descField != null ? descField.getText() : "");
        stage.close();
        if (existant != null) modifierCategorie(c); else sauvegarderCategorie(c);
    }

    private VBox buildForm(Categorie cat) {
        VBox form = new VBox(0);
        form.setPrefWidth(460);
        form.setStyle("-fx-background-color:" + AppTheme.BG + ";");

        Label formTitre = new Label(cat == null ? "Nouvelle catégorie" : "Modifier : " + cat.getNom());
        formTitre.setStyle(
                "-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:" + AppTheme.PRIMARY + ";" +
                        "-fx-font-family:'Segoe UI Semibold','Segoe UI',sans-serif;"
        );
        HBox formHeader = new HBox(formTitre);
        formHeader.setPadding(new Insets(22, 28, 16, 28));
        formHeader.setStyle(
                "-fx-background-color:" + AppTheme.BG + ";" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-width:0 0 1 0;"
        );

        VBox body = new VBox(16);
        body.setPadding(new Insets(20, 28, 8, 28));
        body.setStyle("-fx-background-color:" + AppTheme.BG + ";");

        VBox nomBox = fBox("Nom de la catégorie *");
        TextField nomField = fText(cat != null ? cat.getNom() : "", "Ex: Bijoux artisanaux");
        nomField.setId("nomField");
        nomBox.getChildren().add(nomField);

        VBox descBox = fBox("Description");
        TextArea descField = new TextArea(cat != null && cat.getDescription() != null ? cat.getDescription() : "");
        descField.setId("descField");
        descField.setPromptText("Décrivez cette catégorie...");
        descField.setPrefRowCount(4);
        descField.setStyle(
                "-fx-background-color:" + AppTheme.CARD_BG + ";-fx-background-radius:9;" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-radius:9;-fx-border-width:1.5;" +
                        "-fx-font-size:13px;-fx-text-fill:" + AppTheme.TEXT_MAIN + ";" +
                        "-fx-prompt-text-fill:" + AppTheme.TEXT_MUTED + ";"
        );
        descBox.getChildren().add(descField);

        Label noteLabel = new Label("* Champ obligatoire");
        noteLabel.setStyle("-fx-font-size:11px;-fx-text-fill:" + AppTheme.TEXT_MUTED + ";");

        body.getChildren().addAll(nomBox, descBox, noteLabel);
        form.getChildren().addAll(formHeader, body);
        return form;
    }

    private VBox fBox(String label) {
        VBox b = new VBox(6);
        Label l = new Label(label);
        l.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + AppTheme.PRIMARY + ";");
        b.getChildren().add(l);
        return b;
    }

    private TextField fText(String val, String ph) {
        TextField f = new TextField(val);
        f.setPromptText(ph);
        f.setStyle(
                "-fx-background-color:" + AppTheme.CARD_BG + ";-fx-background-radius:9;" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-radius:9;-fx-border-width:1.5;" +
                        "-fx-padding:10 14 10 14;-fx-font-size:13px;" +
                        "-fx-text-fill:" + AppTheme.TEXT_MAIN + ";" +
                        "-fx-prompt-text-fill:" + AppTheme.TEXT_MUTED + ";"
        );
        f.focusedProperty().addListener((obs, old, focused) -> {
            f.setStyle(focused
                    ? "-fx-background-color:" + AppTheme.CARD_BG + ";-fx-background-radius:9;" +
                    "-fx-border-color:" + AppTheme.PRIMARY + ";-fx-border-radius:9;-fx-border-width:2;" +
                    "-fx-padding:10 14 10 14;-fx-font-size:13px;-fx-text-fill:" + AppTheme.TEXT_MAIN + ";"
                    : "-fx-background-color:" + AppTheme.CARD_BG + ";-fx-background-radius:9;" +
                    "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-radius:9;-fx-border-width:1.5;" +
                    "-fx-padding:10 14 10 14;-fx-font-size:13px;-fx-text-fill:" + AppTheme.TEXT_MAIN + ";" +
                    "-fx-prompt-text-fill:" + AppTheme.TEXT_MUTED + ";"
            );
        });
        return f;
    }

    private void supprimerCategorie(Categorie cat) {
        // ── Blocage si la catégorie contient des produits ─────────────
        if (cat.getNbProduits() > 0) {
            Stage warnStage = new Stage();
            warnStage.setTitle("Suppression impossible");
            warnStage.initModality(Modality.APPLICATION_MODAL);
            warnStage.initStyle(StageStyle.DECORATED);
            warnStage.setResizable(false);

            FontIcon warnIcon = new FontIcon(Feather.ALERT_TRIANGLE);
            warnIcon.setIconSize(26);
            warnIcon.setIconColor(Color.web("#D97706"));
            StackPane warnCircle = new StackPane(warnIcon);
            warnCircle.setPrefSize(56, 56); warnCircle.setMinSize(56, 56); warnCircle.setMaxSize(56, 56);
            warnCircle.setStyle("-fx-background-color:#FEF3C7;-fx-background-radius:28;");

            Label titreW = new Label("Suppression impossible");
            titreW.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + AppTheme.TEXT_MAIN + ";");
            Label nomW = new Label("\"" + cat.getNom() + "\"");
            nomW.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + AppTheme.PRIMARY + ";");
            int nb = cat.getNbProduits();
            Label msgW = new Label("Cette catégorie contient " + nb + " produit" + (nb > 1 ? "s" : "") + ".\n"
                    + "Veuillez d'abord supprimer ses produits.");
            msgW.setStyle("-fx-font-size:13px;-fx-text-fill:" + AppTheme.TEXT_MUTED + ";");
            msgW.setWrapText(true);

            VBox textBoxW = new VBox(6, titreW, nomW, msgW);
            textBoxW.setAlignment(Pos.CENTER_LEFT);
            HBox contentBoxW = new HBox(18, warnCircle, textBoxW);
            contentBoxW.setAlignment(Pos.CENTER_LEFT);
            contentBoxW.setPadding(new Insets(24, 28, 16, 28));
            contentBoxW.setStyle("-fx-background-color:" + AppTheme.BG + ";");

            Button okW = new Button("Compris");
            okW.setPrefWidth(130);
            String okWStyle =
                    "-fx-background-color:#D97706;-fx-text-fill:white;" +
                            "-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:9;" +
                            "-fx-padding:9 20 9 20;-fx-cursor:hand;";
            okW.setStyle(okWStyle);
            okW.setOnMouseEntered(e -> okW.setStyle(okWStyle.replace("#D97706", "#B45309")));
            okW.setOnMouseExited(e  -> okW.setStyle(okWStyle));
            okW.setOnAction(e -> warnStage.close());

            HBox btnBarW = new HBox(okW);
            btnBarW.setAlignment(Pos.CENTER_RIGHT);
            btnBarW.setPadding(new Insets(14, 28, 20, 28));
            btnBarW.setStyle(
                    "-fx-background-color:" + AppTheme.CARD_BG + ";" +
                            "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-width:1 0 0 0;"
            );

            VBox rootW = new VBox(0, contentBoxW, btnBarW);
            rootW.setStyle("-fx-background-color:" + AppTheme.BG + ";");
            warnStage.setScene(new Scene(rootW));
            warnStage.showAndWait();
            return; // bloquer la suppression
        }

        // ── Confirmation normale (catégorie sans produits) ────────────
        Stage stage = new Stage();
        stage.setTitle("Confirmation");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setResizable(false);

        FontIcon trashIcon = new FontIcon(Feather.TRASH_2);
        trashIcon.setIconSize(26);
        trashIcon.setIconColor(Color.web(AppTheme.ERROR_COLOR));
        StackPane iconCircle = new StackPane(trashIcon);
        iconCircle.setPrefSize(56, 56); iconCircle.setMinSize(56, 56); iconCircle.setMaxSize(56, 56);
        iconCircle.setStyle("-fx-background-color:#FEE2E2;-fx-background-radius:28;");

        Label titreL = new Label("Supprimer cette catégorie ?");
        titreL.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + AppTheme.TEXT_MAIN + ";");
        Label nomL = new Label("\"" + cat.getNom() + "\"");
        nomL.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + AppTheme.PRIMARY + ";");
        Label warnL = new Label("Cette action est irréversible.");
        warnL.setStyle("-fx-font-size:12px;-fx-text-fill:" + AppTheme.TEXT_MUTED + ";");

        VBox textBox = new VBox(6, titreL, nomL, warnL);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox contentBox = new HBox(18, iconCircle, textBox);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setPadding(new Insets(24, 28, 16, 28));
        contentBox.setStyle("-fx-background-color:" + AppTheme.BG + ";");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setPrefWidth(110);
        String cancelStyle =
                "-fx-background-color:transparent;-fx-text-fill:" + AppTheme.PRIMARY + ";" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-radius:9;" +
                        "-fx-font-size:13px;-fx-padding:9 20 9 20;-fx-cursor:hand;-fx-border-width:1.5;";
        cancelBtn.setStyle(cancelStyle);
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(cancelStyle.replace("transparent", AppTheme.CARD_BG)));
        cancelBtn.setOnMouseExited(e  -> cancelBtn.setStyle(cancelStyle));
        cancelBtn.setOnAction(e -> stage.close());

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.setPrefWidth(130);
        String ds =
                "-fx-background-color:" + AppTheme.ERROR_COLOR + ";-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:9;" +
                        "-fx-padding:9 20 9 20;-fx-cursor:hand;";
        deleteBtn.setStyle(ds);
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(ds.replace(AppTheme.ERROR_COLOR, "#A93226")));
        deleteBtn.setOnMouseExited(e  -> deleteBtn.setStyle(ds));
        deleteBtn.setOnAction(e -> {
            stage.close();
            new Thread(() -> {
                try {
                    AppRequest req = new AppRequest.Builder()
                            .controller("Categorie").action("supprimer")
                            .parameter("id", cat.getId())
                            .authToken(client.getAuthToken()).build();
                    AppResponse res = client.sendAndParse(req);
                    Platform.runLater(() -> {
                        if (res.isSuccess()) {
                            chargerCategories();
                        } else {
                            // Données périmées côté client : on rafraîchit puis on informe
                            chargerCategories();
                            Alert a = new Alert(Alert.AlertType.ERROR, res.getMessage(), ButtonType.OK);
                            a.setHeaderText("Suppression impossible");
                            a.showAndWait();
                        }
                    });
                } catch (Exception ex) { ex.printStackTrace(); }
            }).start();
        });

        HBox btnBar = new HBox(12, cancelBtn, deleteBtn);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(14, 28, 20, 28));
        btnBar.setStyle(
                "-fx-background-color:" + AppTheme.CARD_BG + ";" +
                        "-fx-border-color:" + AppTheme.FIELD_BORDER + ";-fx-border-width:1 0 0 0;"
        );

        VBox root = new VBox(0, contentBox, btnBar);
        root.setStyle("-fx-background-color:" + AppTheme.BG + ";");
        stage.setScene(new Scene(root));
        stage.showAndWait();
    }

    private void chargerCategories() {
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
                        categoriesFiltrees = new ArrayList<>(categories);
                        appliquerFiltres();
                    } else {
                        System.err.println("Erreur chargement catégories: " + res.getMessage());
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void sauvegarderCategorie(Categorie cat) {
        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("Categorie").action("ajouter")
                        .payload(cat).authToken(client.getAuthToken()).build();
                AppResponse res = client.sendAndParse(req);
                Platform.runLater(() -> {
                    if (res.isSuccess()) chargerCategories();
                    else {
                        Alert a = new Alert(Alert.AlertType.ERROR, res.getMessage(), ButtonType.OK);
                        a.setHeaderText("Impossible d'ajouter la catégorie");
                        a.showAndWait();
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void modifierCategorie(Categorie cat) {
        new Thread(() -> {
            try {
                AppRequest req = new AppRequest.Builder()
                        .controller("Categorie").action("modifier")
                        .parameter("id", cat.getId())
                        .payload(cat).authToken(client.getAuthToken()).build();
                AppResponse res = client.sendAndParse(req);
                Platform.runLater(() -> {
                    if (res.isSuccess()) chargerCategories();
                    else {
                        Alert a = new Alert(Alert.AlertType.ERROR, res.getMessage(), ButtonType.OK);
                        a.setHeaderText("Impossible de modifier la catégorie");
                        a.showAndWait();
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
}
