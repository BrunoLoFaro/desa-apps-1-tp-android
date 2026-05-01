package com.example.myapplication.ui.bookings.viewmodel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.data.model.ReviewResponse;
import com.example.myapplication.data.repository.BookingRepository;
import com.example.myapplication.data.repository.ProfileRepository;
import com.example.myapplication.data.repository.ReviewRepository;
import com.example.myapplication.data.work.SyncCancellationsWorker;
import com.example.myapplication.util.NetworkMonitor;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
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
    private final NetworkMonitor networkMonitor;
    private Observer<Boolean> connectivityObserver;
    private boolean connectivityInitialized = false;

    private final MutableLiveData<List<BookingResponse>> _bookings =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    private final MutableLiveData<UiMessage> _message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isOffline = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _showOfflineCancelModal = new MutableLiveData<>(false);
    private boolean offline = false;
    private String currentFilter = null;
    private Long lastUserCancelledBookingId = null;

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
    private List<BookingSummaryItem> cachedServerHistorialItems = Collections.emptyList();
    private int selectedTab = 0;

    // ── Mis Calificaciones ────────────────────────────────────────────────────
    private final MutableLiveData<List<ReviewResponse>> _myReviews =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> _myReviewsLoading = new MutableLiveData<>(false);
    private boolean myReviewsLoaded = false;

    private final MutableLiveData<Boolean> _historialNeverSynced = new MutableLiveData<>(false);

    // WorkManager sync observation
    private LiveData<List<WorkInfo>> syncWorkInfoLiveData;
    private Observer<List<WorkInfo>> syncWorkObserver;

    @Inject
    public BookingsViewModel(BookingRepository bookingRepository,
                             ProfileRepository profileRepository,
                             ReviewRepository reviewRepository,
                             NetworkMonitor networkMonitor,
                             @ApplicationContext Context context) {
        this.bookingRepository = bookingRepository;
        this.profileRepository = profileRepository;
        this.reviewRepository = reviewRepository;
        this.context = context;
        this.networkMonitor = networkMonitor;
        this.offline = !networkMonitor.isCurrentlyOnline();
        if (this.offline) _isOffline.setValue(true);

        connectivityObserver = online -> {
            if (!connectivityInitialized) {
                connectivityInitialized = true;
                return; // skip initial emission; initial load is triggered by the fragment
            }
            onConnectivityChanged(Boolean.TRUE.equals(online));
        };
        networkMonitor.isOnline().observeForever(connectivityObserver);
    }

    // ──────────────── Getters ────────────────────────────────────────────────────────────────
    public LiveData<List<BookingResponse>> getBookings() { return _bookings; }
    public LiveData<UiMessage> getError() { return _error; }
    public LiveData<UiMessage> getMessage() { return _message; }
    public LiveData<Boolean> isLoading() { return _loading; }
    public LiveData<Boolean> isOffline() { return _isOffline; }
    public LiveData<Boolean> isShowOfflineCancelModal() { return _showOfflineCancelModal; }

    public int getSelectedTab() { return selectedTab; }
    public void setSelectedTab(int tab) { selectedTab = tab; }

    public LiveData<List<BookingSummaryItem>> getHistorial() { return _historial; }
    public LiveData<Boolean> isHistorialLoading() { return _historialLoading; }
    public LiveData<Boolean> isHistorialNeverSynced() { return _historialNeverSynced; }
    public LiveData<List<String>> getAvailableDestinations() { return _availableDestinations; }

    public LiveData<List<ReviewResponse>> getMyReviews() { return _myReviews; }
    public LiveData<Boolean> isMyReviewsLoading() { return _myReviewsLoading; }

    public void clearMessage() { _message.setValue(null); }
    public void clearOfflineCancelModal() { _showOfflineCancelModal.setValue(false); }

    // ──────────────── Activas actions ──────────────────────────────────────────────────────────

    public void onConnectivityChanged(boolean isOnline) {
        offline = !isOnline;
        _isOffline.setValue(offline);
        if (isOnline) {
            enqueueSyncWorker();
            observeSyncWorker();
            loadMyBookings(currentFilter != null ? currentFilter : "CONFIRMED");
            historialLoaded = false;
            if (selectedTab == 1) loadHistorial();
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

    private void observeSyncWorker() {
        if (syncWorkObserver != null && syncWorkInfoLiveData != null) {
            syncWorkInfoLiveData.removeObserver(syncWorkObserver);
        }
        syncWorkInfoLiveData = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData("sync_cancellations");
        syncWorkObserver = workInfos -> {
            if (workInfos == null || workInfos.isEmpty()) return;
            WorkInfo info = workInfos.get(0);
            if (info.getState().isFinished()) {
                if (info.getState() == WorkInfo.State.SUCCEEDED) {
                    int failedCount = info.getOutputData().getInt("failed_count", 0);
                    if (failedCount > 0) {
                        _message.postValue(UiMessage.from(R.string.cancel_sync_error));
                        loadMyBookings(currentFilter != null ? currentFilter : "CONFIRMED");
                    }
                }
                if (syncWorkInfoLiveData != null && syncWorkObserver != null) {
                    syncWorkInfoLiveData.removeObserver(syncWorkObserver);
                    syncWorkObserver = null;
                    syncWorkInfoLiveData = null;
                }
            }
        };
        syncWorkInfoLiveData.observeForever(syncWorkObserver);
    }

    public void loadMyBookings(String statusFilter) {
        currentFilter = statusFilter;
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
                        boolean isOnlyUserCancellation = lastUserCancelledBookingId != null
                                && prevIds.size() == 1
                                && prevIds.contains(lastUserCancelledBookingId);
                        if (!isOnlyUserCancellation) {
                            _message.setValue(UiMessage.from(R.string.booking_cancelled_by_server));
                        }
                        if (lastUserCancelledBookingId != null && prevIds.contains(lastUserCancelledBookingId)) {
                            lastUserCancelledBookingId = null;
                        }
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
                _showOfflineCancelModal.setValue(true);
                loadMyBookings(currentFilter);
            });
            return;
        }

        _loading.setValue(true);
        lastUserCancelledBookingId = bookingId;
        bookingRepository.cancelBooking(bookingId, new RepositoryCallback<BookingResponse>() {
            @Override
            public void onSuccess(BookingResponse data) {
                _loading.setValue(false);
                _message.setValue(UiMessage.from(R.string.cancel_booking_success));
                loadMyBookings(currentFilter);
                historialLoaded = false;
                if (selectedTab == 1) loadHistorial();
            }

            @Override
            public void onError(UiMessage error) {
                _loading.setValue(false);
                _error.setValue(error);
                if (lastUserCancelledBookingId != null && lastUserCancelledBookingId.equals(bookingId)) {
                    lastUserCancelledBookingId = null;
                }
            }
        });
    }

    // ──────────────── Historial actions ───────────────────────────────────────────────────────────

    public void loadHistorialIfNeeded() {
        if (historialLoaded) return;
        loadHistorial();
    }

    public void loadHistorial() {
        _historialLoading.setValue(true);
        fetchHistorial();
    }

    private void fetchHistorial() {
        if (offline) {
            bookingRepository.loadCachedHistorial(new RepositoryCallback<List<BookingSummaryItem>>() {
                @Override
                public void onSuccess(List<BookingSummaryItem> roomItems) {
                    cachedServerHistorialItems = roomItems != null ? roomItems : Collections.emptyList();
                    historialLoaded = true;
                    allHistorialItems = new ArrayList<>(cachedServerHistorialItems);
                    updateDestinationSuggestions();
                    applyFilters();
                    _historialLoading.setValue(false);
                    _historialNeverSynced.setValue(cachedServerHistorialItems.isEmpty());
                }

                @Override
                public void onError(UiMessage e) {
                    historialLoaded = true;
                    allHistorialItems = Collections.emptyList();
                    updateDestinationSuggestions();
                    applyFilters();
                    _historialLoading.setValue(false);
                    _historialNeverSynced.setValue(true);
                }
            });
            return;
        }

        profileRepository.getActivitySummary(new RepositoryCallback<List<BookingSummaryItem>>() {
            @Override
            public void onSuccess(List<BookingSummaryItem> data) {
                List<BookingSummaryItem> serverItems = data != null ? data : Collections.emptyList();
                cachedServerHistorialItems = serverItems;
                historialLoaded = true;
                allHistorialItems = new ArrayList<>(serverItems);
                updateDestinationSuggestions();
                applyFilters();
                _historialLoading.setValue(false);
                _historialNeverSynced.setValue(false);
            }

            @Override
            public void onError(UiMessage error) {
                historialLoaded = true;
                allHistorialItems = new ArrayList<>(cachedServerHistorialItems);
                updateDestinationSuggestions();
                applyFilters();
                _historialLoading.setValue(false);
                boolean neverSynced = cachedServerHistorialItems.isEmpty();
                _historialNeverSynced.setValue(neverSynced);
                if (allHistorialItems.isEmpty()) _error.setValue(error);
            }
        });
    }

    public void loadMyReviewsIfNeeded() {
        if (myReviewsLoaded) return;
        loadMyReviews();
    }

    public void loadMyReviews() {
        _myReviewsLoading.setValue(true);
        profileRepository.getMyReviews(new RepositoryCallback<List<ReviewResponse>>() {
            @Override
            public void onSuccess(List<ReviewResponse> data) {
                myReviewsLoaded = true;
                _myReviews.setValue(data != null ? data : Collections.emptyList());
                _myReviewsLoading.setValue(false);
            }

            @Override
            public void onError(UiMessage error) {
                myReviewsLoaded = true;
                _myReviews.setValue(Collections.emptyList());
                _error.setValue(error);
                _myReviewsLoading.setValue(false);
            }
        });
    }

    public String getFilterDestination() { return filterDestination; }
    public String getFilterFrom() { return filterFrom; }
    public String getFilterTo() { return filterTo; }
    public boolean hasActiveFilters() {
        return !filterDestination.isEmpty() || !filterFrom.isEmpty() || !filterTo.isEmpty();
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
                        updateLocalReviewStatus(bookingId);
                        myReviewsLoaded = false; // Force reload reviews tab
                        loadMyBookings(currentFilter);
                    }

                    @Override
                    public void onError(UiMessage error) {
                        _loading.setValue(false);
                        _error.setValue(error);
                        // Si el error es que ya existe la reseña, también bloqueamos el botón localmente
                        if (isAlreadyExistsError(error)) {
                            updateLocalReviewStatus(bookingId);
                        }
                    }
                });
    }

    private void updateLocalReviewStatus(Long bookingId) {
        List<BookingSummaryItem> newList = new ArrayList<>();
        boolean changed = false;
        for (BookingSummaryItem item : allHistorialItems) {
            if (item.getId().equals(bookingId)) {
                newList.add(new BookingSummaryItem(
                        item.getId(), item.getActivityId(), item.getActivityName(),
                        item.getStatus(), item.getDate(), item.getPrice(),
                        item.getDestination(), item.getGuideName(),
                        item.getDurationMinutes(), item.getImageUrl(),
                        item.getTime(), false, item.getSessionStartTime()
                ));
                changed = true;
            } else {
                newList.add(item);
            }
        }
        if (changed) {
            allHistorialItems = newList;
            applyFilters();
        }
    }

    private boolean isAlreadyExistsError(UiMessage error) {
        if (error instanceof UiMessage.ResMessage) {
            return ((UiMessage.ResMessage) error).resId == R.string.error_review_already_exists;
        }
        return false;
    }


    @Override
    protected void onCleared() {
        if (connectivityObserver != null) {
            networkMonitor.isOnline().removeObserver(connectivityObserver);
            connectivityObserver = null;
        }
        if (syncWorkInfoLiveData != null && syncWorkObserver != null) {
            syncWorkInfoLiveData.removeObserver(syncWorkObserver);
        }
        bookingRepository.cancelAll();
        profileRepository.cancelAll();
        reviewRepository.cancelAll();
        super.onCleared();
    }
}
