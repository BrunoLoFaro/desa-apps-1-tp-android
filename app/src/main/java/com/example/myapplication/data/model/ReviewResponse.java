package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class ReviewResponse {
    @Json(name = "id")
    public Long id;

    @Json(name = "activityId")
    public Long activityId;

    @Json(name = "activityName")
    public String activityName;

    @Json(name = "destinationName")
    public String destinationName;

    @Json(name = "activityRating")
    public Integer activityRating;

    @Json(name = "guideRating")
    public Integer guideRating;

    @Json(name = "comment")
    public String comment;

    @Json(name = "createdAt")
    public String createdAt;
}
