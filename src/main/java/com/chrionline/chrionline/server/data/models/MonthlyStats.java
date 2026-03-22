package com.chrionline.chrionline.server.data.models;

public class MonthlyStats {
    private int year;
    private int month;
    private String monthName;
    private long count;

    public MonthlyStats(int year, int month, String monthName, long count) {
        this.year = year;
        this.month = month;
        this.monthName = monthName;
        this.count = count;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public String getMonthName() {
        return monthName;
    }

    public void setMonthName(String monthName) {
        this.monthName = monthName;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}