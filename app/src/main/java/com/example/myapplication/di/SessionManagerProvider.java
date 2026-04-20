package com.example.myapplication.di;

import android.content.Context;
import com.example.myapplication.data.session.SessionManager;

public class SessionManagerProvider {
    public static SessionManager provideSessionManager(Context context) {
        // Devuelve una instancia singleton de SessionManager
        return new SessionManager(context.getApplicationContext());
    }
}
