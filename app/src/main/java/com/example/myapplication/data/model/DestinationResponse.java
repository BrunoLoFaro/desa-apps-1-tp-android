package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class DestinationResponse {
    @Json(name = "id")
    public Long id;

    @Json(name = "name")
    public String name;
}
