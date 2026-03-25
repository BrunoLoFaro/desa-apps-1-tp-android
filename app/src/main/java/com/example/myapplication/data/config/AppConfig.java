package com.example.myapplication.data.config;

import com.squareup.moshi.Json;

public class AppConfig {
    @Json(name = "base_url")
    public String baseUrl;

    @Json(name = "login_endpoint")
    public String loginEndpoint;

    @Json(name = "otp_request_endpoint")
    public String otpRequestEndpoint;

    @Json(name = "otp_verify_endpoint")
    public String otpVerifyEndpoint;

    @Json(name = "otp_ttl_seconds")
    public int otpTtlSeconds = 120;
}
