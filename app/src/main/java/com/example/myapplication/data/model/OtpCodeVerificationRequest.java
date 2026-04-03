package com.example.myapplication.data.model;

public class OtpCodeVerificationRequest {
    public final String email;
    public final String code;

    public OtpCodeVerificationRequest(String email, String code) {
        this.email = email;
        this.code = code;
    }
}
