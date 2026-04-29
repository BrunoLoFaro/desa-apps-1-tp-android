package com.example.myapplication.data.model;

import java.io.Serializable;

public class TourActivity implements Serializable {
    private Long id;
    private final String name;
    private final String destination;
    private final String category;
    private final String duration;
    private final String price;
    private final int availableSlots;
    private final String imageUrl;
    private final String description;
    private float rating;
    private int reviewsCount;

    // Nuevos campos según la consigna
    private final String whatIncluded;
    private final String meetingPoint;
    private final String guideName;
    private final String language;
    private final String cancellationPolicy;
    private boolean isFavorite;
    private boolean favoriteUpdate;
    private boolean priceChanged;
    private boolean slotsChanged;
    private String startDate;

    public TourActivity(String name, String destination, String category, String duration,
                        String price, int availableSlots, String imageUrl) {
        this(name, destination, category, duration, price, availableSlots, imageUrl,
             null, 0f, 0, null, null, null, null, null, false);
    }

    public TourActivity(String name, String destination, String category, String duration, String price,
                        int availableSlots, String imageUrl, String description, float rating,
                        int reviewsCount, String whatIncluded, String meetingPoint,
                        String guideName, String language, String cancellationPolicy) {
        this(name, destination, category, duration, price, availableSlots, imageUrl,
                description, rating, reviewsCount, whatIncluded, meetingPoint,
                guideName, language, cancellationPolicy, false);
    }

    public TourActivity(String name, String destination, String category, String duration, String price,
                        int availableSlots, String imageUrl, String description, float rating,
                        int reviewsCount, String whatIncluded, String meetingPoint,
                        String guideName, String language, String cancellationPolicy,
                        boolean isFavorite) {
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
        this.isFavorite = isFavorite;
        this.favoriteUpdate = false;
        this.priceChanged = false;
        this.slotsChanged = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public void setRating(float rating) { this.rating = rating; }
    public void setReviewsCount(int reviewsCount) { this.reviewsCount = reviewsCount; }
    public String getWhatIncluded() { return whatIncluded; }
    public String getMeetingPoint() { return meetingPoint; }
    public String getGuideName() { return guideName; }
    public String getLanguage() { return language; }
    public String getCancellationPolicy() { return cancellationPolicy; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public boolean hasFavoriteUpdate() { return favoriteUpdate; }
    public void setFavoriteUpdate(boolean favoriteUpdate) { this.favoriteUpdate = favoriteUpdate; }
    public boolean isPriceChanged() { return priceChanged; }
    public void setPriceChanged(boolean priceChanged) { this.priceChanged = priceChanged; }
    public boolean isSlotsChanged() { return slotsChanged; }
    public void setSlotsChanged(boolean slotsChanged) { this.slotsChanged = slotsChanged; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
}
