package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class CreateReviewRequest {
    @Json(name = "bookingId")
    public Long bookingId;

    @Json(name = "activityRating")
    public Integer activityRating;

    @Json(name = "guideRating")
    public Integer guideRating;

    @Json(name = "comment")
    public String comment;

    public CreateReviewRequest(Long bookingId, Integer activityRating, Integer guideRating, String comment) {
        this.bookingId = bookingId;
        this.activityRating = activityRating;
        this.guideRating = guideRating;
        this.comment = comment;
    }
}

