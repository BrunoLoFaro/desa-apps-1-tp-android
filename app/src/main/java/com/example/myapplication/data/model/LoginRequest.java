package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class LoginRequest {
    @Json(name = "email")
    public String email;

    @Json(name = "password")
    public String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
