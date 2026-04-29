package com.example.myapplication;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.work.Configuration;
import androidx.hilt.work.HiltWorkerFactory;
import dagger.hilt.android.HiltAndroidApp;
import javax.inject.Inject;

@HiltAndroidApp
public class XploreNowApplication extends Application implements Configuration.Provider {

    @Inject
    HiltWorkerFactory workerFactory;

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }
}
