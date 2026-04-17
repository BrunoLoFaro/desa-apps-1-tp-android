package com.example.myapplication.data.model;

public class BookingSummaryItem {
    private final Long id;
    private final Long activityId;
    private final String activityName;
    private final String status;
    private final String date;
    private final String price;
    private final String destination;
    private final String guideName;
    private final int durationMinutes;

    public BookingSummaryItem(Long id, Long activityId, String activityName, String status,
                              String date, String price,
                              String destination, String guideName, int durationMinutes) {
        this.id = id;
        this.activityId = activityId;
        this.activityName = activityName;
        this.status = status;
        this.date = date;
        this.price = price;
        this.destination = destination;
        this.guideName = guideName;
        this.durationMinutes = durationMinutes;
    }

    public Long getId() { return id; }
    public Long getActivityId() { return activityId; }
    public String getActivityName() { return activityName; }
    public String getStatus() { return status; }
    public String getDate() { return date; }
    public String getPrice() { return price; }
    public String getDestination() { return destination; }
    public String getGuideName() { return guideName; }
    public int getDurationMinutes() { return durationMinutes; }
}
