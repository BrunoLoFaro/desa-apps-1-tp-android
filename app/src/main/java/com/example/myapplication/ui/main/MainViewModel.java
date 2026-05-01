package com.example.myapplication.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.repository.BookingRepository;
import com.example.myapplication.data.repository.ProfileRepository;
import com.example.myapplication.data.repository.SessionRepository;
import com.example.myapplication.data.repository.TourRepository;
import com.example.myapplication.util.NetworkMonitor;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public class MainViewModel extends ViewModel {

    private final BookingRepository bookingRepository;
    private final ProfileRepository profileRepository;
    private final TourRepository tourRepository;
    private final NetworkMonitor networkMonitor;

    @Inject
    public MainViewModel(BookingRepository bookingRepository,
                         ProfileRepository profileRepository,
                         SessionRepository sessionRepository,
                         TourRepository tourRepository,
                         NetworkMonitor networkMonitor) {
        this.bookingRepository = bookingRepository;
        this.profileRepository = profileRepository;
        this.tourRepository = tourRepository;
        this.networkMonitor = networkMonitor;

        if (sessionRepository.hasValidSession() && networkMonitor.isCurrentlyOnline()) {
            syncBookings();
            syncHistorial();
            syncActivities();
        }
    }

    /** Observe from any fragment to react to connectivity changes without registering callbacks. */
    public LiveData<Boolean> isOnline() {
        return networkMonitor.isOnline();
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

    private void syncActivities() {
        tourRepository.getAllTours(new RepositoryCallback<List<TourActivity>>() {
            @Override public void onSuccess(List<TourActivity> data) {}
            @Override public void onError(UiMessage e) {}
        });
    }

    @Override
    protected void onCleared() {
        bookingRepository.cancelAll();
        profileRepository.cancelAll();
        tourRepository.cancelAll();
        super.onCleared();
    }
}
