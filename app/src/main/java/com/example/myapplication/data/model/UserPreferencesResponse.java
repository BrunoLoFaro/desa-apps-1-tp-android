package com.example.myapplication.data.model;

import com.squareup.moshi.Json;
import java.util.List;

public class UserPreferencesResponse {
    @Json(name = "preferredCategories")
    public List<String> preferredCategories;

    @Json(name = "preferredDestinations")
    public List<DestinationResponse> preferredDestinations;
}
