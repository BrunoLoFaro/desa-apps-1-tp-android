package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class ReviewResponse {
    @Json(name = "id")
    public Long id;

    @Json(name = "activityRating")
    public Integer activityRating;

    @Json(name = "guideRating")
    public Integer guideRating;

    @Json(name = "comment")
    public String comment;
}
