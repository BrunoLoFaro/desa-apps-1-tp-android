package com.example.myapplication.data.model;

import java.util.List;

public class TourActivity {
    private String name;
    private String destination;
    private String category;
    private String duration;
    private String price;
    private int availableSlots;
    private String imageUrl;
    private String description;
    private float rating;
    private int reviewsCount;
    
    // Nuevos campos según la consigna
    private String whatIncluded;
    private String meetingPoint;
    private String guideName;
    private String language;
    private String cancellationPolicy;
    private List<String> galleryUrls;

    public TourActivity(String name, String destination, String category, String duration, String price, 
                        int availableSlots, String imageUrl, String description, float rating, 
                        int reviewsCount, String whatIncluded, String meetingPoint, 
                        String guideName, String language, String cancellationPolicy, 
                        List<String> galleryUrls) {
        this.name = name;
        this.destination = destination;
        this.category = category;
        this.duration = duration;
        this.price = price;
        this.availableSlots = availableSlots;
        this.imageUrl = imageUrl;
        this.description = description;
        this.rating = rating;
        this.reviewsCount = reviewsCount;
        this.whatIncluded = whatIncluded;
        this.meetingPoint = meetingPoint;
        this.guideName = guideName;
        this.language = language;
        this.cancellationPolicy = cancellationPolicy;
        this.galleryUrls = galleryUrls;
    }

    public String getName() { return name; }
    public String getDestination() { return destination; }
    public String getCategory() { return category; }
    public String getDuration() { return duration; }
    public String getPrice() { return price; }
    public int getAvailableSlots() { return availableSlots; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
    public float getRating() { return rating; }
    public int getReviewsCount() { return reviewsCount; }
    public String getWhatIncluded() { return whatIncluded; }
    public String getMeetingPoint() { return meetingPoint; }
    public String getGuideName() { return guideName; }
    public String getLanguage() { return language; }
    public String getCancellationPolicy() { return cancellationPolicy; }
    public List<String> getGalleryUrls() { return galleryUrls; }
}
