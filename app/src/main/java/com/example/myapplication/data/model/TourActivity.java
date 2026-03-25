package com.example.myapplication.data.model;

public class TourActivity {
    private String name;
    private String destination;
    private String category;
    private String duration;
    private String price;
    private int availableSlots;
    private String imageUrl;

    public TourActivity(String name, String destination, String category, String duration, String price, int availableSlots, String imageUrl) {
        this.name = name;
        this.destination = destination;
        this.category = category;
        this.duration = duration;
        this.price = price;
        this.availableSlots = availableSlots;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public String getDestination() { return destination; }
    public String getCategory() { return category; }
    public String getDuration() { return duration; }
    public String getPrice() { return price; }
    public int getAvailableSlots() { return availableSlots; }
    public String getImageUrl() { return imageUrl; }
}
