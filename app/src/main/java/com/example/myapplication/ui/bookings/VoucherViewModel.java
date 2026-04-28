package com.example.myapplication.ui.bookings;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.local.OfflineBookingDao;
import com.example.myapplication.data.local.OfflineBookingEntity;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Inject;

@HiltViewModel
public class VoucherViewModel extends ViewModel {

    private final OfflineBookingDao offlineBookingDao;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<OfflineBookingEntity> _booking = new MutableLiveData<>();

    public LiveData<OfflineBookingEntity> getBooking() { return _booking; }

    @Inject
    public VoucherViewModel(OfflineBookingDao offlineBookingDao, SavedStateHandle savedStateHandle) {
        this.offlineBookingDao = offlineBookingDao;
        long bookingId = savedStateHandle.get("bookingId") != null
                ? savedStateHandle.<Long>get("bookingId") : -1L;
        if (bookingId > 0) load(bookingId);
    }

    public void load(long bookingId) {
        dbExecutor.execute(() -> {
            OfflineBookingEntity entity = offlineBookingDao.getById(bookingId);
            new Handler(Looper.getMainLooper()).post(() -> _booking.setValue(entity));
        });
    }
}
