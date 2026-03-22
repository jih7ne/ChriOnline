package com.chrionline.chrionline.server.data.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyStats {
    private LocalDate date;
    private long orders;
    private BigDecimal revenue;
    private long newUsers;

    public DailyStats(LocalDate date, long orders, BigDecimal revenue, long newUsers) {
        this.date = date;
        this.orders = orders;
        this.revenue = revenue;
        this.newUsers = newUsers;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getOrders() {
        return orders;
    }

    public void setOrders(long orders) {
        this.orders = orders;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public long getNewUsers() {
        return newUsers;
    }

    public void setNewUsers(long newUsers) {
        this.newUsers = newUsers;
    }
}

