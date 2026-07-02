package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class ItineraryPointResponse {
    @Json(name = "name")
    public String name;

    @Json(name = "address")
    public String address;

    @Json(name = "position")
    public Integer position;
}

