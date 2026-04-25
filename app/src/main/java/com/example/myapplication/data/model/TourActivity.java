package com.example.myapplication.data.model;

import java.io.Serializable;
import java.util.List;

public class TourActivity implements Serializable {
    private Long id;
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
    private List<ItineraryPoint> itineraryPoints;
    private boolean isFavorite;
    private boolean favoriteUpdate;
    private boolean priceChanged;
    private boolean slotsChanged;

    public TourActivity(String name, String destination, String category, String duration,
                        String price, int availableSlots, String imageUrl) {
        this(name, destination, category, duration, price, availableSlots, imageUrl,
             null, 0f, 0, null, null, null, null, null, null, false);
    }

    public TourActivity(String name, String destination, String category, String duration, String price, 
                        int availableSlots, String imageUrl, String description, float rating, 
                        int reviewsCount, String whatIncluded, String meetingPoint, 
                        String guideName, String language, String cancellationPolicy, 
                        List<String> galleryUrls) {
        this(name, destination, category, duration, price, availableSlots, imageUrl,
                description, rating, reviewsCount, whatIncluded, meetingPoint,
                guideName, language, cancellationPolicy, galleryUrls, false);
    }

    public TourActivity(String name, String destination, String category, String duration, String price,
                        int availableSlots, String imageUrl, String description, float rating,
                        int reviewsCount, String whatIncluded, String meetingPoint,
                        String guideName, String language, String cancellationPolicy,
                        List<String> galleryUrls, boolean isFavorite) {
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
        this.itineraryPoints = null;
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
    public List<String> getGalleryUrls() { return galleryUrls; }
    public List<ItineraryPoint> getItineraryPoints() { return itineraryPoints; }
    public void setItineraryPoints(List<ItineraryPoint> itineraryPoints) { this.itineraryPoints = itineraryPoints; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public boolean hasFavoriteUpdate() { return favoriteUpdate; }
    public void setFavoriteUpdate(boolean favoriteUpdate) { this.favoriteUpdate = favoriteUpdate; }
    public boolean isPriceChanged() { return priceChanged; }
    public void setPriceChanged(boolean priceChanged) { this.priceChanged = priceChanged; }
    public boolean isSlotsChanged() { return slotsChanged; }
    public void setSlotsChanged(boolean slotsChanged) { this.slotsChanged = slotsChanged; }
}
