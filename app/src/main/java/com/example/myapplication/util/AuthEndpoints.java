package com.example.myapplication.util;

import com.example.myapplication.data.config.AppConfig;

public final class AuthEndpoints {

    public static final String LOGIN = "api/v1/auth/login";
    public static final String REGISTER = "api/v1/auth/register";
    public static final String SIGNUP_OTP_REQUEST = "api/v1/auth/signup/otp/request";
    public static final String SIGNUP_OTP_RESEND = "api/v1/auth/signup/otp/resend";
    public static final String SIGNUP_OTP_COMPLETE = "api/v1/auth/signup/otp/complete";
    public static final String PASSWORD_RESET_REQUEST = "api/v1/auth/password-reset/request";
    public static final String PASSWORD_RESET_RESEND = "api/v1/auth/password-reset/resend";
    public static final String PASSWORD_RESET_VERIFY = "api/v1/auth/password-reset/verify";
    public static final String PASSWORD_RESET_CONFIRM = "api/v1/auth/password-reset/confirm";

    private AuthEndpoints() {
    }

    public static String login(AppConfig config) {
        return valueOrDefault(config != null ? config.loginEndpoint : null, LOGIN);
    }

    public static String register(AppConfig config) {
        return valueOrDefault(config != null ? config.registerEndpoint : null, REGISTER);
    }

    public static String signupOtpRequest(AppConfig config) {
        return valueOrDefault(config != null ? config.signupOtpRequestEndpoint : null, SIGNUP_OTP_REQUEST);
    }

    public static String signupOtpResend(AppConfig config) {
        return valueOrDefault(config != null ? config.signupOtpResendEndpoint : null, SIGNUP_OTP_RESEND);
    }

    public static String signupOtpComplete(AppConfig config) {
        return valueOrDefault(config != null ? config.signupOtpCompleteEndpoint : null, SIGNUP_OTP_COMPLETE);
    }

    public static String passwordResetRequest(AppConfig config) {
        return valueOrDefault(config != null ? config.passwordResetRequestEndpoint : null, PASSWORD_RESET_REQUEST);
    }

    public static String passwordResetResend(AppConfig config) {
        return valueOrDefault(config != null ? config.passwordResetResendEndpoint : null, PASSWORD_RESET_RESEND);
    }

    public static String passwordResetVerify(AppConfig config) {
        return valueOrDefault(config != null ? config.passwordResetVerifyEndpoint : null, PASSWORD_RESET_VERIFY);
    }

    public static String passwordResetConfirm(AppConfig config) {
        return valueOrDefault(config != null ? config.passwordResetConfirmEndpoint : null, PASSWORD_RESET_CONFIRM);
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }
}