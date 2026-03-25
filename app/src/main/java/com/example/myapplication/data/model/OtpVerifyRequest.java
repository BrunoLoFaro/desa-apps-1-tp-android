package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class OtpVerifyRequest {
    @Json(name = "email")
    public String email;

    @Json(name = "otp")
    public String otp;

    public OtpVerifyRequest(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }
}

