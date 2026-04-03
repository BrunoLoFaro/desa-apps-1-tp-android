package com.example.myapplication.data.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREFS_NAME = "auth_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveAccessToken(String token) {
        preferences.edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }

    public String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public boolean hasValidSession() {
        String token = getAccessToken();
        return token != null && !token.trim().isEmpty();
    }

    public void clearSession() {
        preferences.edit().remove(KEY_ACCESS_TOKEN).apply();
    }
}
