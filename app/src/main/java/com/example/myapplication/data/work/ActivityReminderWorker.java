package com.example.myapplication.data.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapplication.R;
import com.example.myapplication.util.NotificationHelper;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

/**
 * Dispara el recordatorio 24 h antes de una actividad (Feature 12, requisito 29).
 * Se programa con {@link com.example.myapplication.util.ReminderScheduler} y el
 * tap de la notificación abre directamente el voucher de la reserva.
 */
@HiltWorker
public class ActivityReminderWorker extends Worker {

    public static final String KEY_BOOKING_ID = "booking_id";
    public static final String KEY_ACTIVITY_NAME = "activity_name";

    @AssistedInject
    public ActivityReminderWorker(@Assisted @NonNull Context context,
                                  @Assisted @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long bookingId = getInputData().getLong(KEY_BOOKING_ID, -1L);
        if (bookingId <= 0) return Result.success();

        String activityName = getInputData().getString(KEY_ACTIVITY_NAME);
        Context ctx = getApplicationContext();

        NotificationHelper.showBookingNotification(
                ctx,
                NotificationHelper.CHANNEL_REMINDERS,
                bookingId,
                ctx.getString(R.string.notif_reminder_title,
                        activityName != null ? activityName : ""),
                ctx.getString(R.string.notif_reminder_text));

        return Result.success();
    }
}
