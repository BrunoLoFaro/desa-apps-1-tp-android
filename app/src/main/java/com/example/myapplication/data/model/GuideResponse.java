package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class GuideResponse {
    @Json(name = "id")
    public Long id;

    @Json(name = "fullName")
    public String fullName;
}

