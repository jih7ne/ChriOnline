package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.core.interfaces.ViewManager;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.network.tcp.TCPClient;
import com.chrionline.chrionline.server.data.models.Produit;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.stage.FileChooser;

public class AdminProduitsView extends BorderPane {

    private final TCPClient client;
    @SuppressWarnings("unused") private final Map<String, Object> userData;
    @SuppressWarnings("unused") private final ViewManager viewManager;
    @SuppressWarnings("unused") private final AdminView adminView;

    private VBox tableContainer;
    private List<Produit> produits = new ArrayList<>();
    private Label totalLabel;

    // ── Couleurs exactes screenshot 2 ────────────────────────────────
    // BG: #EDE0D4  Table bg: #FFFFFF  Header: #F0E0CC
    // Titre: #7F5539  Texte rows: #3B1F0E
    // Bouton ajouter: #7F5539 bg, #EDE0D4 texte

    public AdminProduitsView(TCPClient client, Map<String, Object> userData,
                             ViewManager viewManager, AdminView adminView) {
        this.client = client;
        this.userData = userData;
        this.viewManager = viewManager;
        this.adminView = adminView;
        setBackground(new Background(new BackgroundFill(Color.web("#EDE0D4"), CornerRadii.EMPTY, Insets.EMPTY)));
        buildUI();
        chargerProduits();
    }

    private void buildUI() {
        ScrollPane scrollPane = new ScrollPane(buildContent());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle(
                "-fx-background-color: #EDE0D4;" +
                        "-fx-background: #EDE0D4;" +
                        "-fx-border-color: transparent;"
        );
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        setCenter(scrollPane);
    }

    private VBox buildContent() {
        VBox content = new VBox(28);
        content.setPadding(new Insets(36, 40, 40, 40));
        content.setBackground(new Background(new BackgroundFill(Color.web("#EDE0D4"), CornerRadii.EMPTY, Insets.EMPTY)));
        // Pas de maxWidth — la table prend toute la place disponible
        content.setMaxWidth(Double.MAX_VALUE);

        // ── Header ──────────────────────────────────────────────────
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titreBox = new VBox(5);
        Label titre = new Label("Gestion des Produits");
        titre.setStyle(
                "-fx-font-size: 32px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #7F5539;" +
                        "-fx-font-family: 'Segoe UI Semibold', 'Segoe UI', sans-serif;"
        );
        totalLabel = new Label("Chargement...");
        totalLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7F5539;");
        titreBox.getChildren().addAll(titre, totalLabel);
        HBox.setHgrow(titreBox, Priority.ALWAYS);

        Button ajouterBtn = new Button("  Ajouter un produit");
        FontIcon plusIcon = new FontIcon(Feather.PLUS);
        plusIcon.setIconSize(17);
        plusIcon.setIconColor(Color.web("#EDE0D4"));
        ajouterBtn.setGraphic(plusIcon);
        String styleBtn =
                "-fx-background-color: #7F5539;" +
                        "-fx-text-fill: #EDE0D4;" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 14 24 14 24;" +
                        "-fx-cursor: hand;";
        ajouterBtn.setStyle(styleBtn);
        ajouterBtn.setOnMouseEntered(e -> ajouterBtn.setStyle(styleBtn.replace("#7F5539", "#6A4730")));
        ajouterBtn.setOnMouseExited(e -> ajouterBtn.setStyle(styleBtn));
        ajouterBtn.setOnAction(e -> showDialogAjouter());

        header.getChildren().addAll(titreBox, ajouterBtn);

        // ── Table container — pleine largeur, beige exactement comme la photo ─
        tableContainer = new VBox(0);
        tableContainer.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);
        HBox.setHgrow(tableContainer, Priority.ALWAYS);
        // Fond beige crème + coins arrondis + légère ombre
        tableContainer.setStyle(
                "-fx-background-color: #F5EBE0;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: #DDB892;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(100,60,30,0.08), 16, 0, 0, 3);"
        );

        content.getChildren().addAll(header, tableContainer);
        return content;
    }

    private HBox buildTableHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setMaxWidth(Double.MAX_VALUE);
        // En-tête sable/tan chaud — exactement comme la photo
        header.setStyle(
                "-fx-background-color: #D4B896;" +
                        "-fx-background-radius: 13 13 0 0;" +
                        "-fx-border-radius: 13 13 0 0;" +
                        "-fx-border-color: #C4A882;" +
                        "-fx-border-width: 0 0 1 0;"
        );
        header.setAlignment(Pos.CENTER_LEFT);

        header.getChildren().addAll(
                buildHeaderCell("Produit",    320),
                buildHeaderCell("Catégorie",  170),
                buildHeaderCell("Prix",       130),
                buildHeaderCell("Stock",      110),
                buildHeaderCell("Actions",    120)
        );
        return header;
    }

    private Label buildHeaderCell(String text, double width) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setMinWidth(width);
        label.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #5C3317;" +
                        "-fx-font-family: 'Segoe UI Semibold', 'Segoe UI', sans-serif;"
        );
        return label;
    }

    private HBox buildProduitRow(Produit produit, boolean isEven) {
        HBox row = new HBox();
        row.setPadding(new Insets(13, 24, 13, 24));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        // Couleurs crème/beige alternées — exactement comme la photo
        String rowBg     = isEven ? "#F5EBE0" : "#FBF6F2";
        String rowHover  = "#EDD9C8";
        String rowBorder = "#DDB892";
        row.setBackground(new Background(new BackgroundFill(Color.web(rowBg), CornerRadii.EMPTY, Insets.EMPTY)));
        row.setStyle("-fx-border-color: " + rowBorder + "; -fx-border-width: 0 0 1 0;");
        row.setOnMouseEntered(e ->
                row.setBackground(new Background(new BackgroundFill(Color.web(rowHover), CornerRadii.EMPTY, Insets.EMPTY)))
        );
        row.setOnMouseExited(e ->
                row.setBackground(new Background(new BackgroundFill(Color.web(rowBg), CornerRadii.EMPTY, Insets.EMPTY)))
        );

        // ── Colonne Produit (image + nom) ───────────────────────────
        HBox produitCell = new HBox(12);
        produitCell.setPrefWidth(320);
        produitCell.setMinWidth(320);
        produitCell.setAlignment(Pos.CENTER_LEFT);

        StackPane imgContainer = new StackPane();
        imgContainer.setPrefSize(52, 52);
        imgContainer.setMinSize(52, 52);
        imgContainer.setMaxSize(52, 52);
        imgContainer.setStyle("-fx-background-color: #DDB892; -fx-background-radius: 10;");

        FontIcon placeholder = new FontIcon(Feather.IMAGE);
        placeholder.setIconSize(20);
        placeholder.setIconColor(Color.web("#EDE0D4"));
        imgContainer.getChildren().add(placeholder);

        if (produit.getUrlImage() != null && !produit.getUrlImage().isEmpty()) {
            new Thread(() -> {
                try {
                    Image image = new Image(produit.getUrlImage(), true);
                    Platform.runLater(() -> {
                        if (!image.isError()) {
                            ImageView iv = new ImageView(image);
                            iv.setFitWidth(52);
                            iv.setFitHeight(52);
                            iv.setPreserveRatio(false);
                            Rectangle clip = new Rectangle(52, 52);
                            clip.setArcWidth(16);
                            clip.setArcHeight(16);
                            iv.setClip(clip);
                            imgContainer.getChildren().setAll(iv);
                        }
                    });
                } catch (Exception ignored) {}
            }).start();
        }

        Label nomLabel = new Label(produit.getNom());
        nomLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #3B1F0E; -fx-font-weight: 500;");
        nomLabel.setWrapText(true);
        nomLabel.setMaxWidth(220);
        produitCell.getChildren().addAll(imgContainer, nomLabel);

        // ── Catégorie ────────────────────────────────────────────────
        Label catLabel = new Label(
                produit.getNomCategorie() != null ? produit.getNomCategorie() : "—"
        );
        catLabel.setPrefWidth(170);
        catLabel.setMinWidth(170);
        catLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5C3D20;");

        // ── Prix ─────────────────────────────────────────────────────
        Label prixLabel = new Label(String.format("%.2f\nMAD", produit.getPrix()));
        prixLabel.setPrefWidth(130);
        prixLabel.setMinWidth(130);
        prixLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5C3D20;");

        // ── Stock avec couleur selon niveau ─────────────────────────
        HBox stockBox = new HBox(7);
        stockBox.setPrefWidth(110);
        stockBox.setMinWidth(110);
        stockBox.setAlignment(Pos.CENTER_LEFT);
        boolean enStock = produit.getStock() > 0;
        Label stockLabel = new Label(String.valueOf(produit.getStock()));
        stockLabel.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + (enStock ? "#2D7A47" : "#C0392B") + ";"
        );
        stockBox.getChildren().add(stockLabel);

        // ── Actions ──────────────────────────────────────────────────
        HBox actionsBox = new HBox(6);
        actionsBox.setPrefWidth(120);
        actionsBox.setMinWidth(120);
        actionsBox.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = new Button();
        FontIcon editIcon = new FontIcon(Feather.EDIT_2);
        editIcon.setIconSize(16);
        editIcon.setIconColor(Color.web("#7F5539"));
        editBtn.setGraphic(editIcon);
        editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6;");
        editBtn.setOnMouseEntered(e -> editBtn.setStyle(
                "-fx-background-color: #E6CCB2; -fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 6;"
        ));
        editBtn.setOnMouseExited(e -> editBtn.setStyle(
                "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6;"
        ));
        editBtn.setOnAction(e -> showDialogModifier(produit));

        Button deleteBtn = new Button();
        FontIcon deleteIcon = new FontIcon(Feather.TRASH_2);
        deleteIcon.setIconSize(16);
        deleteIcon.setIconColor(Color.web("#C0392B"));
        deleteBtn.setGraphic(deleteIcon);
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6;");
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(
                "-fx-background-color: #FEF2F2; -fx-background-radius: 7; -fx-cursor: hand; -fx-padding: 6;"
        ));
        deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(
                "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6;"
        ));
        deleteBtn.setOnAction(e -> supprimerProduit(produit));

        actionsBox.getChildren().addAll(editBtn, deleteBtn);
        row.getChildren().addAll(produitCell, catLabel, prixLabel, stockBox, actionsBox);
        return row;
    }

    private void afficherProduits() {
        tableContainer.getChildren().clear();
        tableContainer.getChildren().add(buildTableHeader());

        if (produits.isEmpty()) {
            Label empty = new Label("Aucun produit trouvé");
            empty.setStyle(
                    "-fx-font-size: 15px;" +
                            "-fx-text-fill: #B08968;" +
                            "-fx-padding: 40;"
            );
            tableContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < produits.size(); i++) {
            tableContainer.getChildren().add(buildProduitRow(produits.get(i), i % 2 == 0));
        }

        totalLabel.setText(produits.size() + " produit" + (produits.size() > 1 ? "s" : "") + " au total");
    }

    // ─────────────────────────────────────────────────────────────────
    //  Dialog Ajouter
    // ─────────────────────────────────────────────────────────────────
    private void showDialogAjouter() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un produit");

        VBox form = buildForm(null);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setStyle("-fx-background-color: #EDE0D4;");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Ajouter");
        okBtn.setStyle(
                "-fx-background-color: #7F5539;" +
                        "-fx-text-fill: #EDE0D4;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 9 22 9 22;"
        );

        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.setText("Annuler");
        cancelBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #7F5539;" +
                        "-fx-border-color: #7F5539;" +
                        "-fx-border-radius: 8;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 9 22 9 22;"
        );

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                TextField nomField   = (TextField) form.lookup("#nomField");
                TextField prixField  = (TextField) form.lookup("#prixField");
                TextField stockField = (TextField) form.lookup("#stockField");
                TextField imageField = (TextField) form.lookup("#imageField");
                TextArea  descField  = (TextArea)  form.lookup("#descField");

                if (nomField != null && !nomField.getText().trim().isEmpty()) {
                    try {
                        Produit nouveau = new Produit();
                        nouveau.setNom(nomField.getText().trim());
                        nouveau.setDescription(descField != null ? descField.getText() : "");
                        nouveau.setPrix(prixField != null && !prixField.getText().isEmpty()
                                ? Double.parseDouble(prixField.getText().replace(",", ".")) : 0);
                        nouveau.setStock(stockField != null && !stockField.getText().isEmpty()
                                ? Integer.parseInt(stockField.getText().trim()) : 0);
                        nouveau.setUrlImage(imageField != null ? imageField.getText() : "");
                        sauvegarderProduit(nouveau);
                    } catch (NumberFormatException ex) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erreur de saisie");
                        alert.setHeaderText(null);
                        alert.setContentText("Prix et Stock doivent être des nombres valides.");
                        alert.showAndWait();
                    }
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Dialog Modifier
    // ─────────────────────────────────────────────────────────────────
    private void showDialogModifier(Produit produit) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier le produit");

        VBox form = buildForm(produit);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setStyle("-fx-background-color: #EDE0D4;");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Enregistrer");
        okBtn.setStyle(
                "-fx-background-color: #7F5539;" +
                        "-fx-text-fill: #EDE0D4;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 9 22 9 22;"
        );

        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.setText("Annuler");
        cancelBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #7F5539;" +
                        "-fx-border-color: #7F5539;" +
                        "-fx-border-radius: 8;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 9 22 9 22;"
        );

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                TextField nomField   = (TextField) form.lookup("#nomField");
                TextField prixField  = (TextField) form.lookup("#prixField");
                TextField stockField = (TextField) form.lookup("#stockField");
                TextField imageField = (TextField) form.lookup("#imageField");
                TextArea  descField  = (TextArea)  form.lookup("#descField");

                if (nomField != null) {
                    produit.setNom(nomField.getText());
                    produit.setDescription(descField != null ? descField.getText() : "");
                    try { produit.setPrix(Double.parseDouble(prixField.getText())); }  catch (Exception ignored) {}
                    try { produit.setStock(Integer.parseInt(stockField.getText())); }   catch (Exception ignored) {}
                    produit.setUrlImage(imageField != null ? imageField.getText() : "");
                    modifierProduit(produit);
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Formulaire partagé
    // ─────────────────────────────────────────────────────────────────
    private VBox buildForm(Produit produit) {
        VBox form = new VBox(18);
        form.setPadding(new Insets(10));
        form.setPrefWidth(520);

        VBox nomBox = buildFieldBox("Nom du produit");
        TextField nomField = buildTextField(
                produit != null ? produit.getNom() : "", "Ex: Café Artisan Premium"
        );
        nomField.setId("nomField");
        nomBox.getChildren().add(nomField);

        VBox descBox = buildFieldBox("Description");
        TextArea descField = new TextArea(produit != null ? produit.getDescription() : "");
        descField.setId("descField");
        descField.setPromptText("Description du produit...");
        descField.setPrefRowCount(3);
        descField.setStyle(
                "-fx-background-color: #E6CCB2;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: #DDB892;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-width: 1;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: #3B1F0E;"
        );
        descBox.getChildren().add(descField);

        HBox rowBox = new HBox(18);

        VBox prixBox = buildFieldBox("Prix (MAD)");
        TextField prixField = buildTextField(
                produit != null ? String.valueOf(produit.getPrix()) : "", "24.99"
        );
        prixField.setId("prixField");
        prixBox.getChildren().add(prixField);
        HBox.setHgrow(prixBox, Priority.ALWAYS);

        VBox stockBox = buildFieldBox("Stock");
        TextField stockField = buildTextField(
                produit != null ? String.valueOf(produit.getStock()) : "", "15"
        );
        stockField.setId("stockField");
        stockBox.getChildren().add(stockField);
        HBox.setHgrow(stockBox, Priority.ALWAYS);

        rowBox.getChildren().addAll(prixBox, stockBox);

        // ── Image : URL ou fichier local ─────────────────────────────
        VBox imageBox = buildFieldBox("Image du produit");

        TextField imageField = buildTextField(
                produit != null ? (produit.getUrlImage() != null ? produit.getUrlImage() : "") : "",
                "https://... ou choisir depuis le PC"
        );
        imageField.setId("imageField");
        imageField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(imageField, Priority.ALWAYS);

        // Bouton « Parcourir »
        Button parcourirBtn = new Button("📁  Parcourir");
        parcourirBtn.setStyle(
                "-fx-background-color: #7F5539;" +
                        "-fx-text-fill: #EDE0D4;" +
                        "-fx-font-size: 13px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 10 16 10 16;" +
                        "-fx-cursor: hand;"
        );
        parcourirBtn.setOnMouseEntered(e -> parcourirBtn.setStyle(
                "-fx-background-color: #6A4730; -fx-text-fill: #EDE0D4;" +
                        "-fx-font-size: 13px; -fx-background-radius: 8;" +
                        "-fx-padding: 10 16 10 16; -fx-cursor: hand;"
        ));
        parcourirBtn.setOnMouseExited(e -> parcourirBtn.setStyle(
                "-fx-background-color: #7F5539; -fx-text-fill: #EDE0D4;" +
                        "-fx-font-size: 13px; -fx-background-radius: 8;" +
                        "-fx-padding: 10 16 10 16; -fx-cursor: hand;"
        ));
        parcourirBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir une image");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
            );
            File fichier = fc.showOpenDialog(parcourirBtn.getScene() != null
                    ? parcourirBtn.getScene().getWindow() : null);
            if (fichier != null) {
                imageField.setText(fichier.toURI().toString());
            }
        });

        HBox imageRow = new HBox(10, imageField, parcourirBtn);
        imageRow.setAlignment(Pos.CENTER_LEFT);
        imageBox.getChildren().add(imageRow);

        form.getChildren().addAll(nomBox, descBox, rowBox, imageBox);
        return form;
    }

    private VBox buildFieldBox(String labelText) {
        VBox box = new VBox(7);
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3B1F0E;");
        box.getChildren().add(label);
        return box;
    }

    private TextField buildTextField(String value, String placeholder) {
        TextField field = new TextField(value);
        field.setPromptText(placeholder);
        field.setStyle(
                "-fx-background-color: #E6CCB2;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: #DDB892;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 10 14 10 14;" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-fill: #3B1F0E;"
        );
        return field;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Réseau
    // ─────────────────────────────────────────────────────────────────
    private void chargerProduits() {
        new Thread(() -> {
            try {
                AppRequest request = new AppRequest.Builder()
                        .controller("Produit")
                        .action("lister")
                        .authToken(client.getAuthToken())
                        .build();
                AppResponse response = client.sendAndParse(request);
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        Type type = new TypeToken<List<Produit>>(){}.getType();
                        produits = new Gson().fromJson(new Gson().toJson(response.getData()), type);
                        if (produits == null) produits = new ArrayList<>();
                        afficherProduits();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sauvegarderProduit(Produit produit) {
        new Thread(() -> {
            try {
                AppRequest request = new AppRequest.Builder()
                        .controller("Produit")
                        .action("ajouter")
                        .payload(produit)
                        .authToken(client.getAuthToken())
                        .build();
                AppResponse response = client.sendAndParse(request);
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        chargerProduits();
                    } else {
                        // ← AJOUTEZ CECI pour voir l'erreur exacte
                        System.err.println("Erreur serveur: " + response.getMessage());
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erreur");
                        alert.setHeaderText("Impossible d'ajouter le produit");
                        alert.setContentText(response.getMessage());
                        alert.showAndWait();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void modifierProduit(Produit produit) {
        new Thread(() -> {
            try {
                AppRequest request = new AppRequest.Builder()
                        .controller("Produit")
                        .action("modifier")
                        .parameter("id", produit.getId())
                        .payload(produit)
                        .authToken(client.getAuthToken())
                        .build();
                AppResponse response = client.sendAndParse(request);
                Platform.runLater(() -> {
                    if (response.isSuccess()) chargerProduits();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void supprimerProduit(Produit produit) {
        // ── Dialog de confirmation stylisé ────────────────────────────
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("");

        // Icône trash dans un cercle rouge
        FontIcon trashIcon = new FontIcon(Feather.TRASH_2);
        trashIcon.setIconSize(28);
        trashIcon.setIconColor(Color.web("#C0392B"));
        StackPane iconCircle = new StackPane(trashIcon);
        iconCircle.setPrefSize(64, 64);
        iconCircle.setMinSize(64, 64);
        iconCircle.setMaxSize(64, 64);
        iconCircle.setStyle("-fx-background-color: #FEE2E2; -fx-background-radius: 32;");

        Label titreLabel = new Label("Supprimer le produit ?");
        titreLabel.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #3B1F0E;" +
                        "-fx-font-family: 'Segoe UI Semibold', 'Segoe UI', sans-serif;"
        );

        Label nomLabel = new Label("\"" + produit.getNom() + "\"");
        nomLabel.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #7F5539;"
        );

        Label warnLabel = new Label("Cette action est irréversible. Le produit sera définitivement supprimé.");
        warnLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8B6347;");
        warnLabel.setWrapText(true);
        warnLabel.setMaxWidth(360);

        VBox textBox = new VBox(6, titreLabel, nomLabel, warnLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox content = new HBox(20, iconCircle, textBox);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(10, 10, 10, 10));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle(
                "-fx-background-color: #EDE0D4;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;"
        );
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Bouton Supprimer — rouge
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Supprimer");
        okBtn.setStyle(
                "-fx-background-color: #C0392B;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 10 24 10 24;"
        );
        okBtn.setOnMouseEntered(e -> okBtn.setStyle(
                "-fx-background-color: #A93226; -fx-text-fill: white;" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-padding: 10 24 10 24;"
        ));
        okBtn.setOnMouseExited(e -> okBtn.setStyle(
                "-fx-background-color: #C0392B; -fx-text-fill: white;" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-padding: 10 24 10 24;"
        ));

        // Bouton Annuler — outline
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.setText("Annuler");
        cancelBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #7F5539;" +
                        "-fx-border-color: #7F5539;" +
                        "-fx-border-radius: 8;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 10 24 10 24;"
        );

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        AppRequest request = new AppRequest.Builder()
                                .controller("Produit")
                                .action("supprimer")
                                .parameter("id", produit.getId())
                                .authToken(client.getAuthToken())
                                .build();
                        AppResponse response = client.sendAndParse(request);
                        Platform.runLater(() -> {
                            if (response.isSuccess()) chargerProduits();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }
}