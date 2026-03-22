package com.chrionline.chrionline.server.data.models;

import com.chrionline.chrionline.shared.models.*;

import java.math.BigDecimal;
import java.util.List;

public class DashboardStats {
    private long totalProducts;
    private long totalCategories;
    private long totalUsers;
    private long totalOrders;
    private long totalPayments;
    private long activeUsers;
    private long pendingOrders;
    private long deliveredOrders;
    private long approvedOrders;
    private long cancelledOrders;
    private long shippedOrders;
    private long completedOrders;
    private BigDecimal totalRevenue;
    private long lowStockProducts;
    private long outOfStockProducts;
    private List<MonthlyStats> monthlyOrders;
    private List<MonthlyRevenueStats> monthlyRevenue;
    private List<MonthlyUserStats> monthlyUsers;
    private List<CategoryStats> productsByCategory;
    private List<OrderSummary> recentOrders;
    private List<UserSummary> recentUsers;

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalCategories() {
        return totalCategories;
    }

    public void setTotalCategories(long totalCategories) {
        this.totalCategories = totalCategories;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(long totalPayments) {
        this.totalPayments = totalPayments;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(long completedOrders) {
        this.completedOrders = completedOrders;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getLowStockProducts() {
        return lowStockProducts;
    }

    public void setLowStockProducts(long lowStockProducts) {
        this.lowStockProducts = lowStockProducts;
    }

    public long getOutOfStockProducts() {
        return outOfStockProducts;
    }

    public void setOutOfStockProducts(long outOfStockProducts) {
        this.outOfStockProducts = outOfStockProducts;
    }

    public List<MonthlyStats> getMonthlyOrders() {
        return monthlyOrders;
    }

    public void setMonthlyOrders(List<MonthlyStats> monthlyOrders) {
        this.monthlyOrders = monthlyOrders;
    }

    public List<MonthlyRevenueStats> getMonthlyRevenue() {
        return monthlyRevenue;
    }

    public void setMonthlyRevenue(List<MonthlyRevenueStats> monthlyRevenue) {
        this.monthlyRevenue = monthlyRevenue;
    }

    public List<MonthlyUserStats> getMonthlyUsers() {
        return monthlyUsers;
    }

    public void setMonthlyUsers(List<MonthlyUserStats> monthlyUsers) {
        this.monthlyUsers = monthlyUsers;
    }

    public List<CategoryStats> getProductsByCategory() {
        return productsByCategory;
    }

    public void setProductsByCategory(List<CategoryStats> productsByCategory) {
        this.productsByCategory = productsByCategory;
    }

    public List<OrderSummary> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<OrderSummary> recentOrders) {
        this.recentOrders = recentOrders;
    }

    public List<UserSummary> getRecentUsers() {
        return recentUsers;
    }

    public void setRecentUsers(List<UserSummary> recentUsers) {
        this.recentUsers = recentUsers;
    }

    public long getDeliveredOrders() {
        return deliveredOrders;
    }

    public void setDeliveredOrders(long deliveredOrders) {
        this.deliveredOrders = deliveredOrders;
    }

    public long getApprovedOrders() {
        return approvedOrders;
    }

    public void setApprovedOrders(long approvedOrders) {
        this.approvedOrders = approvedOrders;
    }

    public long getCancelledOrders() {
        return cancelledOrders;
    }

    public void setCancelledOrders(long cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }

    public long getShippedOrders() {
        return shippedOrders;
    }

    public void setShippedOrders(long shippedOrders) {
        this.shippedOrders = shippedOrders;
    }
}
