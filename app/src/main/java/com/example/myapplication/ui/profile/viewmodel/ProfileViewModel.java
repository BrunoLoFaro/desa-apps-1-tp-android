package com.example.myapplication.ui.profile.viewmodel;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.data.model.UserProfileData;
import com.example.myapplication.data.repository.BookingRepository;
import com.example.myapplication.data.repository.ProfileRepository;
import com.example.myapplication.data.repository.TourRepository;
import com.example.myapplication.data.session.SessionManager;
import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final ProfileRepository profileRepository;
    private final TourRepository tourRepository;
    private final BookingRepository bookingRepository;
    private final SessionManager sessionManager;
    private final Context context;

    private final MutableLiveData<UserProfileData> _profile = new MutableLiveData<>();
    private final MutableLiveData<List<String>> _preferences = new MutableLiveData<>();
    private final MutableLiveData<List<String>> _categories = new MutableLiveData<>();
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _saveSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> _historialCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> _pendingCount = new MutableLiveData<>(0);
    private final MutableLiveData<List<BookingSummaryItem>> _recentActivities = new MutableLiveData<>();

    @Inject
    public ProfileViewModel(ProfileRepository profileRepository, TourRepository tourRepository,
                            BookingRepository bookingRepository,
                            SessionManager sessionManager,
                            @ApplicationContext Context context) {
        this.profileRepository = profileRepository;
        this.tourRepository = tourRepository;
        this.bookingRepository = bookingRepository;
        this.sessionManager = sessionManager;
        this.context = context;
        trySyncPendingProfile();
        loadAll();
    }

    public LiveData<UserProfileData> getProfile() { return _profile; }
    public LiveData<List<String>> getPreferences() { return _preferences; }
    public LiveData<List<String>> getCategories() { return _categories; }
    public LiveData<UiMessage> getError() { return _error; }
    public LiveData<Boolean> isLoading() { return _loading; }
    public LiveData<Boolean> isSaveSuccess() { return _saveSuccess; }
    public LiveData<Integer> getHistorialCount() { return _historialCount; }
    public LiveData<Integer> getPendingCount() { return _pendingCount; }
    public LiveData<List<BookingSummaryItem>> getRecentActivities() { return _recentActivities; }

    public List<String> getCurrentPreferences() {
        List<String> prefs = _preferences.getValue();
        return prefs != null ? prefs : Collections.emptyList();
    }

    public void saveProfileImageUri(android.net.Uri uri) {
        sessionManager.saveProfilePhotoUri(uri.toString());
    }

    public android.net.Uri getSavedProfileImageUri() {
        String raw = sessionManager.getProfilePhotoUri();
        return raw != null ? android.net.Uri.parse(raw) : null;
    }

    public void loadAll() {
        final int[] pending = {5};
        _loading.setValue(true);

        profileRepository.getProfile(new RepositoryCallback<UserProfileData>() {
            @Override public void onSuccess(UserProfileData data) {
                _profile.setValue(data);
                if (--pending[0] <= 0) _loading.setValue(false);
            }
            @Override public void onError(UiMessage error) {
                _error.setValue(error);
                if (--pending[0] <= 0) _loading.setValue(false);
            }
        });

        profileRepository.getPreferences(new RepositoryCallback<List<String>>() {
            @Override public void onSuccess(List<String> data) {
                _preferences.setValue(data);
                if (--pending[0] <= 0) _loading.setValue(false);
            }
            @Override public void onError(UiMessage error) {
                if (--pending[0] <= 0) _loading.setValue(false);
            }
        });

        tourRepository.getCategories(new RepositoryCallback<List<String>>() {
            @Override public void onSuccess(List<String> data) {
                _categories.setValue(data);
                if (--pending[0] <= 0) _loading.setValue(false);
            }
            @Override public void onError(UiMessage error) {
                _categories.setValue(Collections.emptyList());
                if (--pending[0] <= 0) _loading.setValue(false);
            }
        });

        profileRepository.getActivitySummary(new RepositoryCallback<List<BookingSummaryItem>>() {
            @Override public void onSuccess(List<BookingSummaryItem> data) {
                List<BookingSummaryItem> list = data != null ? data : Collections.emptyList();
                _historialCount.setValue(list.size());
                _recentActivities.setValue(list.size() > 2 ? new ArrayList<>(list.subList(0, 2)) : new ArrayList<>(list));
                if (--pending[0] <= 0) _loading.setValue(false);
            }
            @Override public void onError(UiMessage error) {
                _historialCount.setValue(0);
                _recentActivities.setValue(Collections.emptyList());
                if (--pending[0] <= 0) _loading.setValue(false);
            }
        });

        bookingRepository.listMyBookings("CONFIRMED", new RepositoryCallback<List<BookingResponse>>() {
            @Override public void onSuccess(List<BookingResponse> data) {
                _pendingCount.setValue(data != null ? data.size() : 0);
                if (--pending[0] <= 0) _loading.setValue(false);
            }
            @Override public void onError(UiMessage error) {
                _pendingCount.setValue(0);
                if (--pending[0] <= 0) _loading.setValue(false);
            }
        });
    }

    public void saveAll(String firstName, String lastName, String phone,
                        List<String> selectedCategories) {
        _loading.setValue(true);
        _saveSuccess.setValue(false);

        if (!isOnline()) {
            sessionManager.savePendingProfile(firstName, lastName, phone, null, selectedCategories);
            _loading.setValue(false);
            _saveSuccess.setValue(true);
            return;
        }

        uploadProfileOnline(firstName, lastName, phone, selectedCategories);
    }

    private void uploadProfileOnline(String firstName, String lastName, String phone,
                                     List<String> selectedCategories) {
        final int[] pending = {2};
        final boolean[] hasError = {false};

        profileRepository.updateProfile(firstName, lastName, phone,
                new RepositoryCallback<UserProfileData>() {
                    @Override public void onSuccess(UserProfileData data) {
                        _profile.setValue(data);
                        if (--pending[0] <= 0) onSaveDone(hasError[0]);
                    }
                    @Override public void onError(UiMessage error) {
                        hasError[0] = true;
                        _error.setValue(error);
                        if (--pending[0] <= 0) onSaveDone(true);
                    }
                });

        profileRepository.updatePreferences(selectedCategories,
                new RepositoryCallback<List<String>>() {
                    @Override public void onSuccess(List<String> data) {
                        _preferences.setValue(data);
                        if (--pending[0] <= 0) onSaveDone(hasError[0]);
                    }
                    @Override public void onError(UiMessage error) {
                        hasError[0] = true;
                        _error.setValue(error);
                        if (--pending[0] <= 0) onSaveDone(true);
                    }
                });
    }

    private void trySyncPendingProfile() {
        if (!sessionManager.hasPendingProfile() || !isOnline()) return;

        String firstName   = sessionManager.getPendingFirstName();
        String lastName    = sessionManager.getPendingLastName();
        String phone       = sessionManager.getPendingPhone();
        List<String> categories = sessionManager.getPendingCategories();

        final int[] pending = {2};

        profileRepository.updateProfile(firstName, lastName, phone,
                new RepositoryCallback<UserProfileData>() {
                    @Override public void onSuccess(UserProfileData data) {
                        _profile.setValue(data);
                        if (--pending[0] <= 0) sessionManager.clearPendingProfile();
                    }
                    @Override public void onError(UiMessage ignored) {
                        if (--pending[0] <= 0) { /* mantener pendiente */ }
                    }
                });

        profileRepository.updatePreferences(categories,
                new RepositoryCallback<List<String>>() {
                    @Override public void onSuccess(List<String> data) {
                        _preferences.setValue(data);
                        if (--pending[0] <= 0) sessionManager.clearPendingProfile();
                    }
                    @Override public void onError(UiMessage ignored) {
                        if (--pending[0] <= 0) { /* mantener pendiente */ }
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

    public void errorConsumed() { _error.setValue(null); }
    public void saveSuccessConsumed() { _saveSuccess.setValue(false); }

    private void onSaveDone(boolean hadError) {
        _loading.setValue(false);
        if (!hadError) _saveSuccess.setValue(true);
    }

    @Override
    protected void onCleared() {
        profileRepository.cancelAll();
        super.onCleared();
    }
}
