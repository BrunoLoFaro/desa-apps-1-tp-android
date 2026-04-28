package com.example.myapplication.data.model;

import com.squareup.moshi.Json;
import java.util.List;

public class ActivityDetailResponse {
    @Json(name = "id")
    public Long id;

    @Json(name = "name")
    public String name;

    @Json(name = "imageUrl")
    public String imageUrl;

    @Json(name = "destination")
    public DestinationResponse destination;

    @Json(name = "category")
    public String category;

    @Json(name = "description")
    public String description;

    @Json(name = "includesText")
    public String includesText;

    @Json(name = "meetingPoint")
    public String meetingPoint;

    @Json(name = "guide")
    public GuideResponse guide;

    @Json(name = "durationMinutes")
    public int durationMinutes;

    @Json(name = "language")
    public String language;

    @Json(name = "cancellationPolicy")
    public String cancellationPolicy;

    @Json(name = "basePrice")
    public double basePrice;

    @Json(name = "currency")
    public String currency;

    @Json(name = "sessions")
    public List<ActivitySessionResponse> sessions;

    @Json(name = "availableSpots")
    public int availableSpots;

    @Json(name = "avgRating")
    public Double avgRating;

    @Json(name = "reviewCount")
    public Long reviewCount;

    @Json(name = "isFavorite")
    public boolean isFavorite;

    @Json(name = "discountPercentage")
    public Integer discountPercentage;
}
