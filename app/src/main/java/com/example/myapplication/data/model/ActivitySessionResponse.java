package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class ActivitySessionResponse {
    @Json(name = "id")
    public Long id;

    @Json(name = "startTime")
    public String startTime;

    @Json(name = "capacity")
    public int capacity;

    @Json(name = "bookedCount")
    public int bookedCount;

    @Json(name = "availableSpots")
    public int availableSpots;

    @Json(name = "price")
    public double price;
}

