package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class CreateBookingRequest {
    @Json(name = "sessionId")
    public Long sessionId;

    @Json(name = "participants")
    public Integer participants;

    public CreateBookingRequest(Long sessionId, Integer participants) {
        this.sessionId = sessionId;
        this.participants = participants;
    }
}

