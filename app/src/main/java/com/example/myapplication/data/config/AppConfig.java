package com.example.myapplication.data.config;

import com.squareup.moshi.Json;

public class AppConfig {
    @Json(name = "base_url")
    public String baseUrl;

    @Json(name = "login_endpoint")
    public String loginEndpoint;
}
