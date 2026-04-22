package com.example.myapplication.data.model;

public class RegisterRequest {
    public final String email;
    public final String password;
    public final String firstName;
    public final String lastName;
    public final String phone;

    public RegisterRequest(String email, String password, String firstName, String lastName, String phone) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }
}