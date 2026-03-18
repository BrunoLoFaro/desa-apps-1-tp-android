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
            config.loginEndpoint = prefs.getString(KEY_LOGIN_ENDPOINT, "");
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
                .apply();
    }
}
