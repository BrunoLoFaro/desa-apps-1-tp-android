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
    private final String imageUrl;
    private final String time;
    private final boolean canReview;
    private final String sessionStartTime;

    public BookingSummaryItem(Long id, Long activityId, String activityName, String status,
                              String date, String price,
                              String destination, String guideName, int durationMinutes,
                              String imageUrl, String time, boolean canReview, String sessionStartTime) {
        this.id = id;
        this.activityId = activityId;
        this.activityName = activityName;
        this.status = status;
        this.date = date;
        this.price = price;
        this.destination = destination;
        this.guideName = guideName;
        this.durationMinutes = durationMinutes;
        this.imageUrl = imageUrl;
        this.time = time;
        this.canReview = canReview;
        this.sessionStartTime = sessionStartTime;
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
    public String getImageUrl() { return imageUrl; }
    public String getTime() { return time; }
    public boolean isCanReview() { return canReview; }
    public String getSessionStartTime() { return sessionStartTime; }
}
