package com.chrionline.chrionline.server.data.models;

import com.chrionline.chrionline.core.enums.StatutCommande;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderSummary {
    private Long orderId;
    private String username;
    private BigDecimal total;
    private StatutCommande status;
    private LocalDateTime date;

    public OrderSummary(Long orderId, String username, BigDecimal total, StatutCommande status, LocalDateTime date) {
        this.orderId = orderId;
        this.username = username;
        this.total = total;
        this.status = status;
        this.date = date;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public StatutCommande getStatus() {
        return status;
    }

    public void setStatus(StatutCommande status) {
        this.status = status;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
