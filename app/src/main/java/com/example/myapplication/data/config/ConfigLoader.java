package com.example.myapplication.data.config;

import android.content.Context;
import android.content.SharedPreferences;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.io.IOException;
import java.io.InputStream;
import okio.BufferedSource;
import okio.Okio;

public class ConfigLoader {
    private static final String CONFIG_FILE = "config.json";
    private static final String PREFS_NAME = "app_config";

    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_LOGIN_ENDPOINT = "login_endpoint";
    private static final String KEY_REGISTER_ENDPOINT = "register_endpoint";
    private static final String KEY_SIGNUP_OTP_REQUEST_ENDPOINT = "signup_otp_request_endpoint";
    private static final String KEY_SIGNUP_OTP_RESEND_ENDPOINT = "signup_otp_resend_endpoint";
    private static final String KEY_SIGNUP_OTP_VERIFY_ENDPOINT = "signup_otp_verify_endpoint";
    private static final String KEY_SIGNUP_OTP_COMPLETE_ENDPOINT = "signup_otp_complete_endpoint";
    private static final String KEY_PASSWORD_RESET_REQUEST_ENDPOINT = "password_reset_request_endpoint";
    private static final String KEY_PASSWORD_RESET_RESEND_ENDPOINT = "password_reset_resend_endpoint";
    private static final String KEY_PASSWORD_RESET_VERIFY_ENDPOINT = "password_reset_verify_endpoint";
    private static final String KEY_PASSWORD_RESET_CONFIRM_ENDPOINT = "password_reset_confirm_endpoint";

    private final Context context;
    private final Moshi moshi;

    public ConfigLoader(Context context) {
        this.context = context.getApplicationContext();
        this.moshi = new Moshi.Builder().build();
    }

    public AppConfig loadConfig() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.contains(KEY_BASE_URL)) {
            AppConfig config = new AppConfig();
            config.baseUrl = prefs.getString(KEY_BASE_URL, "");
            config.loginEndpoint = prefs.getString(KEY_LOGIN_ENDPOINT, "api/v1/auth/login");
            config.registerEndpoint = prefs.getString(KEY_REGISTER_ENDPOINT, "api/v1/auth/register");
            config.signupOtpRequestEndpoint = prefs.getString(KEY_SIGNUP_OTP_REQUEST_ENDPOINT, "api/v1/auth/signup/otp/request");
            config.signupOtpResendEndpoint = prefs.getString(KEY_SIGNUP_OTP_RESEND_ENDPOINT, "api/v1/auth/signup/otp/resend");
            config.signupOtpVerifyEndpoint = prefs.getString(KEY_SIGNUP_OTP_VERIFY_ENDPOINT, "api/v1/auth/signup/otp/verify");
            config.signupOtpCompleteEndpoint = prefs.getString(KEY_SIGNUP_OTP_COMPLETE_ENDPOINT, "api/v1/auth/signup/otp/complete");
            config.passwordResetRequestEndpoint = prefs.getString(KEY_PASSWORD_RESET_REQUEST_ENDPOINT, "api/v1/auth/password-reset/request");
            config.passwordResetResendEndpoint = prefs.getString(KEY_PASSWORD_RESET_RESEND_ENDPOINT, "api/v1/auth/password-reset/resend");
            config.passwordResetVerifyEndpoint = prefs.getString(KEY_PASSWORD_RESET_VERIFY_ENDPOINT, "api/v1/auth/password-reset/verify");
            config.passwordResetConfirmEndpoint = prefs.getString(KEY_PASSWORD_RESET_CONFIRM_ENDPOINT, "api/v1/auth/password-reset/confirm");

            // migración: corregir endpoint viejo si alguien tenía guardado el valor incorrecto
            if ("users/login".equals(config.loginEndpoint)) {
                config.loginEndpoint = "api/v1/auth/login";
            }
            return config;
        }

        try {
            InputStream is = context.getAssets().open(CONFIG_FILE);
            BufferedSource source = Okio.buffer(Okio.source(is));
            JsonAdapter<AppConfig> adapter = moshi.adapter(AppConfig.class);
            return adapter.fromJson(source);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveConfig(AppConfig config) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_BASE_URL, config.baseUrl)
                .putString(KEY_LOGIN_ENDPOINT, config.loginEndpoint)
                .putString(KEY_REGISTER_ENDPOINT, config.registerEndpoint)
                .putString(KEY_SIGNUP_OTP_REQUEST_ENDPOINT, config.signupOtpRequestEndpoint)
                .putString(KEY_SIGNUP_OTP_RESEND_ENDPOINT, config.signupOtpResendEndpoint)
                .putString(KEY_SIGNUP_OTP_VERIFY_ENDPOINT, config.signupOtpVerifyEndpoint)
                .putString(KEY_SIGNUP_OTP_COMPLETE_ENDPOINT, config.signupOtpCompleteEndpoint)
                .putString(KEY_PASSWORD_RESET_REQUEST_ENDPOINT, config.passwordResetRequestEndpoint)
                .putString(KEY_PASSWORD_RESET_RESEND_ENDPOINT, config.passwordResetResendEndpoint)
                .putString(KEY_PASSWORD_RESET_VERIFY_ENDPOINT, config.passwordResetVerifyEndpoint)
                .putString(KEY_PASSWORD_RESET_CONFIRM_ENDPOINT, config.passwordResetConfirmEndpoint)
                .apply();
    }
}
