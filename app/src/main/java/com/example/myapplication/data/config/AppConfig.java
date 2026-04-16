package com.example.myapplication.data.config;

import com.squareup.moshi.Json;

public class AppConfig {
    @Json(name = "base_url")
    public String baseUrl;

    @Json(name = "login_endpoint")
    public String loginEndpoint;

    @Json(name = "refresh_endpoint")
    public String refreshEndpoint;

    public boolean hasValidBaseUrl() {
        return baseUrl != null && !baseUrl.trim().isEmpty();
    }

    @Json(name = "register_endpoint")
    public String registerEndpoint;

    @Json(name = "signup_otp_request_endpoint")
    public String signupOtpRequestEndpoint;

    @Json(name = "signup_otp_resend_endpoint")
    public String signupOtpResendEndpoint;

    @Json(name = "signup_otp_verify_endpoint")
    public String signupOtpVerifyEndpoint;

    @Json(name = "signup_otp_complete_endpoint")
    public String signupOtpCompleteEndpoint;

    @Json(name = "password_reset_request_endpoint")
    public String passwordResetRequestEndpoint;

    @Json(name = "password_reset_resend_endpoint")
    public String passwordResetResendEndpoint;

    @Json(name = "password_reset_verify_endpoint")
    public String passwordResetVerifyEndpoint;

    @Json(name = "password_reset_confirm_endpoint")
    public String passwordResetConfirmEndpoint;

    @Json(name = "activities_endpoint")
    public String activitiesEndpoint;

    @Json(name = "activities_featured_endpoint")
    public String activitiesFeaturedEndpoint;

    @Json(name = "activities_recommended_endpoint")
    public String activitiesRecommendedEndpoint;

    @Json(name = "profile_endpoint")
    public String profileEndpoint;

    @Json(name = "preferences_endpoint")
    public String preferencesEndpoint;

    @Json(name = "activity_summary_endpoint")
    public String activitySummaryEndpoint;
}
