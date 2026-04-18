package com.example.myapplication.ui.bookings.viewmodel;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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
    private final SessionRepository sessionRepository;
    private final Context context;

    // ── Activas ──────────────────────────────────────────────────────────────
    private final MutableLiveData<List<BookingResponse>> _bookings =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
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
    private ConnectivityManager.NetworkCallback networkCallback;

    @Inject
    public BookingsViewModel(BookingRepository bookingRepository,
                             ProfileRepository profileRepository,
                             SessionRepository sessionRepository,
                             @ApplicationContext Context context) {
        this.bookingRepository = bookingRepository;
        this.profileRepository = profileRepository;
        this.sessionRepository = sessionRepository;
        this.context = context;
        registerConnectivityCallback();
    }

    private void registerConnectivityCallback() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (Boolean.TRUE.equals(_isOffline.getValue())) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            loadMyBookings(currentFilter != null ? currentFilter : "CONFIRMED"));
                }
            }
        };
        cm.registerDefaultNetworkCallback(networkCallback);
    }

    // ── Activas getters ──────────────────────────────────────────────────────
    public LiveData<List<BookingResponse>> getBookings() { return _bookings; }
    public LiveData<UiMessage> getError() { return _error; }
    public LiveData<Boolean> isLoading() { return _loading; }
    public LiveData<Boolean> isOffline() { return _isOffline; }

    // ── Tab state ────────────────────────────────────────────────────────────
    public int getSelectedTab() { return selectedTab; }
    public void setSelectedTab(int tab) { selectedTab = tab; }

    // ── Historial getters ────────────────────────────────────────────────────
    public LiveData<List<BookingSummaryItem>> getHistorial() { return _historial; }
    public LiveData<Boolean> isHistorialLoading() { return _historialLoading; }
    public LiveData<List<String>> getAvailableDestinations() { return _availableDestinations; }

    // ── Activas actions ──────────────────────────────────────────────────────

    public void loadMyBookings(String statusFilter) {
        currentFilter = statusFilter;
        _loading.setValue(true);

        if (!isOnline()) {
            List<BookingResponse> cached = bookingRepository.getCachedBookings(sessionRepository.getUserId());
            _loading.setValue(false);
            if (cached != null) {
                _isOffline.setValue(true);
                _bookings.setValue(cached);
            } else {
                _isOffline.setValue(false);
                _bookings.setValue(Collections.emptyList());
            }
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
                List<BookingResponse> cached = bookingRepository.getCachedBookings(
                        sessionRepository.getUserId());
                _loading.setValue(false);
                if (cached != null && !cached.isEmpty()) {
                    _isOffline.setValue(true);
                    _bookings.setValue(cached);
                } else {
                    _error.setValue(error);
                }
            }
        });
    }

    public void cancelBooking(Long bookingId) {
        if (bookingId == null) return;
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

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    protected void onCleared() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (networkCallback != null) {
            cm.unregisterNetworkCallback(networkCallback);
        }
        bookingRepository.cancelAll();
        profileRepository.cancelAll();
        super.onCleared();
    }
}
