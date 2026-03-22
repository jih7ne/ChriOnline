package com.chrionline.chrionline.shared.models;

public class MonthlyUserStats {
    private int year;
    private int month;
    private String monthName;
    private long newUsers;


    public MonthlyUserStats(){}

    public MonthlyUserStats(int year, int month, String monthName, long newUsers) {
        this.year = year;
        this.month = month;
        this.monthName = monthName;
        this.newUsers = newUsers;
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

    public long getNewUsers() {
        return newUsers;
    }

    public void setNewUsers(long newUsers) {
        this.newUsers = newUsers;
    }
}

