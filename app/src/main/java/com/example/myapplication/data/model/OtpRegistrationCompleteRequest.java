package com.example.myapplication.data.model;

public class OtpRegistrationCompleteRequest {
    public final String email;
    public final String code;
    public final String password;
    public final String firstName;
    public final String lastName;

    public OtpRegistrationCompleteRequest(
            String email,
            String code,
            String password,
            String firstName,
            String lastName
    ) {
        this.email = email;
        this.code = code;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}