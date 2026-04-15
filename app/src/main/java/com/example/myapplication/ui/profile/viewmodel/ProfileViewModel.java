package com.example.myapplication.ui.profile.viewmodel;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.data.model.UserProfileData;
import com.example.myapplication.data.repository.ProfileRepository;
import com.example.myapplication.data.session.SessionManager;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final ProfileRepository profileRepository;
    private final SessionManager sessionManager;

    private final MutableLiveData<UserProfileData> _profile = new MutableLiveData<>();
    private final MutableLiveData<List<String>> _preferences = new MutableLiveData<>();
    private final MutableLiveData<List<BookingSummaryItem>> _activitySummary = new MutableLiveData<>();
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _saveSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<Uri> _selectedPhotoUri = new MutableLiveData<>();

    @Inject
    public ProfileViewModel(ProfileRepository profileRepository, SessionManager sessionManager) {
        this.profileRepository = profileRepository;
        this.sessionManager = sessionManager;
        String savedUri = sessionManager.getProfilePhotoUri();
        if (savedUri != null) _selectedPhotoUri.setValue(Uri.parse(savedUri));
        loadAll();
    }

    public LiveData<UserProfileData> getProfile() { return _profile; }
    public LiveData<List<String>> getPreferences() { return _preferences; }
    public LiveData<List<BookingSummaryItem>> getActivitySummary() { return _activitySummary; }
    public LiveData<UiMessage> getError() { return _error; }
    public LiveData<Boolean> isLoading() { return _loading; }
    public LiveData<Boolean> isSaveSuccess() { return _saveSuccess; }
    public LiveData<Uri> getSelectedPhotoUri() { return _selectedPhotoUri; }

    public void setSelectedPhotoUri(Uri uri) {
        _selectedPhotoUri.setValue(uri);
        sessionManager.saveProfilePhotoUri(uri.toString());
    }

    public void loadAll() {
        final int[] pending = {3};
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
                _error.setValue(error);
                if (--pending[0] <= 0) _loading.setValue(false);
            }
        });

        profileRepository.getActivitySummary(new RepositoryCallback<List<BookingSummaryItem>>() {
            @Override public void onSuccess(List<BookingSummaryItem> data) {
                _activitySummary.setValue(data);
                if (--pending[0] <= 0) _loading.setValue(false);
            }
            @Override public void onError(UiMessage error) {
                // Summary es best-effort: falla silenciosamente con lista vacía
                _activitySummary.setValue(Collections.emptyList());
                if (--pending[0] <= 0) _loading.setValue(false);
            }
        });
    }

    public void saveAll(String firstName, String lastName, String phone,
                        String profilePhotoUrl, List<String> selectedCategories) {
        // La URI local (content://) es solo para display — el backend espera una URL HTTP.
        // Se mantiene el profilePhotoUrl del backend sin cambios.
        String effectivePhotoUrl = profilePhotoUrl;
        final int[] pending = {2};
        final boolean[] hasError = {false};
        _loading.setValue(true);
        _saveSuccess.setValue(false);

        profileRepository.updateProfile(firstName, lastName, phone, effectivePhotoUrl,
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
