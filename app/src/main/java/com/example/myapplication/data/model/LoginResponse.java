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

    // setters
    public void setUserId(Long userId) { this.userId = userId; }
    public void setEmail(String email) { this.email = email; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setDni(String dni) { this.dni = dni; }
    public void setToken(String token) { this.token = token; }
}
