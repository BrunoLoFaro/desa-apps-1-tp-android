package com.example.myapplication.ui.bookings.viewmodel;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.data.repository.BookingRepository;
import com.example.myapplication.data.repository.ProfileRepository;
import com.example.myapplication.data.repository.ReviewRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

@HiltViewModel
public class BookingsViewModel extends ViewModel {

    private final BookingRepository bookingRepository;
    private final ProfileRepository profileRepository;
    private final ReviewRepository reviewRepository;
    private final Context context;

    private final MutableLiveData<List<BookingResponse>> _bookings =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    private final MutableLiveData<UiMessage> _message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isOffline = new MutableLiveData<>(false);
    private String currentFilter = null;

    // ── Historial ────────────────────────────────────────────────────────────
    private final MutableLiveData<List<BookingSummaryItem>> _historial =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> _historialLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<String>> _availableDestinations =
            new MutableLiveData<>(Collections.emptyList());
    private List<BookingSummaryItem> allHistorialItems = Collections.emptyList();
    private String filterDestination = "";
    private String filterFrom = "";
    private String filterTo = "";
    private boolean historialLoaded = false;
    private int selectedTab = 0;

    @Inject
    public BookingsViewModel(BookingRepository bookingRepository,
                             ProfileRepository profileRepository,
                             ReviewRepository reviewRepository,
                             @ApplicationContext Context context) {
        this.bookingRepository = bookingRepository;
        this.profileRepository = profileRepository;
        this.reviewRepository = reviewRepository;
        this.context = context;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public LiveData<List<BookingResponse>> getBookings() { return _bookings; }
    public LiveData<UiMessage> getError() { return _error; }
    public LiveData<UiMessage> getMessage() { return _message; }
    public LiveData<Boolean> isLoading() { return _loading; }
    public LiveData<Boolean> isOffline() { return _isOffline; }

    public int getSelectedTab() { return selectedTab; }
    public void setSelectedTab(int tab) { selectedTab = tab; }

    public LiveData<List<BookingSummaryItem>> getHistorial() { return _historial; }
    public LiveData<Boolean> isHistorialLoading() { return _historialLoading; }
    public LiveData<List<String>> getAvailableDestinations() { return _availableDestinations; }

    public void clearMessage() { _message.setValue(null); }

    // ── Activas actions ──────────────────────────────────────────────────────

    public void loadMyBookings(String statusFilter) {
        currentFilter = statusFilter;
        _loading.setValue(true);

        if (!isOnline()) {
            _isOffline.setValue(true);
            bookingRepository.loadCachedConfirmedBookings(new RepositoryCallback<List<BookingResponse>>() {
                @Override
                public void onSuccess(List<BookingResponse> data) {
                    _loading.setValue(false);
                    _bookings.setValue(data != null ? data : Collections.emptyList());
                }

                @Override
                public void onError(UiMessage error) {
                    _loading.setValue(false);
                    _bookings.setValue(Collections.emptyList());
                }
            });
            return;
        }

        _isOffline.setValue(false);
        bookingRepository.listMyBookings(statusFilter, new RepositoryCallback<List<BookingResponse>>() {
            @Override
            public void onSuccess(List<BookingResponse> data) {
                _loading.setValue(false);
                _bookings.setValue(data != null ? data : Collections.emptyList());
            }

            @Override
            public void onError(UiMessage error) {
                _loading.setValue(false);
                _error.setValue(error);
            }
        });
    }

    public void syncBookings() {
        if (!isOnline()) return;
        _isOffline.setValue(false);
        bookingRepository.syncPendingCancellations(() ->
                loadMyBookings(currentFilter != null ? currentFilter : "CONFIRMED"));
    }

    public void cancelBooking(Long bookingId) {
        if (bookingId == null) return;

        if (!isOnline()) {
            bookingRepository.cancelBookingLocally(bookingId,
                    () -> loadMyBookings(currentFilter));
            return;
        }

        _loading.setValue(true);
        bookingRepository.cancelBooking(bookingId, new RepositoryCallback<BookingResponse>() {
            @Override
            public void onSuccess(BookingResponse data) {
                _loading.setValue(false);
                loadMyBookings(currentFilter);
            }

            @Override
            public void onError(UiMessage error) {
                _loading.setValue(false);
                _error.setValue(error);
            }
        });
    }

    // ── Historial actions ────────────────────────────────────────────────────

    public void loadHistorialIfNeeded() {
        if (historialLoaded) return;
        loadHistorial();
    }

    public void loadHistorial() {
        _historialLoading.setValue(true);
        profileRepository.getActivitySummary(new RepositoryCallback<List<BookingSummaryItem>>() {
            @Override
            public void onSuccess(List<BookingSummaryItem> data) {
                historialLoaded = true;
                allHistorialItems = data != null ? data : Collections.emptyList();
                updateDestinationSuggestions();
                applyFilters();
                _historialLoading.setValue(false);
            }

            @Override
            public void onError(UiMessage error) {
                historialLoaded = true;
                allHistorialItems = Collections.emptyList();
                _historial.setValue(Collections.emptyList());
                _error.setValue(error);
                _historialLoading.setValue(false);
            }
        });
    }

    public void setFilterDestination(String destination) {
        filterDestination = destination != null ? destination.trim() : "";
        applyFilters();
    }

    public void setFilterFrom(String date) {
        filterFrom = date != null ? date.trim() : "";
        applyFilters();
    }

    public void setFilterTo(String date) {
        filterTo = date != null ? date.trim() : "";
        applyFilters();
    }

    public void clearFilters() {
        filterDestination = "";
        filterFrom = "";
        filterTo = "";
        applyFilters();
    }

    private void applyFilters() {
        List<BookingSummaryItem> filtered = new ArrayList<>();
        for (BookingSummaryItem item : allHistorialItems) {
            if (!filterDestination.isEmpty()
                    && !item.getDestination().toLowerCase().contains(filterDestination.toLowerCase())) {
                continue;
            }
            if (!filterFrom.isEmpty() && item.getDate().compareTo(filterFrom) < 0) continue;
            if (!filterTo.isEmpty() && item.getDate().compareTo(filterTo) > 0) continue;
            filtered.add(item);
        }
        _historial.setValue(filtered);
    }

    private void updateDestinationSuggestions() {
        Set<String> seen = new LinkedHashSet<>();
        for (BookingSummaryItem item : allHistorialItems) {
            if (item.getDestination() != null && !item.getDestination().isEmpty()) {
                seen.add(item.getDestination());
            }
        }
        _availableDestinations.setValue(new ArrayList<>(seen));
    }

    public void submitReview(Long bookingId, int activityRating, Integer guideRating, String comment) {
        if (bookingId == null) return;
        _loading.setValue(true);
        reviewRepository.createReview(bookingId, activityRating, guideRating, comment,
                new RepositoryCallback<com.example.myapplication.data.model.ReviewSummaryResponse>() {
                    @Override
                    public void onSuccess(com.example.myapplication.data.model.ReviewSummaryResponse data) {
                        _loading.setValue(false);
                        _message.setValue(UiMessage.from(R.string.review_thanks));
                        loadMyBookings(currentFilter);
                    }

                    @Override
                    public void onError(UiMessage error) {
                        _loading.setValue(false);
                        _error.setValue(error);
                    }
                });
    }

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    protected void onCleared() {
        bookingRepository.cancelAll();
        profileRepository.cancelAll();
        reviewRepository.cancelAll();
        super.onCleared();
    }
}
