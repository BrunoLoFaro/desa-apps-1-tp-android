package com.example.myapplication.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.myapplication.data.local.OfflineBookingEntity;

public final class CheckInStore {

    private static final String PREFS_NAME = "check_in_store";
    private static final String KEY_PREFIX = "booking_";

    private CheckInStore() {}

    public static void markConfirmed(Context context, long bookingId) {
        getPrefs(context).edit()
                .putBoolean(KEY_PREFIX + bookingId, true)
                .apply();
    }

    public static boolean isConfirmed(Context context, OfflineBookingEntity booking) {
        return booking != null && isConfirmed(context, booking.id);
    }

    public static boolean isConfirmed(Context context, long bookingId) {
        return getPrefs(context).getBoolean(KEY_PREFIX + bookingId, false);
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
