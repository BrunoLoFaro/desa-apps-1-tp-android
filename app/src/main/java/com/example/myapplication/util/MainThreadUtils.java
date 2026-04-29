package com.example.myapplication.util;

import android.os.Handler;
import android.os.Looper;

public final class MainThreadUtils {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private MainThreadUtils() {}

    public static void post(Runnable action) {
        MAIN_HANDLER.post(action);
    }
}
