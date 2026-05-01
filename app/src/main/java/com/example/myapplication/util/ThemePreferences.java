package com.example.myapplication.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public final class ThemePreferences {

    private static final String PREFS_NAME = "ui_prefs";
    private static final String KEY_NIGHT_MODE = "night_mode";

    private ThemePreferences() {}

    public static void applySavedNightMode(Context context) {
        AppCompatDelegate.setDefaultNightMode(getSavedNightMode(context));
    }

    public static int getSavedNightMode(Context context) {
        if (context == null) return AppCompatDelegate.MODE_NIGHT_NO;
        return prefs(context).getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_NO);
    }

    public static void setNightMode(Context context, int mode) {
        if (context == null) return;
        prefs(context).edit().putInt(KEY_NIGHT_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}

