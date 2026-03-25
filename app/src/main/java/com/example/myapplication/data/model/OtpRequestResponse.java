package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class OtpRequestResponse {
    @Json(name = "message")
    public String message;

    @Json(name = "otp_id")
    public String otpId;

    @Json(name = "expires_in")
    public Integer expiresIn;
}

