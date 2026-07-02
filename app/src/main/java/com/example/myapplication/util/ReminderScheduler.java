package com.example.myapplication.util;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.work.ActivityReminderWorker;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Programa con WorkManager el recordatorio que se dispara 24 h antes de cada
 * actividad confirmada (Feature 12, requisito 29).
 */
public final class ReminderScheduler {

    private static final long REMINDER_LEAD_MILLIS = TimeUnit.HOURS.toMillis(24);
    private static final String WORK_PREFIX = "reminder_booking_";

    private ReminderScheduler() {}

    /** Programa (o reprograma) los recordatorios de una lista de reservas confirmadas. */
    public static void scheduleAll(Context context, List<BookingResponse> bookings) {
        if (bookings == null) return;
        for (BookingResponse b : bookings) {
            if (b != null && b.id != null && "CONFIRMED".equalsIgnoreCase(b.status)) {
                schedule(context, b.id, b.activityName, b.sessionStartTime);
            }
        }
    }

    /**
     * Programa el recordatorio de una reserva. Si la actividad comienza en menos
     * de 24 h (o ya pasó) no se agenda nada: la ventana del recordatorio ya venció.
     */
    public static void schedule(Context context, long bookingId, String activityName, String sessionStartTime) {
        long startMillis = FormatUtils.parseToEpochMillis(sessionStartTime);
        if (startMillis <= 0) return;

        long delay = startMillis - REMINDER_LEAD_MILLIS - System.currentTimeMillis();
        if (delay <= 0) return;

        Data input = new Data.Builder()
                .putLong(ActivityReminderWorker.KEY_BOOKING_ID, bookingId)
                .putString(ActivityReminderWorker.KEY_ACTIVITY_NAME, activityName)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ActivityReminderWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(input)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_PREFIX + bookingId, ExistingWorkPolicy.REPLACE, request);
    }

    /** Cancela el recordatorio de una reserva (p. ej. si se cancela la actividad). */
    public static void cancel(Context context, long bookingId) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_PREFIX + bookingId);
    }
}
