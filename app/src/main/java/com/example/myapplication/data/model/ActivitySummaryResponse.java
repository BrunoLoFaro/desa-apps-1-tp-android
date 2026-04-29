package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class ActivitySummaryResponse {
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

    @Json(name = "durationMinutes")
    public int durationMinutes;

    @Json(name = "price")
    public double price;

    @Json(name = "currency")
    public String currency;

    @Json(name = "availableSpots")
    public int availableSpots;

    @Json(name = "avgRating")
    public Double avgRating;

    @Json(name = "reviewCount")
    public Long reviewCount;

    @Json(name = "isFavorite")
    public boolean isFavorite;

    @Json(name = "hasPriceChange")
    public boolean hasPriceChange;

    @Json(name = "hasAvailabilityChange")
    public boolean hasAvailabilityChange;

    @Json(name = "startDate")
    public String startDate;

    @Json(name = "startTime")
    public String startTime;
}
