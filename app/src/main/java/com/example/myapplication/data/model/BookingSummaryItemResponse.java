package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class BookingSummaryItemResponse {
    @Json(name = "id")
    public Long id;

    @Json(name = "activityName")
    public String activityName;

    @Json(name = "status")
    public String status;

    @Json(name = "sessionStartTime")
    public String sessionStartTime;

    @Json(name = "totalPrice")
    public double totalPrice;

    @Json(name = "currency")
    public String currency;
}
