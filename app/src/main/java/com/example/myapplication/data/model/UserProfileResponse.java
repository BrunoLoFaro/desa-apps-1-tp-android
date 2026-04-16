package com.example.myapplication.data.model;

import com.squareup.moshi.Json;
import java.util.List;

public class UserProfileResponse {
    @Json(name = "id")
    public Long id;

    @Json(name = "email")
    public String email;

    @Json(name = "firstName")
    public String firstName;

    @Json(name = "lastName")
    public String lastName;

    @Json(name = "phone")
    public String phone;

    @Json(name = "profilePhotoUrl")
    public String profilePhotoUrl;

    @Json(name = "profilePhotoBase64")
    public String profilePhotoBase64;

    @Json(name = "preferredCategories")
    public List<String> preferredCategories;

    @Json(name = "confirmedBookings")
    public long confirmedBookings;

    @Json(name = "completedBookings")
    public long completedBookings;

    @Json(name = "cancelledBookings")
    public long cancelledBookings;
}
