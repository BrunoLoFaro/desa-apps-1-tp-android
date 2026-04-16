package com.example.myapplication.glide;

import android.content.Context;
import com.example.myapplication.data.session.SessionManager;
import com.example.myapplication.di.AppModule;
import dagger.hilt.android.qualifiers.ApplicationContext;
import okhttp3.OkHttpClient;

public class GlideOkHttpProvider {
    private static OkHttpClient client;

    public static synchronized OkHttpClient getClient(Context context) {
        if (client == null) {
            // Usa el mismo SessionManager y configuración que AppModule
            SessionManager sessionManager = com.example.myapplication.di.SessionManagerProvider.provideSessionManager(context);
            client = AppModule.provideOkHttpClient(sessionManager, null);
        }
        return client;
    }
}
