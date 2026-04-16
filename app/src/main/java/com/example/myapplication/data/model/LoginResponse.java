package com.example.myapplication.data.model;

import com.squareup.moshi.Json;

public class LoginResponse {
    @Json(name = "userId")
    public Long userId;

    @Json(name = "email")
    public String email;

    @Json(name = "firstName")
    public String firstName;

    @Json(name = "lastName")
    public String lastName;

    @Json(name = "dni")
    public String dni;

    @Json(name = "token")
    public String token;

    @Json(name = "refreshToken")
    public String refreshToken;
}
