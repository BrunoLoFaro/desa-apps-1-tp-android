package com.example.myapplication.ui.main;

import android.content.Context;
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
import com.example.myapplication.util.ConnectivityUtils;
import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public class MainViewModel extends ViewModel {

    private final BookingRepository bookingRepository;
    private final ProfileRepository profileRepository;
    private final TourRepository tourRepository;

    @Inject
    public MainViewModel(BookingRepository bookingRepository,
                         ProfileRepository profileRepository,
                         SessionRepository sessionRepository,
                         TourRepository tourRepository,
                         @ApplicationContext Context context) {
        this.bookingRepository = bookingRepository;
        this.profileRepository = profileRepository;
        this.tourRepository = tourRepository;

        if (sessionRepository.hasValidSession() && ConnectivityUtils.isOnline(context)) {
            syncBookings();
            syncHistorial();
            syncActivities();
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
