package com.example.myapplication.ui.bookings.viewmodel;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.data.repository.BookingRepository;
import com.example.myapplication.data.repository.ProfileRepository;
import com.example.myapplication.data.repository.ReviewRepository;
import com.example.myapplication.data.work.SyncCancellationsWorker;
import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
    private boolean offline = false;
    private String currentFilter = null;

    // ── Historial ────────────────────────────────────────────────────────────
    private final MutableLiveData<List<BookingSummaryItem>> _historial = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _historialLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<String>> _availableDestinations =
            new MutableLiveData<>(Collections.emptyList());
    private List<BookingSummaryItem> allHistorialItems = Collections.emptyList();
    private String filterDestination = "";
    private String filterFrom = "";
    private String filterTo = "";
    private boolean historialLoaded = false;
    private List<BookingSummaryItem> cachedServerHistorialItems = Collections.emptyList();
    private List<BookingResponse> reconnectPendingSnapshot = null;
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
        this.offline = !isOnline();
        if (this.offline) _isOffline.setValue(true);
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

    public void onConnectivityChanged(boolean isOnline) {
        offline = !isOnline;
        _isOffline.setValue(offline);
        if (isOnline) {
            // Leer el snapshot de Room ANTES de encolar el WorkManager para evitar la race condition
            // donde el worker borra la fila de pendingCancel antes de que loadHistorial pueda leerla.
            bookingRepository.loadCachedPendingCancellations(new RepositoryCallback<List<BookingResponse>>() {
                @Override
                public void onSuccess(List<BookingResponse> snapshot) {
                    reconnectPendingSnapshot = snapshot != null ? snapshot : Collections.emptyList();
                    enqueueSyncWorker();
                    loadMyBookings(currentFilter != null ? currentFilter : "CONFIRMED");
                    historialLoaded = false;
                    if (selectedTab == 1) loadHistorial();
                }
                @Override
                public void onError(UiMessage e) {
                    reconnectPendingSnapshot = Collections.emptyList();
                    enqueueSyncWorker();
                    loadMyBookings(currentFilter != null ? currentFilter : "CONFIRMED");
                    historialLoaded = false;
                    if (selectedTab == 1) loadHistorial();
                }
            });
        } else {
            loadCachedBookings();
        }
    }

    private void enqueueSyncWorker() {
        OneTimeWorkRequest syncWork = new OneTimeWorkRequest.Builder(SyncCancellationsWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,
                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                "sync_cancellations", ExistingWorkPolicy.KEEP, syncWork);
    }

    public void loadMyBookings(String statusFilter) {
        currentFilter = statusFilter;
        // Capturar IDs actuales antes del refresh para detectar cancelaciones del servidor (O17)
        final Set<Long> prevIds = new HashSet<>();
        if ("CONFIRMED".equals(statusFilter)) {
            List<BookingResponse> cur = _bookings.getValue();
            if (cur != null) {
                for (BookingResponse b : cur) { if (b.id != null) prevIds.add(b.id); }
            }
        }
        _loading.setValue(true);

        if (offline) {
            loadCachedBookings();
            return;
        }

        bookingRepository.listMyBookings(statusFilter, new RepositoryCallback<List<BookingResponse>>() {
            @Override
            public void onSuccess(List<BookingResponse> data) {
                _loading.setValue(false);
                List<BookingResponse> fresh = data != null ? data : Collections.emptyList();
                if (!prevIds.isEmpty()) {
                    Set<Long> newIds = new HashSet<>();
                    for (BookingResponse b : fresh) { if (b.id != null) newIds.add(b.id); }
                    prevIds.removeAll(newIds);
                    if (!prevIds.isEmpty()) {
                        _message.setValue(UiMessage.from(R.string.booking_cancelled_by_server));
                    }
                }
                _bookings.setValue(fresh);
            }

            @Override
            public void onError(UiMessage error) {
                _loading.setValue(false);
                _error.setValue(error);
            }
        });
    }

    private void loadCachedBookings() {
        bookingRepository.loadCachedConfirmedBookings(new RepositoryCallback<List<BookingResponse>>() {
            @Override
            public void onSuccess(List<BookingResponse> cached) {
                _loading.setValue(false);
                _bookings.setValue(cached != null ? cached : Collections.emptyList());
            }

            @Override
            public void onError(UiMessage e) {
                _loading.setValue(false);
            }
        });
    }

    public void cancelBooking(Long bookingId) {
        if (bookingId == null) return;

        if (offline) {
            bookingRepository.cancelBookingLocally(bookingId, () -> {
                _message.setValue(UiMessage.from(R.string.cancel_booking_offline_queued));
                loadMyBookings(currentFilter);
                historialLoaded = false;
                if (selectedTab == 1) loadHistorial();
            });
            return;
        }

        _loading.setValue(true);
        bookingRepository.cancelBooking(bookingId, new RepositoryCallback<BookingResponse>() {
            @Override
            public void onSuccess(BookingResponse data) {
                _loading.setValue(false);
                loadMyBookings(currentFilter);
                historialLoaded = false;
                if (selectedTab == 1) loadHistorial();
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
        if (reconnectPendingSnapshot != null) {
            List<BookingResponse> snapshot = reconnectPendingSnapshot;
            reconnectPendingSnapshot = null;
            fetchHistorialWithPending(snapshot);
            return;
        }
        bookingRepository.loadCachedPendingCancellations(new RepositoryCallback<List<BookingResponse>>() {
            @Override
            public void onSuccess(List<BookingResponse> pending) {
                fetchHistorialWithPending(pending);
            }
            @Override
            public void onError(UiMessage e) {
                fetchHistorialWithPending(null);
            }
        });
    }

    // pending == null indica fallo de DB: se omite el merge y se usa solo el servidor.
    private void fetchHistorialWithPending(List<BookingResponse> pending) {
        List<BookingSummaryItem> pendingItems = new ArrayList<>();
        Set<Long> pendingIds = new java.util.HashSet<>();
        if (pending != null) {
            for (BookingResponse p : pending) {
                BookingSummaryItem item = toSummaryItem(p);
                pendingItems.add(item);
                if (p.id != null) pendingIds.add(p.id);
            }
        }

        // Sin red: usar datos locales directamente sin esperar timeout de red
        if (offline) {
            historialLoaded = true;
            List<BookingSummaryItem> all = new ArrayList<>(pendingItems);
            for (BookingSummaryItem cached : cachedServerHistorialItems) {
                if (cached.getId() == null || !pendingIds.contains(cached.getId())) all.add(cached);
            }
            allHistorialItems = all;
            updateDestinationSuggestions();
            applyFilters();
            _historialLoading.setValue(false);
            return;
        }

        profileRepository.getActivitySummary(new RepositoryCallback<List<BookingSummaryItem>>() {
            @Override
            public void onSuccess(List<BookingSummaryItem> data) {
                List<BookingSummaryItem> serverItems = data != null ? data : Collections.emptyList();
                cachedServerHistorialItems = serverItems;
                historialLoaded = true;
                List<BookingSummaryItem> all = new ArrayList<>();
                for (BookingSummaryItem p : pendingItems) {
                    if (p.getId() == null || !containsId(serverItems, p.getId())) all.add(p);
                }
                all.addAll(serverItems);
                allHistorialItems = all;
                updateDestinationSuggestions();
                applyFilters();
                _historialLoading.setValue(false);
            }

            @Override
            public void onError(UiMessage error) {
                historialLoaded = true;
                List<BookingSummaryItem> all = new ArrayList<>();
                if (pending == null) {
                    // DB también falló: usar solo caché del servidor anterior
                    all.addAll(cachedServerHistorialItems);
                } else {
                    all.addAll(pendingItems);
                    for (BookingSummaryItem cached : cachedServerHistorialItems) {
                        if (cached.getId() == null || !pendingIds.contains(cached.getId())) all.add(cached);
                    }
                    updateDestinationSuggestions();
                }
                allHistorialItems = all;
                applyFilters();
                _historialLoading.setValue(false);
                if (all.isEmpty()) _error.setValue(error);
            }
        });
    }

    private static boolean containsId(List<BookingSummaryItem> items, Long id) {
        for (BookingSummaryItem item : items) {
            if (id.equals(item.getId())) return true;
        }
        return false;
    }

    private BookingSummaryItem toSummaryItem(BookingResponse b) {
        String date = "";
        String time = "";
        if (b.sessionStartTime != null && b.sessionStartTime.length() >= 10) {
            date = b.sessionStartTime.substring(0, 10);
            if (b.sessionStartTime.length() >= 16) time = b.sessionStartTime.substring(11, 16);
        }
        String price = b.currency != null ? b.totalPrice + " " + b.currency : "";
        String destination = b.destination != null ? b.destination.name : "";
        return new BookingSummaryItem(b.id, b.activityId, b.activityName,
                b.status, date, price, destination, b.guideName, b.durationMinutes, null, time);
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
