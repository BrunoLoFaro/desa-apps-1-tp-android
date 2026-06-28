package com.example.myapplication.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;

/**
 * Centraliza la creación de canales y el armado de notificaciones para los
 * Recordatorios y Avisos (Feature 12).
 *
 * <p>Sigue el patrón de la implementación de referencia de
 * mobile-practices-android (NotificationChannel + NotificationCompat), agregando
 * un {@link PendingIntent} que lleva directo al voucher de la reserva.</p>
 */
public final class NotificationHelper {

    public static final String CHANNEL_REMINDERS = "reminders_channel";
    public static final String CHANNEL_NOVEDADES = "novedades_channel";

    /** Extra que viaja en el intent para abrir el voucher de una reserva. */
    public static final String EXTRA_OPEN_VOUCHER_BOOKING_ID = "open_voucher_booking_id";

    private NotificationHelper() {}

    /** Crea los canales de notificación (no-op en API < 26 y si ya existen). */
    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel reminders = new NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.notif_channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH);
        reminders.setDescription(context.getString(R.string.notif_channel_reminders_desc));
        manager.createNotificationChannel(reminders);

        NotificationChannel novedades = new NotificationChannel(
                CHANNEL_NOVEDADES,
                context.getString(R.string.notif_channel_novedades_name),
                NotificationManager.IMPORTANCE_HIGH);
        novedades.setDescription(context.getString(R.string.notif_channel_novedades_desc));
        manager.createNotificationChannel(novedades);
    }

    /**
     * Muestra una notificación cuyo tap abre el voucher de la reserva indicada.
     * El id de la notificación se deriva del bookingId para que el aviso de una
     * misma reserva se reemplace en lugar de duplicarse.
     */
    public static void showBookingNotification(Context context, String channelId,
                                               long bookingId, String title, String text) {
        createChannels(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_OPEN_VOUCHER_BOOKING_ID, bookingId);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pending = PendingIntent.getActivity(
                context, (int) bookingId, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_ticket)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pending)
                .setAutoCancel(true);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(channelId.hashCode() + (int) bookingId, builder.build());
        }
    }
}
