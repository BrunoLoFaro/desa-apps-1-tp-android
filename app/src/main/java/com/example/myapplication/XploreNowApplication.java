package com.example.myapplication;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.work.Configuration;
import androidx.hilt.work.HiltWorkerFactory;
import com.example.myapplication.util.ThemePreferences;
import dagger.hilt.android.HiltAndroidApp;
import javax.inject.Inject;

@HiltAndroidApp
public class XploreNowApplication extends Application implements Configuration.Provider {

    @Inject
    HiltWorkerFactory workerFactory;

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferences.applySavedNightMode(this);
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }
}
