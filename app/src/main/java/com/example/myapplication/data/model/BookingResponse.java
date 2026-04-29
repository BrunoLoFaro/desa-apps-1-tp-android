package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class BookingResponse {
    @Json(name = "id")
    public Long id;

    @Json(name = "sessionId")
    public Long sessionId;

    @Json(name = "activityId")
    public Long activityId;

    @Json(name = "activityName")
    public String activityName;

    @Json(name = "destination")
    public DestinationResponse destination;

    @Json(name = "guideName")
    public String guideName;

    @Json(name = "sessionStartTime")
    public String sessionStartTime;

    @Json(name = "durationMinutes")
    public int durationMinutes;

    @Json(name = "participants")
    public int participants;

    @Json(name = "totalPrice")
    public double totalPrice;

    @Json(name = "currency")
    public String currency;

    @Json(name = "status")
    public String status;

    @Json(name = "cancellationPolicy")
    public String cancellationPolicy;

    @Json(name = "createdAt")
    public String createdAt;

    @Json(name = "cancelledAt")
    public String cancelledAt;

    @Json(name = "canReview")
    public boolean canReview;

    @Json(name = "voucherCode")
    public String voucherCode;

    @Json(name = "meetingPoint")
    public String meetingPoint;
}

