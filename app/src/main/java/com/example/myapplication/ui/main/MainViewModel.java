package com.example.myapplication.ui.main;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.data.repository.BookingRepository;
import com.example.myapplication.data.repository.ProfileRepository;
import com.example.myapplication.data.repository.SessionRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public class MainViewModel extends ViewModel {

    private final BookingRepository bookingRepository;
    private final ProfileRepository profileRepository;

    @Inject
    public MainViewModel(BookingRepository bookingRepository,
                         ProfileRepository profileRepository,
                         SessionRepository sessionRepository,
                         @ApplicationContext Context context) {
        this.bookingRepository = bookingRepository;
        this.profileRepository = profileRepository;

        if (sessionRepository.hasValidSession() && isOnline(context)) {
            syncBookings();
            syncHistorial();
        }
    }

    private void syncBookings() {
        bookingRepository.listMyBookings("CONFIRMED", new RepositoryCallback<List<BookingResponse>>() {
            @Override public void onSuccess(List<BookingResponse> data) {}
            @Override public void onError(UiMessage e) {}
        });
    }

    private void syncHistorial() {
        profileRepository.getActivitySummary(new RepositoryCallback<List<BookingSummaryItem>>() {
            @Override public void onSuccess(List<BookingSummaryItem> data) {}
            @Override public void onError(UiMessage e) {}
        });
    }

    private boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    protected void onCleared() {
        bookingRepository.cancelAll();
        profileRepository.cancelAll();
        super.onCleared();
    }
}
