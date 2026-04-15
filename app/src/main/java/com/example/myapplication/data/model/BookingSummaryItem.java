package com.example.myapplication.data.model;

public class BookingSummaryItem {
    private final Long id;
    private final String activityName;
    private final String status;
    private final String date;
    private final String price;

    public BookingSummaryItem(Long id, String activityName, String status,
                              String date, String price) {
        this.id = id;
        this.activityName = activityName;
        this.status = status;
        this.date = date;
        this.price = price;
    }

    public Long getId() { return id; }
    public String getActivityName() { return activityName; }
    public String getStatus() { return status; }
    public String getDate() { return date; }
    public String getPrice() { return price; }
}
