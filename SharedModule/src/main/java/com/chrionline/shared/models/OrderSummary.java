package com.chrionline.shared.models;

import com.chrionline.core.enums.StatutCommande;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderSummary {
    private Long orderId;
    private String uuid;
    private String username;
    private String email;
    private BigDecimal total;
    private StatutCommande status;
    private LocalDateTime date;

    public OrderSummary() {}

    public OrderSummary(Long orderId, String uuid, String username, String email, BigDecimal total, StatutCommande status, LocalDateTime date) {
        this.orderId = orderId;
        this.uuid = uuid;
        this.username = username;
        this.email = email;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
