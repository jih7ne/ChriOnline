package com.chrionline.chrionline.server.data.models;

import java.math.BigDecimal;

public class PaymentMethodStats {
    private String paymentMethod;
    private long count;
    private BigDecimal totalAmount;

    public PaymentMethodStats(String paymentMethod, long count, BigDecimal totalAmount) {
        this.paymentMethod = paymentMethod;
        this.count = count;
        this.totalAmount = totalAmount;
    }


    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
