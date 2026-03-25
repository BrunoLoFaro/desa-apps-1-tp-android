package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class OtpRequest {
    @Json(name = "email")
    public String email;

    public OtpRequest(String email) {
        this.email = email;
    }
}

