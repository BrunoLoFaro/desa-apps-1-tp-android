package com.example.myapplication.data.model;

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

    public TourActivity(String name, String destination, String category, String duration, String price, int availableSlots, String imageUrl, String description, float rating, int reviewsCount) {
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
}
