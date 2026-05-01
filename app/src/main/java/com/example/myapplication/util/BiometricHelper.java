package com.example.myapplication.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.biometric.BiometricManager;

public final class BiometricHelper {

    private static final String PREFS_NAME = "biometric_prefs";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_BIOMETRIC_SKIPPED = "biometric_skipped";

    private BiometricHelper() {}

    public static boolean shouldShowEnrollment(Context context) {
        if (context == null) return false;

        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

        BiometricManager biometricManager = BiometricManager.from(context);
        if (biometricManager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            return false;
        }

        SharedPreferences prefs = prefs(context);
        boolean enabled = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
        boolean skipped = prefs.getBoolean(KEY_BIOMETRIC_SKIPPED, false);
        return !enabled && !skipped;
    }

    public static void setBiometricEnabled(Context context, boolean enabled) {
        if (context == null) return;
        prefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public static void setBiometricSkipped(Context context, boolean skipped) {
        if (context == null) return;
        prefs(context).edit().putBoolean(KEY_BIOMETRIC_SKIPPED, skipped).apply();
    }

    public static boolean isBiometricEnabled(Context context) {
        if (context == null) return false;
        return prefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public static boolean isBiometricSkipped(Context context) {
        if (context == null) return false;
        return prefs(context).getBoolean(KEY_BIOMETRIC_SKIPPED, false);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}

