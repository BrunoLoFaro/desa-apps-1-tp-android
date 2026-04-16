package com.example.myapplication.ui.profile.viewmodel;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
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
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final ProfileRepository profileRepository;
    private final SessionManager sessionManager;
    private final Context context;

    private final MutableLiveData<UserProfileData> _profile = new MutableLiveData<>();
    private final MutableLiveData<List<String>> _preferences = new MutableLiveData<>();
    private final MutableLiveData<List<BookingSummaryItem>> _activitySummary = new MutableLiveData<>();
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _saveSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<Uri> _selectedPhotoUri = new MutableLiveData<>();

    @Inject
    public ProfileViewModel(ProfileRepository profileRepository, SessionManager sessionManager,
                            @ApplicationContext Context context) {
        this.profileRepository = profileRepository;
        this.sessionManager = sessionManager;
        this.context = context;
        String savedUri = sessionManager.getProfilePhotoUri();
        if (savedUri != null) _selectedPhotoUri.setValue(Uri.parse(savedUri));
        trySyncPendingProfile();
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
                        List<String> selectedCategories) {
        _loading.setValue(true);
        _saveSuccess.setValue(false);

        if (!isOnline()) {
            // Sin conexión: guardar localmente y marcar como pendiente
            Uri photoUri = _selectedPhotoUri.getValue();
            String photoUriStr = photoUri != null ? photoUri.toString() : null;
            sessionManager.savePendingProfile(firstName, lastName, phone, photoUriStr, selectedCategories);
            _loading.setValue(false);
            _saveSuccess.setValue(true);
            return;
        }

        uploadProfileOnline(firstName, lastName, phone, selectedCategories);
    }

    private void uploadProfileOnline(String firstName, String lastName, String phone,
                                     List<String> selectedCategories) {
        Uri photoUri = _selectedPhotoUri.getValue();
        // Solo enviar la Uri si es una URI local (content://) — significa que el usuario seleccionó
        // una foto nueva. Si ya tiene URL de servidor, no volvemos a subirla.
        Uri uriToUpload = (photoUri != null && "content".equals(photoUri.getScheme())) ? photoUri : null;

        final int[] pending = {2};
        final boolean[] hasError = {false};

        profileRepository.updateProfile(firstName, lastName, phone, uriToUpload,
                new RepositoryCallback<UserProfileData>() {
                    @Override public void onSuccess(UserProfileData data) {
                        _profile.setValue(data);
                        // Si la imagen fue subida, ahora la URL viene del servidor
                        if (uriToUpload != null && data.getProfilePhotoUrl() != null) {
                            sessionManager.saveProfilePhotoUri(data.getProfilePhotoUrl());
                            _selectedPhotoUri.setValue(Uri.parse(data.getProfilePhotoUrl()));
                        }
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

    /** Intenta sincronizar el perfil pendiente si hay conexión y datos guardados offline. */
    private void trySyncPendingProfile() {
        if (!sessionManager.hasPendingProfile() || !isOnline()) return;

        String firstName  = sessionManager.getPendingFirstName();
        String lastName   = sessionManager.getPendingLastName();
        String phone      = sessionManager.getPendingPhone();
        String photoUriStr = sessionManager.getPendingPhotoUri();
        List<String> categories = sessionManager.getPendingCategories();

        if (photoUriStr != null && !photoUriStr.isEmpty()) {
            _selectedPhotoUri.setValue(Uri.parse(photoUriStr));
        }

        Uri uriToUpload = (photoUriStr != null && photoUriStr.startsWith("content://"))
                ? Uri.parse(photoUriStr) : null;

        final int[] pending = {2};
        profileRepository.updateProfile(firstName, lastName, phone, uriToUpload,
                new RepositoryCallback<UserProfileData>() {
                    @Override public void onSuccess(UserProfileData data) {
                        _profile.setValue(data);
                        if (uriToUpload != null && data.getProfilePhotoUrl() != null) {
                            sessionManager.saveProfilePhotoUri(data.getProfilePhotoUrl());
                            _selectedPhotoUri.postValue(Uri.parse(data.getProfilePhotoUrl()));
                        }
                        if (--pending[0] <= 0) sessionManager.clearPendingProfile();
                    }
                    @Override public void onError(UiMessage ignored) {
                        if (--pending[0] <= 0) { /* mantener pendiente para próximo intento */ }
                    }
                });

        profileRepository.updatePreferences(categories,
                new RepositoryCallback<List<String>>() {
                    @Override public void onSuccess(List<String> data) {
                        _preferences.setValue(data);
                        if (--pending[0] <= 0) sessionManager.clearPendingProfile();
                    }
                    @Override public void onError(UiMessage ignored) {
                        if (--pending[0] <= 0) { /* mantener pendiente para próximo intento */ }
                    }
                });
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
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
