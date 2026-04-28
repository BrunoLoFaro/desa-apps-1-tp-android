package com.example.myapplication.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

public final class ConnectivityUtils {

    private ConnectivityUtils() {}

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
