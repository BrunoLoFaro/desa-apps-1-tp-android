package com.example.myapplication.data.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import okio.BufferedSource;
import okio.Okio;

@Singleton
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

    @Inject
    public ConfigLoader(@ApplicationContext Context context, Moshi moshi) {
        this.context = context.getApplicationContext();
        this.moshi = moshi;
    }

    public AppConfig loadConfig() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        AppConfig defaultConfig = loadDefaultConfig();
        if (prefs.contains(KEY_BASE_URL)) {
            AppConfig config = new AppConfig();
            config.baseUrl = prefs.getString(KEY_BASE_URL, defaultConfig != null ? defaultConfig.baseUrl : "");
            config.loginEndpoint = prefs.getString(KEY_LOGIN_ENDPOINT, defaultConfig != null ? defaultConfig.loginEndpoint : null);
            config.registerEndpoint = prefs.getString(KEY_REGISTER_ENDPOINT, defaultConfig != null ? defaultConfig.registerEndpoint : null);
            config.signupOtpRequestEndpoint = prefs.getString(KEY_SIGNUP_OTP_REQUEST_ENDPOINT, defaultConfig != null ? defaultConfig.signupOtpRequestEndpoint : null);
            config.signupOtpResendEndpoint = prefs.getString(KEY_SIGNUP_OTP_RESEND_ENDPOINT, defaultConfig != null ? defaultConfig.signupOtpResendEndpoint : null);
            config.signupOtpVerifyEndpoint = prefs.getString(KEY_SIGNUP_OTP_VERIFY_ENDPOINT, defaultConfig != null ? defaultConfig.signupOtpVerifyEndpoint : null);
            config.signupOtpCompleteEndpoint = prefs.getString(KEY_SIGNUP_OTP_COMPLETE_ENDPOINT, defaultConfig != null ? defaultConfig.signupOtpCompleteEndpoint : null);
            config.passwordResetRequestEndpoint = prefs.getString(KEY_PASSWORD_RESET_REQUEST_ENDPOINT, defaultConfig != null ? defaultConfig.passwordResetRequestEndpoint : null);
            config.passwordResetResendEndpoint = prefs.getString(KEY_PASSWORD_RESET_RESEND_ENDPOINT, defaultConfig != null ? defaultConfig.passwordResetResendEndpoint : null);
            config.passwordResetVerifyEndpoint = prefs.getString(KEY_PASSWORD_RESET_VERIFY_ENDPOINT, defaultConfig != null ? defaultConfig.passwordResetVerifyEndpoint : null);
            config.passwordResetConfirmEndpoint = prefs.getString(KEY_PASSWORD_RESET_CONFIRM_ENDPOINT, defaultConfig != null ? defaultConfig.passwordResetConfirmEndpoint : null);
            return config;
        }

        return defaultConfig;
    }

    private AppConfig loadDefaultConfig() {
        try {
            InputStream is = context.getAssets().open(CONFIG_FILE);
            BufferedSource source = Okio.buffer(Okio.source(is));
            JsonAdapter<AppConfig> adapter = moshi.adapter(AppConfig.class);
            AppConfig config = adapter.fromJson(source);
            return config != null ? config : new AppConfig();
        } catch (IOException e) {
            Log.e("ConfigLoader", "Error al leer config.json", e);
            return new AppConfig();
        }
    }

}
