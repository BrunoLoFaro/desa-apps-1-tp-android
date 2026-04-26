package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class ApiErrorResponse {
    @Json(name = "message")
    public String message;

    @Json(name = "error")
    public String error;
}
