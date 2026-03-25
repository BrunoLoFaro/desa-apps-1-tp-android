package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class OtpVerifyResponse {
    @Json(name = "token")
    public String token;

    @Json(name = "user_id")
    public String userId;

    @Json(name = "message")
    public String message;
}

