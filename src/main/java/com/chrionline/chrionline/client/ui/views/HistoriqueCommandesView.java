package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.core.theme.AppTheme;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.network.tcp.TCPClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import com.chrionline.chrionline.core.interfaces.ViewManager;
import com.chrionline.chrionline.client.ui.components.ClientNavbar;

import java.time.format.DateTimeFormatter;
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
        backBtn.setOnMouseClicked(e -> {
            if (this.onBackToCatalog != null) {
                this.onBackToCatalog.run();
            }
        });

        Label title = new Label("Mes commandes");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #5C3D2E;");
        
        headerRow.getChildren().addAll(backBtn, title);

        // FILTERS
        HBox filtersBox = new HBox(12);
        filtersBox.setAlignment(Pos.CENTER_LEFT);

        String[] filters = {"Toutes", "En attente", "Validée", "Expédiée", "Livrée"};
        for (String filter : filters) {
            StackPane chip = createFilterChip(filter);
            filtersBox.getChildren().add(chip);
        }

        listContainer = new VBox(16);
        listContainer.setAlignment(Pos.TOP_CENTER);

        root.getChildren().addAll(headerRow, filtersBox, listContainer);
        scroll.setContent(root);
        setCenter(scroll);

        chargerHistorique();
    }

    private StackPane createFilterChip(String text) {
        StackPane chip = new StackPane();
        Label label = new Label(text);
        
        Runnable updateStyle = () -> {
            boolean isActive = currentFilter.equals(text);
            String bgColor = isActive ? "#7F5539" : "#E6CCB2";
            String textColor = isActive ? "white" : "#7F5539";
            
            chip.setStyle(
                    "-fx-background-color: " + bgColor + ";" +
                    "-fx-background-radius: 8px;" +
                    "-fx-cursor: hand;"
            );
            label.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 14px;");
        };

        updateStyle.run();
        chip.setPadding(new Insets(8, 16, 8, 16));
        chip.getChildren().add(label);

        chip.setOnMouseClicked(e -> {
            currentFilter = text;
            // Update all chips styles (simplified by just redrawing the list with the filter)
            // But we need to update the visual state of the chips. We will do this by re-creating or looping through children.
            // For now, let's just re-apply styles to all siblings
            HBox parent = (HBox) chip.getParent();
            for (javafx.scene.Node node : parent.getChildren()) {
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
                        // Show error
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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy");

        for (Map<String, Object> cmd : allCommandes) {
            String statut = String.valueOf(cmd.get("statut")).toLowerCase();
            
            // Apply filtering logic based on the uppercase string presentation
            boolean match = currentFilter.equals("Toutes") || 
                            currentFilter.toLowerCase().replace("é", "e").equals(statut.replace("_", " "));
            
            if (!match) continue;

            String uuid = String.valueOf(cmd.get("uuid_commande"));
            String displayId = "#" + uuid.toUpperCase().substring(0, Math.min(uuid.length(), 13));
            
            double total = ((Number) cmd.get("prix_total")).doubleValue();

            // Card container
            VBox card = new VBox(8);
            card.setPadding(new Insets(24));
            card.setStyle(
                    "-fx-background-color: #F5EAE0;" + // Using the exact color from standard cards
                    "-fx-background-radius: 16px;" +
                    "-fx-cursor: hand;"
            );

            // First Row: Title + Badge + Arrow
            HBox topRow = new HBox();
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label titleLabel = new Label("Commande " + displayId);
            titleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #5C3D2E;");

            StackPane badge = createStatusBadge(statut);
            HBox.setMargin(badge, new Insets(0, 0, 0, 16));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            FontIcon arrowIcon = new FontIcon(Feather.CHEVRON_DOWN);
            arrowIcon.setIconSize(18);
            arrowIcon.setIconColor(Color.web("#7F5539"));

            topRow.getChildren().addAll(titleLabel, badge, spacer, arrowIcon);

            // Second Row: Date + Items + Price
            HBox bottomRow = new HBox(24);
            bottomRow.setAlignment(Pos.CENTER_LEFT);

            // Assuming we don't fetch item count initially in `lister`.
            // We can fake it or fetch details. For now we will just put "Voir détails"
            Label dateLabel = new Label("Voir détails..."); // Placeholder
            dateLabel.setStyle("-fx-text-fill: " + AppTheme.TEXT_MUTED + "; -fx-font-size: 13px;");

            Label priceLabel = new Label(String.format("%.2f€", total));
            priceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #5C3D2E; -fx-font-size: 14px;");

            bottomRow.getChildren().addAll(dateLabel, priceLabel);

            card.getChildren().addAll(topRow, bottomRow);

            // Fetch details for the date and articles
            fetchOrderDetails(card, bottomRow, ((Number) cmd.get("id_commande")).intValue(), cmd);

            // Hover effect
            card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #EFE2D5; -fx-background-radius: 16px; -fx-cursor: hand;"));
            card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #F5EAE0; -fx-background-radius: 16px; -fx-cursor: hand;"));

            listContainer.getChildren().add(card);
        }
    }

    private void fetchOrderDetails(VBox card, HBox bottomRow, int idCommande, Map<String, Object> cmd) {
        new Thread(() -> {
            try {
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("idCommande", idCommande);
                AppRequest req = new AppRequest.Builder()
                        .controller("Commande")
                        .action("details")
                        .payload(payload)
                        .build();
                String jsonRes = client.sendRequest(req);
                AppResponse res = AppResponse.fromJson(jsonRes);

                Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        Map<String, Object> data = (Map<String, Object>) res.getData();
                        List<Map<String, Object>> lignes = (List<Map<String, Object>>) data.get("lignes");
                        
                        int articlesCount = lignes != null ? lignes.size() : 0;
                        
                        // Parse date from commande
                        Object dateObj = cmd.get("date");
                        String dateStr = "";
                        if (dateObj != null) {
                            try {
                                if (dateObj instanceof java.util.Map) {
                                    Map<String, Object> dm = (java.util.Map<String, Object>) dateObj;
                                    java.time.LocalDate ld = java.time.LocalDate.of(
                                        ((Number) dm.get("year")).intValue(),
                                        ((Number) dm.get("monthValue")).intValue(),
                                        ((Number) dm.get("dayOfMonth")).intValue()
                                    );
                                    dateStr = ld.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.FRANCE));
                                } else {
                                    dateStr = dateObj.toString();
                                }
                            } catch (Exception e) {
                                dateStr = dateObj.toString();
                            }
                        }

                        Label dateL = new Label(dateStr);
                        dateL.setStyle("-fx-text-fill: #7F5539; -fx-font-size: 13px;");

                        Label artL = new Label(articlesCount + (articlesCount > 1 ? " articles" : " article"));
                        artL.setStyle("-fx-text-fill: #7F5539; -fx-font-size: 13px;");

                        // Add to UI
                        bottomRow.getChildren().clear();
                        bottomRow.getChildren().addAll(dateL, artL, bottomRow.getChildren().isEmpty() ? new Label("") : bottomRow.getChildren().get(bottomRow.getChildren().size()-1));
                        
                        // Retain the price label from original
                        Label priceLabel = new Label(String.format("%.2f€", ((Number) cmd.get("prix_total")).doubleValue()));
                        priceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #5C3D2E; -fx-font-size: 14px;");
                        bottomRow.getChildren().add(priceLabel);
                    }
                });
            } catch (Exception e) {}
        }).start();
    }

    private StackPane createStatusBadge(String statutCode) {
        StackPane badge = new StackPane();
        badge.setPadding(new Insets(4, 12, 4, 12));
        
        String bg = "#E6CCB2"; // Default
        String textCol = "white";
        String labelText = "Inconnu";

        if (statutCode.equals("en_attente")) {
            bg = "#E6CCB2";
            textCol = "#7F5539";
            labelText = "En attente";
        } else if (statutCode.equals("validee")) {
            bg = "#7F5539"; // Using standard brown for Validée if we don't want green yet
            labelText = "Validée";
        } else if (statutCode.equals("expediee")) {
            bg = "#7F5539"; // Darker brown
            labelText = "Expédiée";
        } else if (statutCode.equals("livree")) {
            bg = "#00C853"; // Green
            labelText = "Livrée";
        } else if (statutCode.equals("annulee")) {
            bg = "#E74C3C"; // Red
            labelText = "Annulée";
        }

        badge.setStyle(
                "-fx-background-color: " + bg + ";" +
                "-fx-background-radius: 12px;"
        );

        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: " + textCol + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        badge.getChildren().add(lbl);

        return badge;
    }
}
