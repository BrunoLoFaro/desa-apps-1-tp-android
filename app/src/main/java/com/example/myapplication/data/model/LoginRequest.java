package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class LoginRequest {
    @Json(name = "username")
    public String username;

    @Json(name = "password")
    public String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
