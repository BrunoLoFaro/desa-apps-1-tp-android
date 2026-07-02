package com.example.myapplication.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;
import com.example.myapplication.data.local.OfflineBookingDao;
import com.example.myapplication.data.local.OfflineBookingEntity;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.model.BookingsPageResponse;
import com.example.myapplication.data.network.BookingService;
import com.example.myapplication.data.repository.SessionRepository;
import com.example.myapplication.util.NotificationHelper;
import com.example.myapplication.util.ReminderScheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Response;

/**
 * Foreground service que escucha novedades de las reservas del usuario y avisa
 * de inmediato cuando la operadora reprograma o cancela una actividad
 * (Feature 12, requisito 30).
 *
 * <p>Replica el patrón del {@code PollingService} de referencia de
 * mobile-practices-android (foreground service + NotificationChannel + hilo de
 * polling), adaptado al backend de XploreNow: consulta periódicamente las
 * reservas y compara contra el último estado conocido para detectar cambios de
 * horario (reprogramación) o de estado a {@code CANCELLED}.</p>
 */
@AndroidEntryPoint
public class NovedadesPollingService extends Service {

    private static final String TAG = "NovedadesPolling";
    private static final int NOTIF_ID_FOREGROUND = 4101;
    private static final long POLL_INTERVAL_MILLIS = 60_000L; // 60 s

    @Inject BookingService bookingService;
    @Inject OfflineBookingDao offlineBookingDao;
    @Inject SessionRepository sessionRepository;

    private Thread pollingThread;
    private volatile boolean running = false;

    // Último estado conocido por reserva (id → horario / estado).
    private final Map<Long, String> lastStartTime = new HashMap<>();
    private final Map<Long, String> lastStatus = new HashMap<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationHelper.createChannels(this);

        Notification foreground = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_NOVEDADES)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notif_service_listening))
                .setSmallIcon(R.drawable.ic_ticket)
                .setOngoing(true)
                .build();
        startForeground(NOTIF_ID_FOREGROUND, foreground);

        if (!running) {
            running = true;
            pollingThread = new Thread(this::loopDePolling, "novedades-polling");
            pollingThread.start();
        }
        return START_STICKY;
    }

    private void loopDePolling() {
        long userId = sessionRepository.getUserId();
        if (userId <= 0) {
            Log.d(TAG, "Sin sesión activa; no se inicia el polling");
            return;
        }

        seedFromCache(userId);

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                consultarNovedades(userId);
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.w(TAG, "Error consultando novedades", e);
                try {
                    Thread.sleep(POLL_INTERVAL_MILLIS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        Log.d(TAG, "Loop de polling finalizado");
    }

    /** Inicializa el estado conocido con lo cacheado en Room para detectar cambios offline. */
    private void seedFromCache(long userId) {
        List<OfflineBookingEntity> cached = offlineBookingDao.getConfirmedByUser(userId);
        if (cached == null) return;
        for (OfflineBookingEntity e : cached) {
            lastStartTime.put(e.id, e.sessionStartTime);
            lastStatus.put(e.id, e.status);
        }
    }

    private void consultarNovedades(long userId) throws Exception {
        Response<BookingsPageResponse> resp =
                bookingService.listBookings("users/" + userId + "/bookings").execute();
        if (!resp.isSuccessful() || resp.body() == null || resp.body().items == null) {
            return;
        }

        for (BookingResponse b : resp.body().items) {
            if (b == null || b.id == null) continue;

            String prevStatus = lastStatus.get(b.id);
            String prevStart = lastStartTime.get(b.id);

            boolean known = prevStatus != null;

            // Cancelación por la operadora: pasó de CONFIRMED a CANCELLED.
            if (known && "CANCELLED".equalsIgnoreCase(b.status)
                    && !"CANCELLED".equalsIgnoreCase(prevStatus)) {
                NotificationHelper.showBookingNotification(this,
                        NotificationHelper.CHANNEL_NOVEDADES, b.id,
                        getString(R.string.notif_cancelled_title),
                        getString(R.string.notif_cancelled_text, safe(b.activityName)));
                ReminderScheduler.cancel(this, b.id);
            }
            // Reprogramación: cambió el horario de inicio de una reserva activa.
            else if (known && "CONFIRMED".equalsIgnoreCase(b.status)
                    && b.sessionStartTime != null
                    && !b.sessionStartTime.equals(prevStart)) {
                NotificationHelper.showBookingNotification(this,
                        NotificationHelper.CHANNEL_NOVEDADES, b.id,
                        getString(R.string.notif_rescheduled_title),
                        getString(R.string.notif_rescheduled_text, safe(b.activityName)));
                ReminderScheduler.schedule(this, b.id, b.activityName, b.sessionStartTime);
            }

            lastStatus.put(b.id, b.status);
            lastStartTime.put(b.id, b.sessionStartTime);
        }
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        if (pollingThread != null) {
            pollingThread.interrupt();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
