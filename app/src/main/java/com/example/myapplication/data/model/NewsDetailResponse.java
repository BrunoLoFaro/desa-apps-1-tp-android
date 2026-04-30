package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class NewsDetailResponse {
    @Json(name = "id")
    public Long id;

    @Json(name = "title")
    public String title;

    @Json(name = "description")
    public String description;

    @Json(name = "fullContent")
    public String fullContent;

    @Json(name = "imageUrl")
    public String imageUrl;

    @Json(name = "type")
    public String type;

    @Json(name = "relatedActivityId")
    public Long relatedActivityId;

    @Json(name = "relatedActivityName")
    public String relatedActivityName;

    @Json(name = "publishedAt")
    public String publishedAt;

    @Json(name = "validUntil")
    public String validUntil;

    @Json(name = "ctaText")
    public String ctaText;

    @Json(name = "ctaLink")
    public String ctaLink;
}
