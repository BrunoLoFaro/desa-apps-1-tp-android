package com.example.myapplication.data.model;

public class OtpVerificationRequest {
    public final String email;
    public final String code;

    public OtpVerificationRequest(String email, String code) {
        this.email = email;
        this.code = code;
    }
}
