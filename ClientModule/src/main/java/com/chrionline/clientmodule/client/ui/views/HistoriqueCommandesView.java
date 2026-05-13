package com.chrionline.clientmodule.client.ui.views;

import com.chrionline.core.theme.AppTheme;
import com.chrionline.core.network.protocol.AppRequest;
import com.chrionline.core.network.protocol.AppResponse;
import com.chrionline.network.tcp.TCPClient;
import com.chrionline.core.interfaces.ViewManager;
import com.chrionline.clientmodule.client.ui.components.ClientNavbar;

import com.chrionline.shared.models.PanierProduit;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HistoriqueCommandesView extends BorderPane {

    private final TCPClient client;
    private final Map<String, Object> userData;
    private final VBox listContainer;
    private List<Map<String, Object>> allCommandes = new ArrayList<>();
    private String currentFilter = "Toutes";
    private final Runnable onBackToCatalog;
    private final ViewManager viewManager;

    public HistoriqueCommandesView(TCPClient client, Map<String, Object> userData, Runnable onBackToCatalog, ViewManager viewManager) {
        this.client = client;
        this.userData = userData;
        this.onBackToCatalog = onBackToCatalog;
        this.viewManager = viewManager;

        setStyle("-fx-background-color: " + AppTheme.BG + ";");

        ClientNavbar navbar = new ClientNavbar(0, userData, viewManager, null);
        setTop(navbar);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + AppTheme.BG + "; -fx-background-color: " + AppTheme.BG + ";");

        VBox root = new VBox(24);
        root.setPadding(new Insets(48, 64, 48, 64));
        root.setAlignment(Pos.TOP_LEFT);

        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        StackPane backBtn = new StackPane();
        backBtn.setPadding(new Insets(8));
        FontIcon backIcon = new FontIcon(Feather.ARROW_LEFT);
        backIcon.setIconSize(24);
        backIcon.setIconColor(Color.web("#7F5539"));
        backBtn.getChildren().add(backIcon);
        backBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 50%;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle("-fx-background-color: #E6CCB2; -fx-cursor: hand; -fx-background-radius: 50%;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 50%;"));
        backBtn.setOnMouseClicked(e -> { if (this.onBackToCatalog != null) this.onBackToCatalog.run(); });

        Label title = new Label("Mes commandes");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #5C3D2E;");
        headerRow.getChildren().addAll(backBtn, title);

        // FILTERS
        HBox filtersBox = new HBox(12);
        filtersBox.setAlignment(Pos.CENTER_LEFT);
        String[] filters = {"Toutes", "En attente", "Validée", "Annulée"};
        for (String filter : filters) {
            filtersBox.getChildren().add(createFilterChip(filter, filtersBox));
        }

        listContainer = new VBox(16);
        listContainer.setAlignment(Pos.TOP_CENTER);

        root.getChildren().addAll(headerRow, filtersBox, listContainer);
        scroll.setContent(root);
        setCenter(scroll);

        chargerHistorique();
    }

    private StackPane createFilterChip(String text, HBox filtersBox) {
        StackPane chip = new StackPane();
        Label label = new Label(text);

        Runnable applyStyle = () -> {
            boolean isActive = currentFilter.equals(text);
            chip.setStyle("-fx-background-color: " + (isActive ? "#7F5539" : "#E6CCB2") + "; -fx-background-radius: 8px; -fx-cursor: hand;");
            label.setStyle("-fx-text-fill: " + (isActive ? "white" : "#7F5539") + "; -fx-font-size: 14px;");
        };

        applyStyle.run();
        chip.setPadding(new Insets(8, 16, 8, 16));
        chip.getChildren().add(label);

        chip.setOnMouseClicked(e -> {
            currentFilter = text;
            for (javafx.scene.Node node : filtersBox.getChildren()) {
                StackPane c = (StackPane) node;
                Label l = (Label) c.getChildren().get(0);
                boolean act = currentFilter.equals(l.getText());
                c.setStyle("-fx-background-color: " + (act ? "#7F5539" : "#E6CCB2") + "; -fx-background-radius: 8px; -fx-cursor: hand;");
                l.setStyle("-fx-text-fill: " + (act ? "white" : "#7F5539") + "; -fx-font-size: 14px;");
            }
            afficherCommandes();
        });

        return chip;
    }

    private void chargerHistorique() {
        int idClient = ((Number) userData.get("id")).intValue();

        new Thread(() -> {
            try {
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("idUtilisateur", idClient);
                AppRequest request = new AppRequest.Builder()
                        .controller("Commande")
                        .action("lister")
                        .payload(payload)
                        .build();

                String jsonResponse = client.sendRequest(request);
                AppResponse response = AppResponse.fromJson(jsonResponse);

                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        allCommandes = (List<Map<String, Object>>) response.getData();
                        afficherCommandes();
                    } else {
                        Label err = new Label("Erreur de chargement: " + response.getMessage());
                        listContainer.getChildren().setAll(err);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void afficherCommandes() {
        listContainer.getChildren().clear();

        if (allCommandes == null || allCommandes.isEmpty()) {
            Label vide = new Label("Vous n'avez pas encore passé de commande.");
            vide.setStyle("-fx-text-fill: " + AppTheme.TEXT_MUTED + "; -fx-font-size: 16px;");
            listContainer.getChildren().add(vide);
            return;
        }

        for (Map<String, Object> cmd : allCommandes) {
            String statut = String.valueOf(cmd.get("statut")).toLowerCase();

            boolean match = currentFilter.equals("Toutes") ||
                    currentFilter.toLowerCase().replace("é", "e").equals(statut.replace("_", " "));
            if (!match) continue;

            String uuid = String.valueOf(cmd.get("uuid_commande"));
            String displayId = "#" + uuid.toUpperCase().substring(0, Math.min(uuid.length(), 13));
            double total = ((Number) cmd.get("prix_total")).doubleValue();
            int idCommande = ((Number) cmd.get("id_commande")).intValue();

            // ── Card principale ──
            VBox card = new VBox(0);
            card.setStyle(cardStyle(false));

            // ── Ligne du haut : titre + badge + spacer + flèche ──
            HBox topRow = new HBox();
            topRow.setAlignment(Pos.CENTER_LEFT);
            topRow.setPadding(new Insets(20, 24, 20, 24));

            Label titleLabel = new Label("Commande " + displayId);
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #5C3D2E;");

            StackPane badge = createStatusBadge(statut);
            HBox.setMargin(badge, new Insets(0, 0, 0, 14));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            FontIcon arrowIcon = new FontIcon(Feather.CHEVRON_DOWN);
            arrowIcon.setIconSize(18);
            arrowIcon.setIconColor(Color.web("#7F5539"));

            StackPane arrowWrapper = new StackPane(arrowIcon);
            arrowWrapper.setPadding(new Insets(4));
            arrowWrapper.setStyle("-fx-cursor: hand;");

            topRow.getChildren().addAll(titleLabel, badge, spacer, arrowWrapper);

            // ── Ligne du bas : date + articles + prix (toujours visible) ──
            HBox summaryRow = new HBox(20);
            summaryRow.setAlignment(Pos.CENTER_LEFT);
            summaryRow.setPadding(new Insets(0, 24, 16, 24));

            Label dateLabel = new Label("Chargement...");
            dateLabel.setStyle("-fx-text-fill: #7F5539; -fx-font-size: 13px;");

            Label artLabel = new Label("");
            artLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #7F5539; -fx-font-size: 13px;");

            Region spacerSum = new Region();
            HBox.setHgrow(spacerSum, Priority.ALWAYS);

            Label priceLabel = new Label(String.format("%.2f MAD", total));
            priceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #5C3D2E; -fx-font-size: 14px;");

            summaryRow.getChildren().addAll(dateLabel, artLabel, spacerSum, priceLabel);

            // ── Panneau de détails (caché par défaut) ──
            VBox detailsPane = new VBox(0);
            detailsPane.setVisible(false);
            detailsPane.setManaged(false);
            detailsPane.setStyle("-fx-background-color: #EFE2D5; -fx-padding: 0 24 16 24;");

            Separator sep = new Separator();
            sep.setStyle("-fx-background-color: #D4B896;");
            detailsPane.getChildren().add(sep);

            // ── État du toggle ──
            final boolean[] expanded = {false};

            // ── Toggle au clic sur la flèche ──
            Runnable toggleDetails = () -> {
                expanded[0] = !expanded[0];
                detailsPane.setVisible(expanded[0]);
                detailsPane.setManaged(expanded[0]);
                arrowIcon.setIconCode(expanded[0] ? Feather.CHEVRON_UP : Feather.CHEVRON_DOWN);
                card.setStyle(cardStyle(expanded[0]));

                if (expanded[0] && detailsPane.getChildren().size() == 1) {
                    // Premier chargement des détails
                    chargerDetails(detailsPane, idCommande, cmd);
                }
            };

            arrowWrapper.setOnMouseClicked(e -> { toggleDetails.run(); e.consume(); });
            topRow.setOnMouseClicked(e -> toggleDetails.run());
            topRow.setStyle("-fx-cursor: hand;");

            card.getChildren().addAll(topRow, summaryRow, detailsPane);

            // Pré-charger date + count articles en arrière-plan
            prechargerResume(dateLabel, artLabel, idCommande, cmd);

            // Hover effect (seulement quand fermé)
            card.setOnMouseEntered(e -> { if (!expanded[0]) card.setStyle("-fx-background-color: #EFE2D5; -fx-background-radius: 16px;"); });
            card.setOnMouseExited(e -> { if (!expanded[0]) card.setStyle(cardStyle(false)); });

            listContainer.getChildren().add(card);
        }
    }

    /** Pré-charge uniquement la date et le nombre d'articles pour le résumé */
    private void prechargerResume(Label dateLabel, Label artLabel, int idCommande, Map<String, Object> cmd) {
        new Thread(() -> {
            try {
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("idCommande", idCommande);
                AppRequest req = new AppRequest.Builder()
                        .controller("Commande").action("details").payload(payload).build();
                AppResponse res = AppResponse.fromJson(client.sendRequest(req));

                Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        Map<String, Object> data = (Map<String, Object>) res.getData();
                        List<Map<String, Object>> lignes = (List<Map<String, Object>>) data.get("lignes");
                        int count = lignes != null ? lignes.size() : 0;
                        artLabel.setText(count + (count > 1 ? " articles" : " article"));
                    }
                    dateLabel.setText(parseDate(cmd.get("date")));
                });
            } catch (Exception ignored) {}
        }).start();
    }

    /** Charge et affiche le détail complet des lignes dans le panneau expansible */
    private void chargerDetails(VBox detailsPane, int idCommande, Map<String, Object> cmd) {
        new Thread(() -> {
            try {
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("idCommande", idCommande);
                AppRequest req = new AppRequest.Builder()
                        .controller("Commande").action("details").payload(payload).build();
                AppResponse res = AppResponse.fromJson(client.sendRequest(req));

                Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        Map<String, Object> data = (Map<String, Object>) res.getData();
                        List<Map<String, Object>> lignes = (List<Map<String, Object>>) data.get("lignes");

                        if (lignes == null || lignes.isEmpty()) {
                            Label noItem = new Label("Aucun article trouvé.");
                            noItem.setStyle("-fx-text-fill: " + AppTheme.TEXT_MUTED + "; -fx-font-size: 13px; -fx-padding: 8 0 0 0;");
                            detailsPane.getChildren().add(noItem);
                            return;
                        }

                        for (Map<String, Object> ligne : lignes) {
                            String nom = String.valueOf(ligne.getOrDefault("nom_produit", ligne.getOrDefault("nom", "Produit")));
                            int qte = ((Number) ligne.getOrDefault("quantite", 1)).intValue();
                            double prix = ((Number) ligne.getOrDefault("prix_unitaire", 0)).doubleValue();

                            Label nomLbl = new Label(nom);
                            nomLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #5C3D2E; -fx-font-weight: bold;");
                            HBox.setHgrow(nomLbl, Priority.ALWAYS);

                            Label qteLbl = new Label(qte + " × " + String.format("%.2f MAD", prix));
                            qteLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #7F5539;");

                            Region sp = new Region();
                            HBox.setHgrow(sp, Priority.ALWAYS);

                            Label montantLbl = new Label(String.format("%.2f MAD", prix * qte));
                            montantLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #5C3D2E;");

                            VBox leftInfo = new VBox(2, nomLbl, qteLbl);
                            HBox.setHgrow(leftInfo, Priority.ALWAYS);

                            HBox row = new HBox(leftInfo, montantLbl);
                            row.setAlignment(Pos.CENTER_LEFT);
                            row.setPadding(new Insets(10, 0, 0, 0));

                            detailsPane.getChildren().add(row);
                        }

                        // Ligne totale
                        Separator sepTotal = new Separator();
                        sepTotal.setStyle("-fx-background-color: #D4B896;");
                        sepTotal.setPadding(new Insets(8, 0, 0, 0));

                        Label totalTxt = new Label("Total");
                        totalTxt.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #5C3D2E;");
                        Region sp2 = new Region();
                        HBox.setHgrow(sp2, Priority.ALWAYS);
                        double total = ((Number) cmd.get("prix_total")).doubleValue();
                        Label totalVal = new Label(String.format("%.2f MAD", total));
                        totalVal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #7F5539;");
                        HBox totalRow = new HBox(totalTxt, sp2, totalVal);
                        totalRow.setAlignment(Pos.CENTER_LEFT);
                        totalRow.setPadding(new Insets(10, 0, 4, 0));

                        detailsPane.getChildren().addAll(sepTotal, totalRow);
                        String statut = String.valueOf(cmd.get("statut")).toLowerCase();
                        if (statut.equals("en_attente")) {
                            javafx.scene.control.Button annulerBtn = new javafx.scene.control.Button("Annuler la commande");
                            annulerBtn.setStyle(
                                    "-fx-background-color: #E74C3C; -fx-text-fill: white; " +
                                            "-fx-font-size: 13px; -fx-font-weight: bold; " +
                                            "-fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 8 20 8 20;"
                            );
                            annulerBtn.setOnMouseEntered(e -> annulerBtn.setStyle(
                                    "-fx-background-color: #C0392B; -fx-text-fill: white; " +
                                            "-fx-font-size: 13px; -fx-font-weight: bold; " +
                                            "-fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 8 20 8 20;"
                            ));
                            annulerBtn.setOnMouseExited(e -> annulerBtn.setStyle(
                                    "-fx-background-color: #E74C3C; -fx-text-fill: white; " +
                                            "-fx-font-size: 13px; -fx-font-weight: bold; " +
                                            "-fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 8 20 8 20;"
                            ));

                            annulerBtn.setOnAction(e -> confirmerEtAnnuler(idCommande, cmd));

                            javafx.scene.control.Button reprendreBtn = new javafx.scene.control.Button("Reprendre / Modifier");
                            reprendreBtn.setStyle(
                                    "-fx-background-color: #7F5539; -fx-text-fill: white; " +
                                            "-fx-font-size: 13px; -fx-font-weight: bold; " +
                                            "-fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 8 20 8 20;"
                            );
                            reprendreBtn.setOnMouseEntered(e -> reprendreBtn.setStyle(
                                    "-fx-background-color: #5C3D2E; -fx-text-fill: white; " +
                                            "-fx-font-size: 13px; -fx-font-weight: bold; " +
                                            "-fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 8 20 8 20;"
                            ));
                            reprendreBtn.setOnMouseExited(e -> reprendreBtn.setStyle(
                                    "-fx-background-color: #7F5539; -fx-text-fill: white; " +
                                            "-fx-font-size: 13px; -fx-font-weight: bold; " +
                                            "-fx-background-radius: 8px; -fx-cursor: hand; -fx-padding: 8 20 8 20;"
                            ));

                            reprendreBtn.setOnAction(e -> reprendreCommande(idCommande, cmd, lignes));

                            HBox btnRow = new HBox(12, annulerBtn, reprendreBtn);
                            btnRow.setAlignment(Pos.CENTER_RIGHT);
                            btnRow.setPadding(new Insets(12, 0, 4, 0));
                            detailsPane.getChildren().add(btnRow);
                        }
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private String parseDate(Object dateObj) {
        if (dateObj == null) return "";
        try {
            if (dateObj instanceof Map) {
                Map<String, Object> dm = (Map<String, Object>) dateObj;
                java.time.LocalDate ld = java.time.LocalDate.of(
                        ((Number) dm.get("year")).intValue(),
                        ((Number) dm.get("monthValue")).intValue(),
                        ((Number) dm.get("dayOfMonth")).intValue()
                );
                return ld.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.FRANCE));
            }
            return dateObj.toString();
        } catch (Exception e) {
            return dateObj.toString();
        }
    }

    private String cardStyle(boolean expanded) {
        return "-fx-background-color: " + (expanded ? "#EFE2D5" : "#F5EAE0") + ";" +
                "-fx-background-radius: 16px;";
    }

    private StackPane createStatusBadge(String statutCode) {
        StackPane badge = new StackPane();
        badge.setPadding(new Insets(4, 12, 4, 12));

        String bg, textCol, labelText;
        switch (statutCode) {
            case "en_attente" -> { bg = "#E6CCB2"; textCol = "#7F5539"; labelText = "En attente"; }
            case "validee"    -> { bg = "#7F5539"; textCol = "white";   labelText = "Validée"; }
            case "annulee"    -> { bg = "#E74C3C"; textCol = "white";   labelText = "Annulée"; }
            default           -> { bg = "#E6CCB2"; textCol = "#7F5539"; labelText = "Inconnu"; }
        }

        badge.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 12px;");
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: " + textCol + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        badge.getChildren().add(lbl);
        return badge;
    }
    private void confirmerEtAnnuler(int idCommande, Map<String, Object> cmd) {

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("");
        dialog.setResizable(false);

        VBox root = new VBox(24);
        root.setPadding(new Insets(32, 36, 28, 36));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #FDF6EE; -fx-background-radius: 16px;");

        // Icône
        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(64, 64);
        iconCircle.setMaxSize(64, 64);
        iconCircle.setStyle("-fx-background-color: #F5EAE0; -fx-background-radius: 32px;");
        FontIcon warnIcon = new FontIcon(Feather.ALERT_TRIANGLE);
        warnIcon.setIconSize(28);
        warnIcon.setIconColor(Color.web("#E74C3C"));
        iconCircle.getChildren().add(warnIcon);

        // Titre
        Label titre = new Label("Annuler la commande ?");
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #5C3D2E;");

        // Message
        Label msg = new Label("Cette action est irréversible.\nVoulez-vous vraiment annuler cette commande ?");
        msg.setStyle("-fx-font-size: 13px; -fx-text-fill: #7F5539; -fx-text-alignment: center;");
        msg.setAlignment(Pos.CENTER);
        msg.setWrapText(true);

        // Bouton Oui
        Button btnOui = new Button("Oui, annuler");
        btnOui.setMaxWidth(Double.MAX_VALUE);
        btnOui.setStyle(
                "-fx-background-color: #E74C3C; -fx-text-fill: white;" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10px; -fx-cursor: hand; -fx-padding: 11 24 11 24;"
        );
        btnOui.setOnMouseEntered(e -> btnOui.setStyle(
                "-fx-background-color: #C0392B; -fx-text-fill: white;" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10px; -fx-cursor: hand; -fx-padding: 11 24 11 24;"
        ));
        btnOui.setOnMouseExited(e -> btnOui.setStyle(
                "-fx-background-color: #E74C3C; -fx-text-fill: white;" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10px; -fx-cursor: hand; -fx-padding: 11 24 11 24;"
        ));

        // Bouton Non
        Button btnNon = new Button("Non, garder");
        btnNon.setMaxWidth(Double.MAX_VALUE);
        btnNon.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #7F5539; -fx-border-width: 1.5px;" +
                        "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                        "-fx-text-fill: #7F5539; -fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-cursor: hand; -fx-padding: 11 24 11 24;"
        );
        btnNon.setOnMouseEntered(e -> btnNon.setStyle(
                "-fx-background-color: #E6CCB2;" +
                        "-fx-border-color: #7F5539; -fx-border-width: 1.5px;" +
                        "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                        "-fx-text-fill: #7F5539; -fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-cursor: hand; -fx-padding: 11 24 11 24;"
        ));
        btnNon.setOnMouseExited(e -> btnNon.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #7F5539; -fx-border-width: 1.5px;" +
                        "-fx-border-radius: 10px; -fx-background-radius: 10px;" +
                        "-fx-text-fill: #7F5539; -fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-cursor: hand; -fx-padding: 11 24 11 24;"
        ));

        HBox boutons = new HBox(12, btnNon, btnOui);
        HBox.setHgrow(btnNon, Priority.ALWAYS);
        HBox.setHgrow(btnOui, Priority.ALWAYS);
        boutons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(iconCircle, titre, msg, boutons);

        Scene scene = new Scene(root, 380, 260);
        scene.setFill(Color.web("#FDF6EE"));
        dialog.setScene(scene);

        // Actions
        btnNon.setOnAction(e -> dialog.close());

        btnOui.setOnAction(e -> {
            dialog.close();
            new Thread(() -> {
                try {
                    Map<String, Object> payload = new java.util.HashMap<>();
                    payload.put("idCommande", idCommande);
                    AppRequest req = new AppRequest.Builder()
                            .controller("Commande").action("annuler")
                            .payload(payload).build();
                    AppResponse res = AppResponse.fromJson(client.sendRequest(req));

                    Platform.runLater(() -> {
                        if (res.isSuccess()) {
                            cmd.put("statut", "annulee");
                            chargerHistorique();
                        } else {
                            afficherErreurDialog(res.getMessage() != null
                                    ? res.getMessage()
                                    : "Impossible d'annuler la commande.");
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> afficherErreurDialog("Une erreur réseau s'est produite."));
                }
            }).start();
        });

        dialog.showAndWait();
    }

    private void afficherErreurDialog(String message) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(false);

        VBox root = new VBox(20);
        root.setPadding(new Insets(32, 36, 28, 36));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #FDF6EE;");

        FontIcon icon = new FontIcon(Feather.X_CIRCLE);
        icon.setIconSize(40);
        icon.setIconColor(Color.web("#E74C3C"));

        Label msg = new Label(message);
        msg.setStyle("-fx-font-size: 13px; -fx-text-fill: #5C3D2E;");
        msg.setWrapText(true);
        msg.setAlignment(Pos.CENTER);

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle(
                "-fx-background-color: #7F5539; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10px;" +
                        "-fx-padding: 10 28 10 28; -fx-cursor: hand;"
        );
        btnFermer.setOnMouseEntered(e -> btnFermer.setStyle(
                "-fx-background-color: #5C3D2E; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10px;" +
                        "-fx-padding: 10 28 10 28; -fx-cursor: hand;"
        ));
        btnFermer.setOnMouseExited(e -> btnFermer.setStyle(
                "-fx-background-color: #7F5539; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 10px;" +
                        "-fx-padding: 10 28 10 28; -fx-cursor: hand;"
        ));
        btnFermer.setOnAction(e -> dialog.close());

        root.getChildren().addAll(icon, msg, btnFermer);

        Scene scene = new Scene(root, 340, 200);
        scene.setFill(Color.web("#FDF6EE"));
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void reprendreCommande(int idCommande, Map<String, Object> cmd, List<Map<String, Object>> lignesOriginales) {
        String uuidCommande = String.valueOf(cmd.get("uuid_commande"));
        
        Platform.runLater(() -> {
            List<PanierProduit> panierItems = new ArrayList<>();
            for (Map<String, Object> ligne : lignesOriginales) {
                PanierProduit pp = new PanierProduit();
                pp.setIdProduit(((Number) ligne.getOrDefault("id_produit", 0)).intValue());
                pp.setNomProduit(String.valueOf(ligne.getOrDefault("nom_produit", ligne.getOrDefault("nom", "Produit"))));
                pp.setQuantite(((Number) ligne.get("quantite")).intValue());
                pp.setPrix(((Number) ligne.get("prix_unitaire")).doubleValue());
                panierItems.add(pp);
            }
            viewManager.showCheckoutViewForExisting(userData, panierItems, idCommande, uuidCommande);
        });
    }
}
