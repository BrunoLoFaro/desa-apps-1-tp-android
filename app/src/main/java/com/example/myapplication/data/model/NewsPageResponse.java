package com.example.myapplication.data.model;

import com.squareup.moshi.Json;
import java.util.List;

public class NewsPageResponse {
    @Json(name = "items")
    public List<NewsItem> items;

    @Json(name = "page")
    public int page;

    @Json(name = "size")
    public int size;

    @Json(name = "totalElements")
    public long totalElements;

    @Json(name = "totalPages")
    public int totalPages;
}
