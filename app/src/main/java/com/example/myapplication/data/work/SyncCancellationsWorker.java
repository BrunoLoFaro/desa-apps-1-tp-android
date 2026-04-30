package com.example.myapplication.data.work;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.myapplication.data.local.OfflineBookingDao;
import com.example.myapplication.data.local.OfflineBookingEntity;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.network.BookingService;
import com.example.myapplication.data.repository.SessionRepository;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import java.util.List;

@HiltWorker
public class SyncCancellationsWorker extends Worker {

    private static final String TAG = "SyncCancellationsWorker";

    private final BookingService bookingService;
    private final OfflineBookingDao offlineBookingDao;
    private final SessionRepository sessionRepository;

    @AssistedInject
    public SyncCancellationsWorker(
            @Assisted Context context,
            @Assisted WorkerParameters params,
            BookingService bookingService,
            OfflineBookingDao offlineBookingDao,
            SessionRepository sessionRepository) {
        super(context, params);
        this.bookingService = bookingService;
        this.offlineBookingDao = offlineBookingDao;
        this.sessionRepository = sessionRepository;
    }

    @NonNull
    @Override
    public Result doWork() {
        long userId = sessionRepository.getUserId();
        List<OfflineBookingEntity> pending = offlineBookingDao.getPendingCancellations(userId);

        boolean anyRetryable = false;
        int rejectedCount = 0;
        for (OfflineBookingEntity e : pending) {
            try {
                retrofit2.Response<BookingResponse> resp =
                        bookingService.cancelBooking("users/" + userId + "/bookings/" + e.id).execute();
                if (resp.isSuccessful()) {
                    offlineBookingDao.deleteById(e.id);
                    Log.d(TAG, "Cancelación sincronizada: booking " + e.id);
                } else if (resp.code() >= 400 && resp.code() < 500) {
                    // 4xx: el servidor rechazó la cancelación → restaurar a CONFIRMED y reportar al usuario
                    offlineBookingDao.clearPendingCancel(e.id);
                    rejectedCount++;
                    Log.w(TAG, "Cancelación rechazada por el servidor (HTTP " + resp.code() + "), booking " + e.id + " restaurado a CONFIRMED");
                } else {
                    Log.w(TAG, "Sync fallida para booking " + e.id + " — HTTP " + resp.code());
                    anyRetryable = true;
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error sincronizando booking " + e.id, ex);
                anyRetryable = true;
            }
        }

        Data output = new Data.Builder().putInt("failed_count", rejectedCount).build();
        return anyRetryable ? Result.retry() : Result.success(output);
    }
}
