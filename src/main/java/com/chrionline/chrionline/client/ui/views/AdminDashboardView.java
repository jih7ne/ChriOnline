package com.chrionline.chrionline.client.ui.views;

import com.chrionline.chrionline.core.theme.AppTheme;
import com.chrionline.chrionline.network.protocol.AppRequest;
import com.chrionline.chrionline.network.protocol.AppResponse;
import com.chrionline.chrionline.network.tcp.TCPClient;
import com.chrionline.chrionline.server.data.models.DashboardStats;
import com.chrionline.chrionline.shared.models.*;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class AdminDashboardView extends BorderPane {

    private final TCPClient client;
    private VBox contentArea;
    private Label refreshTimeLabel;
    private DashboardStats currentStats;

    // Chart references for updates
    private LineChart<String, Number> ordersChart;
    private LineChart<String, Number> revenueChart;
    private PieChart categoryChart;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public AdminDashboardView(TCPClient client) {
        this.client = client;
        createMainLayout();
        loadStatistics();
    }

    private void createMainLayout() {
        setStyle(AppTheme.getBackgroundStyle());

        // Header
        setTop(createHeader());

        // Left Sidebar
        setLeft(createSidebar());

        // Content Area
        contentArea = new VBox(20);
        contentArea.setPadding(new Insets(20));
        contentArea.setStyle("-fx-background-color: transparent;");

        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.getStyleClass().add("edge-to-edge");

        setCenter(scrollPane);

        // Apply styles to scroll pane
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 30, 20, 30));
        header.setStyle("-fx-background-color: " + AppTheme.CARD_BG +
                ";-fx-border-color: " + AppTheme.FIELD_BORDER +
                ";-fx-border-width: 0 0 1 0;");
        header.setSpacing(20);

        // Title
        Label titleLabel = new Label("Admin Dashboard");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");

        // Refresh button
        Button refreshBtn = new Button("⟳ Refresh");
        AppTheme.stylePrimaryButton(refreshBtn);
        refreshBtn.setPrefWidth(120);
        refreshBtn.setOnAction(e -> loadStatistics());

        // Last update label
        refreshTimeLabel = new Label("Last update: --");
        refreshTimeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleLabel, spacer, refreshBtn, refreshTimeLabel);
        return header;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(30, 20, 30, 20));
        sidebar.setStyle("-fx-background-color: " + AppTheme.CARD_BG + ";" +
                "-fx-border-color: " + AppTheme.FIELD_BORDER + ";" +
                "-fx-border-width: 0 1 0 0;");
        sidebar.setPrefWidth(250);

        // Logo/Title
        Label logo = new Label("AdminPanel");
        logo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";");
        logo.setPadding(new Insets(0, 0, 20, 0));

        // Navigation items
        Button dashboardBtn = createNavButton("Dashboard", true);
        Button productsBtn = createNavButton("Products", false);
        Button ordersBtn = createNavButton("Orders", false);
        Button usersBtn = createNavButton("Users", false);
        Button categoriesBtn = createNavButton("Categories", false);
        Button paymentsBtn = createNavButton("Payments", false);

        // Settings at bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button settingsBtn = createNavButton("Settings", false);

        sidebar.getChildren().addAll(logo, dashboardBtn, productsBtn, ordersBtn,
                usersBtn, categoriesBtn, paymentsBtn,
                spacer, settingsBtn);

        return sidebar;
    }

    private Button createNavButton(String text, boolean isActive) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(12, 20, 12, 20));

        if (isActive) {
            AppTheme.styleToggleActive(btn);
        } else {
            AppTheme.styleToggleInactive(btn);
            btn.setOnMouseEntered(e -> btn.setStyle(
                    "-fx-background-color: " + AppTheme.FIELD_BG + ";" +
                            "-fx-text-fill: " + AppTheme.TEXT_MAIN + ";" +
                            "-fx-font-size: 14px;" +
                            "-fx-background-radius: 30px;" +
                            "-fx-padding: 12px 20px;" +
                            "-fx-cursor: hand;"
            ));
            btn.setOnMouseExited(e -> AppTheme.styleToggleInactive(btn));
        }

        return btn;
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

            // Update refresh time
            refreshTimeLabel.setText("Last update: " +
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        } catch (Exception e) {
            showError("Failed to load statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void refreshContent() {
        contentArea.getChildren().clear();

        // Stats Cards Grid
        contentArea.getChildren().add(createStatsGrid());

        // Charts Section
        contentArea.getChildren().add(createChartsSection());

        // Recent Activity Section
        contentArea.getChildren().add(createRecentActivitySection());

    }

    private GridPane createStatsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(0, 0, 10, 0));

        // Row 1 - Main metrics
        grid.add(createStatCard("Total Products",
                String.valueOf(currentStats.getTotalProducts()),
                "Products in catalog"), 0, 0);
        grid.add(createStatCard("Categories",
                String.valueOf(currentStats.getTotalCategories()),
                "Product categories"), 1, 0);
        grid.add(createStatCard("Total Users",
                String.valueOf(currentStats.getTotalUsers()),
                "Registered users"), 2, 0);
        grid.add(createStatCard("Total Orders",
                String.valueOf(currentStats.getTotalOrders()),
                "All time orders"), 3, 0);

        // Row 2 - Financial metrics
        grid.add(createStatCard("Total Revenue",
                formatCurrency(currentStats.getTotalRevenue()),
                "From completed payments"), 0, 1);

        // Row 3 - Order status & stock
        grid.add(createStatCard("Pending Orders",
                String.valueOf(currentStats.getPendingOrders()),
                "Awaiting processing"), 0, 2);
        grid.add(createStatCard("Completed Orders",
                String.valueOf(currentStats.getCompletedOrders()),
                "Successfully delivered"), 1, 2);
        grid.add(createStatCard("Low Stock",
                String.valueOf(currentStats.getLowStockProducts()),
                "Products below 10 units"), 2, 2);
        grid.add(createStatCard("Out of Stock",
                String.valueOf(currentStats.getOutOfStockProducts()),
                "Products unavailable"), 3, 2);

        return grid;
    }

    private VBox createStatCard(String title, String value, String subtitle) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        AppTheme.styleCard(card);
        card.setPrefWidth(200);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.PRIMARY + ";");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + AppTheme.TEXT_MUTED + ";");

        card.getChildren().addAll(titleLabel, valueLabel, subtitleLabel);
        return card;
    }

    private VBox createChartsSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(0, 0, 20, 0));

        Label sectionTitle = new Label("Analytics Overview");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");

        HBox chartsBox = new HBox(20);
        chartsBox.setAlignment(Pos.CENTER);

        // Orders Chart
        VBox ordersChartCard = createChartCard("Monthly Orders", createOrdersChart());
        // Revenue Chart
        VBox revenueChartCard = createChartCard("Monthly Revenue", createRevenueChart());
        // Category Distribution
        VBox categoryChartCard = createChartCard("Products by Category", createCategoryChart());

        chartsBox.getChildren().addAll(ordersChartCard, revenueChartCard, categoryChartCard);

        section.getChildren().addAll(sectionTitle, chartsBox);
        return section;
    }

    private VBox createChartCard(String title, Chart chart) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        AppTheme.styleCard(card);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");

        card.getChildren().addAll(titleLabel, chart);
        return card;
    }

    private LineChart<String, Number> createOrdersChart() {
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Month");
        yAxis.setLabel("Number of Orders");

        ordersChart = new LineChart<>(xAxis, yAxis);
        ordersChart.setAnimated(false);
        ordersChart.setCreateSymbols(true);
        ordersChart.setPrefHeight(300);
        ordersChart.setPrefWidth(350);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Orders");

        if (currentStats != null && currentStats.getMonthlyOrders() != null) {
            for (MonthlyStats stats : currentStats.getMonthlyOrders()) {
                series.getData().add(new XYChart.Data<>(stats.getMonthName(), stats.getCount()));
            }
        }

        ordersChart.getData().add(series);
        return ordersChart;
    }

    private LineChart<String, Number> createRevenueChart() {
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Month");
        yAxis.setLabel("Revenue (€)");

        revenueChart = new LineChart<>(xAxis, yAxis);
        revenueChart.setAnimated(false);
        revenueChart.setCreateSymbols(true);
        revenueChart.setPrefHeight(300);
        revenueChart.setPrefWidth(350);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");

        if (currentStats != null && currentStats.getMonthlyRevenue() != null) {
            for (MonthlyRevenueStats stats : currentStats.getMonthlyRevenue()) {
                series.getData().add(new XYChart.Data<>(stats.getMonthName(), stats.getRevenue()));
            }
        }

        revenueChart.getData().add(series);
        return revenueChart;
    }

    private PieChart createCategoryChart() {
        categoryChart = new PieChart();
        categoryChart.setPrefHeight(300);
        categoryChart.setPrefWidth(350);
        categoryChart.setLabelsVisible(true);
        categoryChart.setLegendVisible(true);

        if (currentStats != null && currentStats.getProductsByCategory() != null) {
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (CategoryStats stats : currentStats.getProductsByCategory()) {
                pieData.add(new PieChart.Data(stats.getCategoryName(), stats.getProductCount()));
            }
            categoryChart.setData(pieData);
        }

        return categoryChart;
    }

    private VBox createRecentActivitySection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(0, 0, 20, 0));

        Label sectionTitle = new Label("Recent Activity");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");

        HBox activityBox = new HBox(20);
        activityBox.setAlignment(Pos.CENTER);

        // Recent Orders Table
        VBox ordersCard = createRecentOrdersTable();
        // Recent Users Table
        VBox usersCard = createRecentUsersTable();

        activityBox.getChildren().addAll(ordersCard, usersCard);

        section.getChildren().addAll(sectionTitle, activityBox);
        return section;
    }

    private VBox createRecentOrdersTable() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        AppTheme.styleCard(card);
        card.setPrefWidth(400);

        Label title = new Label("Recent Orders");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");

        TableView<OrderSummary> tableView = new TableView<>();
        tableView.setStyle("-fx-background-color: transparent;");

        TableColumn<OrderSummary, Long> idCol = new TableColumn<>("Order ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        idCol.setPrefWidth(80);

        TableColumn<OrderSummary, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        userCol.setPrefWidth(100);

        TableColumn<OrderSummary, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(formatCurrency(cellData.getValue().getTotal())));
        totalCol.setPrefWidth(80);

        TableColumn<OrderSummary, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatus().toString()));
        statusCol.setPrefWidth(80);

        TableColumn<OrderSummary, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getDate().format(dateFormatter)));
        dateCol.setPrefWidth(120);

        tableView.getColumns().addAll(idCol, userCol, totalCol, statusCol, dateCol);

        if (currentStats != null && currentStats.getRecentOrders() != null) {
            tableView.setItems(FXCollections.observableArrayList(currentStats.getRecentOrders()));
        }

        tableView.setPrefHeight(250);

        card.getChildren().addAll(title, tableView);
        return card;
    }

    private VBox createRecentUsersTable() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        AppTheme.styleCard(card);
        card.setPrefWidth(400);

        Label title = new Label("New Users");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + AppTheme.TEXT_MAIN + ";");

        TableView<UserSummary> tableView = new TableView<>();
        tableView.setStyle("-fx-background-color: transparent;");

        TableColumn<UserSummary, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameCol.setPrefWidth(100);

        TableColumn<UserSummary, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(150);

        TableColumn<UserSummary, String> dateCol = new TableColumn<>("Joined");
        dateCol.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getRegistrationDate().format(dateFormatter)));
        dateCol.setPrefWidth(120);

        TableColumn<UserSummary, Long> ordersCol = new TableColumn<>("Orders");
        ordersCol.setCellValueFactory(new PropertyValueFactory<>("orderCount"));
        ordersCol.setPrefWidth(80);

        tableView.getColumns().addAll(usernameCol, emailCol, dateCol, ordersCol);

        if (currentStats != null && currentStats.getRecentUsers() != null) {
            tableView.setItems(FXCollections.observableArrayList(currentStats.getRecentUsers()));
        }

        tableView.setPrefHeight(250);

        card.getChildren().addAll(title, tableView);
        return card;
    }



    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "€0.00";
        return String.format("€%.2f", amount);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}