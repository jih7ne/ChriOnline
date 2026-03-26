package com.chrionline.clientmodule.client.ui.views;


import com.chrionline.core.interfaces.ViewManager;
import com.chrionline.core.theme.AppTheme;
import com.chrionline.network.protocol.AppRequest;
import com.chrionline.network.protocol.AppResponse;
import com.chrionline.network.tcp.TCPClient;
import com.chrionline.shared.models.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class AdminDashboardView extends BorderPane {

    private final TCPClient client;
    private final ViewManager viewManager;
    private VBox contentArea;
    private Label refreshTimeLabel;
    private DashboardStats currentStats;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");


    private static final String CHART_CSS =
            ".chart-title { -fx-text-fill: " + AppTheme.TEXT_MAIN + "; -fx-font-size: 13px; }" +
                    ".axis-label { -fx-label-fill: " + AppTheme.TEXT_MUTED + "; -fx-font-size: 12px; }" + // ✅ FIXED
                    ".axis { -fx-tick-label-fill: " + AppTheme.TEXT_MAIN + "; }" +
                    ".chart-plot-background { -fx-background-color: transparent; }" +
                    ".chart-content { -fx-background-color: transparent; }" +
                    ".chart-series-line { -fx-stroke-width: 2.5; }" +
                    ".chart-legend-item { -fx-text-fill: " + AppTheme.TEXT_MAIN + "; }" +
                    ".chart-legend { -fx-background-color: transparent; }" +
                    ".pie-label { -fx-fill: " + AppTheme.TEXT_MAIN + "; }" +
                    ".default-color0.chart-line-symbol { -fx-background-color: " + AppTheme.TEXT_MAIN + ", black; }" +
                    ".default-color0.chart-series-line { -fx-stroke: " + AppTheme.PRIMARY + "; }";

    public AdminDashboardView(TCPClient client, ViewManager viewManager) {
        this.client = client;
        this.viewManager = viewManager;
        createMainLayout();
        loadStatistics();
    }

    private void createMainLayout() {
        setStyle(AppTheme.getBackgroundStyle());
        setTop(createHeader());

        contentArea = new VBox(24);
        contentArea.setPadding(new Insets(24, 28, 28, 28));
        contentArea.setStyle("-fx-background-color: transparent;");

        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.getStyleClass().add("edge-to-edge");

        setCenter(scrollPane);
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 28, 18, 28));
        header.setStyle(
                "-fx-background-color: " + AppTheme.CARD_BG + ";" +
                        "-fx-border-color: " + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-width: 0 0 1 0;"
        );
        header.setSpacing(16);

        Label titleLabel = new Label("Admin Dashboard");
        titleLabel.setStyle(
                "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";"
        );

        refreshTimeLabel = new Label("Last update: --");
        refreshTimeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");

        Button refreshBtn = new Button("⟳  Refresh");
        AppTheme.stylePrimaryButton(refreshBtn);
        refreshBtn.setPrefWidth(120);
        refreshBtn.setOnAction(e -> loadStatistics());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleLabel, spacer, refreshTimeLabel, refreshBtn);
        return header;
    }

    private void loadStatistics() {
        try {
            AppRequest request = new AppRequest.Builder()
                    .action("getStats")
                    .controller("Admin")
                    .build();

            AppResponse response = client.sendAndParse(request);
            currentStats = response.getDataAs(DashboardStats.class);

            refreshContent();

            refreshTimeLabel.setText("Last update: " +
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        } catch (Exception e) {
            showError("Failed to load statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void refreshContent() {
        contentArea.getChildren().clear();

        // Section: KPI Cards
        Label kpiTitle = createSectionLabel("Key Metrics");
        contentArea.getChildren().add(kpiTitle);
        contentArea.getChildren().add(createStatsGrid());

        // Divider
        contentArea.getChildren().add(createDivider());

        // Section: Analytics (Tabbed Charts)
        Label analyticsTitle = createSectionLabel("Analytics Overview");
        contentArea.getChildren().add(analyticsTitle);
        contentArea.getChildren().add(createChartsTabPane());

        // Divider
        contentArea.getChildren().add(createDivider());

        // Section: Recent Activity Tables
        Label activityTitle = createSectionLabel("Recent Activity");
        contentArea.getChildren().add(activityTitle);
        contentArea.getChildren().add(createRecentActivitySection());
    }

    // ─── Section Label ────────────────────────────────────────────────────────

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";"
        );
        return label;
    }

    private Region createDivider() {
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color: " + AppTheme.FIELD_BORDER + ";");
        VBox.setMargin(divider, new Insets(4, 0, 4, 0));
        return divider;
    }

    // ─── Stats Grid ───────────────────────────────────────────────────────────

    private FlowPane createStatsGrid() {
        FlowPane flow = new FlowPane();
        flow.setHgap(16);
        flow.setVgap(16);


        // Row 1: main metrics
        flow.getChildren().add(createStatCard("📦  Total Products",
                String.valueOf(currentStats.getTotalProducts()), "Products in catalog"));
        flow.getChildren().add(createStatCard("🏷️  Categories",
                String.valueOf(currentStats.getTotalCategories()), "Product categories"));
        flow.getChildren().add(createStatCard("👤  Total Users",
                String.valueOf(currentStats.getTotalUsers()), "Registered users"));
        flow.getChildren().add(createStatCard("🛒  Total Orders",
                String.valueOf(currentStats.getTotalOrders()), "All time orders"));

        // Row 2: financial
        flow.getChildren().add(createStatCard("💰  Total Revenue (MAD)",
                formatCurrency(currentStats.getTotalRevenue()), "From completed payments"));

        // Row 3: status & stock
        flow.getChildren().add(createStatCard("⏳  Pending Orders",
                String.valueOf(currentStats.getPendingOrders()), "Awaiting processing"));
        flow.getChildren().add(createStatCard("✅  Completed Orders",
                String.valueOf(currentStats.getCompletedOrders()), "Successfully delivered"));
        flow.getChildren().add(createStatCard("⚠️  Low Stock",
                String.valueOf(currentStats.getLowStockProducts()), "Below 10 units"));
        flow.getChildren().add(createStatCard("❌  Out of Stock",
                String.valueOf(currentStats.getOutOfStockProducts()), "Products unavailable"));

        return flow;
    }

    private VBox createStatCard(String title, String value, String subtitle) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18, 22, 18, 22));
        AppTheme.styleCard(card);
        card.setPrefWidth(190);
        card.setMinWidth(170);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        titleLabel.setWrapText(true);

        Label valueLabel = new Label(value);
        valueLabel.setStyle(
                "-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";"
        );

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");
        subtitleLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, valueLabel, subtitleLabel);
        return card;
    }

    // ─── Charts Tab Pane ──────────────────────────────────────────────────────

    private TabPane createChartsTabPane() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle(
                "-fx-background-color: " + AppTheme.CARD_BG + ";" +
                        "-fx-border-color: " + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius: 8px;" +
                        "-fx-background-radius: 8px;"
        );

        Tab ordersTab = new Tab("Monthly Orders");
        ordersTab.setContent(createChartTabContent(createOrdersChart()));

        Tab revenueTab = new Tab("Monthly Revenue");
        revenueTab.setContent(createChartTabContent(createRevenueChart()));

        Tab categoryTab = new Tab("By Category");
        categoryTab.setContent(createChartTabContent(createCategoryChart()));

        tabPane.getTabs().addAll(ordersTab, revenueTab, categoryTab);
        tabPane.setPrefHeight(420);

        return tabPane;
    }

    private VBox createChartTabContent(Chart chart) {
        VBox wrapper = new VBox(chart);
        wrapper.setPadding(new Insets(16));
        wrapper.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(chart, Priority.ALWAYS);
        return wrapper;
    }

    private LineChart<String, Number> createOrdersChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        styleAxis(xAxis, "Month");
        styleAxis(yAxis, "Number of Orders");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setPrefHeight(360);
        chart.setStyle("-fx-background-color: transparent;");
        applyChartCSS(chart);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Orders");

        if (currentStats != null && currentStats.getMonthlyOrders() != null) {
            for (MonthlyStats s : currentStats.getMonthlyOrders()) {
                series.getData().add(new XYChart.Data<>(s.getMonthName(), s.getCount()));
            }
        }

        chart.getData().add(series);
        applyChartCSS(chart);
        return chart;
    }

    private LineChart<String, Number> createRevenueChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        styleAxis(xAxis, "Month");
        styleAxis(yAxis, "Revenue (MAD)");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setPrefHeight(360);
        chart.setStyle("-fx-background-color: transparent;");
        applyChartCSS(chart);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");

        if (currentStats != null && currentStats.getMonthlyRevenue() != null) {
            for (MonthlyRevenueStats s : currentStats.getMonthlyRevenue()) {
                series.getData().add(new XYChart.Data<>(s.getMonthName(), s.getRevenue()));
            }
        }

        chart.getData().add(series);
        applyChartCSS(chart);
        return chart;
    }

    private PieChart createCategoryChart() {
        PieChart chart = new PieChart();
        chart.setPrefHeight(360);
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        chart.setStyle("-fx-background-color: transparent;");
        applyChartCSS(chart);

        if (currentStats != null && currentStats.getProductsByCategory() != null) {
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (CategoryStats s : currentStats.getProductsByCategory()) {
                pieData.add(new PieChart.Data(s.getCategoryName(), s.getProductCount()));
            }
            chart.setData(pieData);
        }

        applyChartCSS(chart);

        return chart;
    }


    private void applyChartCSS(Chart chart) {
        chart.getStylesheets().clear();

        String css = """
        .chart-title { -fx-text-fill: %s; -fx-font-size: 13px; }
        .axis-label { -fx-label-fill: %s; -fx-font-size: 12px; }
        .axis { -fx-tick-label-fill: %s; }
        .chart-plot-background { -fx-background-color: transparent; }
        .chart-content { -fx-background-color: transparent; }
        .chart-series-line { -fx-stroke-width: 2.5; }
        .chart-legend-item { -fx-text-fill: %s; }
        .chart-legend { -fx-background-color: transparent; }
        .pie-label { -fx-fill: %s; }
        .default-color0.chart-line-symbol { -fx-background-color: %s, white; }
        .default-color0.chart-series-line { -fx-stroke: %s; }
    """.formatted(
                AppTheme.TEXT_MAIN,
                AppTheme.TEXT_MUTED,
                AppTheme.TEXT_MAIN,
                AppTheme.TEXT_MAIN,
                AppTheme.TEXT_MAIN,
                AppTheme.TEXT_MAIN,
                AppTheme.PRIMARY
        );

        chart.getStylesheets().add("data:text/css," + css.replace("\n", ""));
    }


    private void styleAxis(Axis<?> axis, String label) {
        axis.setLabel(label);
        // Force label color to be readable regardless of theme
        axis.setStyle(
                "-fx-tick-label-fill: " + AppTheme.TEXT_MAIN + ";" +
                        "-fx-text-fill: " + AppTheme.TEXT_MAIN + ";" +
                        "-fx-label-fill: " + AppTheme.TEXT_MAIN + ";"
        );
        axis.lookup(".axis-label");
    }

    // ─── Recent Activity Tables ───────────────────────────────────────────────

    private HBox createRecentActivitySection() {
        HBox box = new HBox(20);
        box.setAlignment(Pos.TOP_LEFT);

        VBox ordersCard = createRecentOrdersTable();
        VBox usersCard = createRecentUsersTable();

        HBox.setHgrow(ordersCard, Priority.ALWAYS);
        HBox.setHgrow(usersCard, Priority.ALWAYS);

        box.getChildren().addAll(ordersCard, usersCard);
        return box;
    }

    private VBox createRecentOrdersTable() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        AppTheme.styleCard(card);

        Label title = new Label("Recent Orders");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");

        TableView<OrderSummary> table = new TableView<>();
        styleTable(table);

        TableColumn<OrderSummary, Long> idCol = makeColumn("Order ID", 80);
        idCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));

        TableColumn<OrderSummary, String> userCol = makeColumn("User", 110);
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<OrderSummary, String> totalCol = makeColumn("Total", 90);
        totalCol.setCellValueFactory(c -> new SimpleObjectProperty<>(formatCurrency(c.getValue().getTotal())));

        TableColumn<OrderSummary, String> statusCol = makeColumn("Status", 100);
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().toString()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(status);
                    badge.setPadding(new Insets(3, 10, 3, 10));
                    badge.setStyle(
                            "-fx-background-radius: 20px;" +
                                    "-fx-font-size: 11px;" +
                                    "-fx-font-weight: bold;" +
                                    getStatusStyle(status)
                    );
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<OrderSummary, String> dateCol = makeColumn("Date", 130);
        dateCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDate().format(dateFormatter)));

        table.getColumns().addAll(idCol, userCol, totalCol, statusCol, dateCol);

        if (currentStats != null && currentStats.getRecentOrders() != null) {
            table.setItems(FXCollections.observableArrayList(currentStats.getRecentOrders()));
        }

        table.setPrefHeight(280);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        card.getChildren().addAll(title, table);
        return card;
    }

    private VBox createRecentUsersTable() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        AppTheme.styleCard(card);

        Label title = new Label("New Users");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");

        TableView<UserSummary> table = new TableView<>();
        styleTable(table);

        TableColumn<UserSummary, String> usernameCol = makeColumn("Username", 110);
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        // Add avatar indicator
        usernameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox cell = new HBox(8);
                    cell.setAlignment(Pos.CENTER_LEFT);

                    // Simple colored circle avatar with initial
                    StackPane avatar = new StackPane();
                    Circle circle = new Circle(13);
                    circle.setFill(Color.web(AppTheme.PRIMARY, 0.2));
                    Label initial = new Label(username.substring(0, 1).toUpperCase());
                    initial.setStyle("-fx-text-fill: " + AppTheme.PRIMARY + "; -fx-font-size: 11px; -fx-font-weight: bold;");
                    avatar.getChildren().addAll(circle, initial);

                    Label nameLabel = new Label(username);
                    nameLabel.setStyle("-fx-text-fill: " + AppTheme.TEXT_MAIN + ";");

                    cell.getChildren().addAll(avatar, nameLabel);
                    setGraphic(cell);
                    setText(null);
                }
            }
        });

        TableColumn<UserSummary, String> emailCol = makeColumn("Email", 160);
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<UserSummary, String> dateCol = makeColumn("Joined", 130);
        dateCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getRegistrationDate().format(dateFormatter)));

        TableColumn<UserSummary, Long> ordersCol = makeColumn("Orders", 70);
        ordersCol.setCellValueFactory(new PropertyValueFactory<>("orderCount"));

        table.getColumns().addAll(usernameCol, emailCol, dateCol, ordersCol);

        if (currentStats != null && currentStats.getRecentUsers() != null) {
            table.setItems(FXCollections.observableArrayList(currentStats.getRecentUsers()));
        }

        table.setPrefHeight(280);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        card.getChildren().addAll(title, table);
        return card;
    }

    // ─── Table helpers ────────────────────────────────────────────────────────

    private <T> void styleTable(TableView<T> table) {
        table.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: " + AppTheme.FIELD_BORDER + ";" +
                        "-fx-border-radius: 6px;"
        );
        // Alternating row colors via row factory
        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (row.getIndex() % 2 == 0) {
                    row.setStyle("-fx-background-color: transparent;");
                } else {
                    row.setStyle("-fx-background-color: " + AppTheme.FIELD_BG + ";");
                }
            });
            // Hover highlight
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) {
                    row.setStyle("-fx-background-color: " + AppTheme.PRIMARY + "22;");
                }
            });
            row.setOnMouseExited(e -> {
                if (!row.isEmpty()) {
                    if (row.getIndex() % 2 == 0) {
                        row.setStyle("-fx-background-color: transparent;");
                    } else {
                        row.setStyle("-fx-background-color: " + AppTheme.FIELD_BG + ";");
                    }
                }
            });
            return row;
        });
    }

    private <S, T> TableColumn<S, T> makeColumn(String header, double prefWidth) {
        TableColumn<S, T> col = new TableColumn<>(header);
        col.setPrefWidth(prefWidth);
        col.setStyle("-fx-text-fill: " + AppTheme.TEXT_MAIN + ";");
        return col;
    }




    private String getStatusStyle(String status) {
        return switch (status.toUpperCase()) {
            case "COMPLETED", "DELIVERED", "VALIDEE" ->
                    "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;";
            case "PENDING","EN_ATTENTE" ->
                    "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;";
            case "CANCELLED", "CANCELED", "ANNULEE" ->
                    "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;";
            case "PROCESSING", "SHIPPED", "EXPEDIEE" ->
                    "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;";
            default ->
                    "-fx-background-color: " + AppTheme.FIELD_BG + "; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";";
        };
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0.00 MAD";
        return String.format("%.2f MAD", amount);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}