package com.example.myapplication.data.model;

import java.util.List;

public class UserProfileData {
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final String profilePhotoUrl;
    private final String profilePhotoBase64;
    private final List<String> preferredCategories;
    private final long confirmedBookings;
    private final long completedBookings;
    private final long cancelledBookings;

    public UserProfileData(String email, String firstName, String lastName,
                           String phone, String profilePhotoUrl, String profilePhotoBase64,
                           List<String> preferredCategories,
                           long confirmedBookings, long completedBookings, long cancelledBookings) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.profilePhotoUrl = profilePhotoUrl;
        this.profilePhotoBase64 = profilePhotoBase64;
        this.preferredCategories = preferredCategories;
        this.confirmedBookings = confirmedBookings;
        this.completedBookings = completedBookings;
        this.cancelledBookings = cancelledBookings;
    }

    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public String getProfilePhotoBase64() { return profilePhotoBase64; }
    public List<String> getPreferredCategories() { return preferredCategories; }
    public long getConfirmedBookings() { return confirmedBookings; }
    public long getCompletedBookings() { return completedBookings; }
    public long getCancelledBookings() { return cancelledBookings; }
}
