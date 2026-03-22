package com.chrionline.chrionline.shared.models;

import java.time.LocalDateTime;

public class UserSummary {
    private Long userId;
    private String username;
    private String email;
    private LocalDateTime registrationDate;
    private long orderCount;

    public UserSummary(){}
    public UserSummary(Long userId, String username, String email, LocalDateTime registrationDate, long orderCount) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.registrationDate = registrationDate;
        this.orderCount = orderCount;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }
}
