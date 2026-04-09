package com.example.myapplication.data.model;

public class PasswordResetConfirmRequest {
    public final String email;
    public final String code;
    public final String newPassword;

    public PasswordResetConfirmRequest(String email, String code, String newPassword) {
        this.email = email;
        this.code = code;
        this.newPassword = newPassword;
    }
}