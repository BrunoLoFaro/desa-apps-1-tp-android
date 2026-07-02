package com.example.myapplication.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_activities")
public class CachedActivityEntity {
    @PrimaryKey
    public long id;
    public String name;
    public String destinationName;
    public String category;
    public String description;
    public String imageUrl;
    /** JSON string with itinerary points for offline maps (may be null). */
    public String itineraryJson;
    /** JSON string with gallery urls for offline carousel (may be null). */
    public String galleryJson;
    public int durationMinutes;
    public double basePrice;
    public String currency;
    public String guideName;
    public String meetingPoint;
    public String language;
    public String includesText;
    public String cancellationPolicy;
    public float avgRating;
    public int reviewCount;
    public int availableSpots;
}
