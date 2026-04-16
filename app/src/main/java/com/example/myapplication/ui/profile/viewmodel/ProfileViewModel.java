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
import com.example.myapplication.data.local.ProfileImageManager;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.data.model.UserProfileData;
import com.example.myapplication.data.repository.ProfileRepository;
import com.example.myapplication.data.repository.TourRepository;
import com.example.myapplication.data.session.SessionManager;
import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Inject;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final ProfileRepository profileRepository;
    private final TourRepository tourRepository;
    private final SessionManager sessionManager;
    private final ProfileImageManager profileImageManager;
    private final Context context;
    private final Executor ioExecutor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<UserProfileData> _profile = new MutableLiveData<>();
    private final MutableLiveData<List<String>> _preferences = new MutableLiveData<>();
    private final MutableLiveData<List<String>> _categories = new MutableLiveData<>();
    private final MutableLiveData<List<BookingSummaryItem>> _activitySummary = new MutableLiveData<>();
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _saveSuccess = new MutableLiveData<>(false);
    // URI válida solo durante la sesión actual (content://). No se persiste entre reinicios.
    private final MutableLiveData<Uri> _selectedPhotoUri = new MutableLiveData<>();

    @Inject
    public ProfileViewModel(ProfileRepository profileRepository, TourRepository tourRepository,
                            SessionManager sessionManager,
                            ProfileImageManager profileImageManager,
                            @ApplicationContext Context context) {
        this.profileRepository = profileRepository;
        this.tourRepository = tourRepository;
        this.sessionManager = sessionManager;
        this.profileImageManager = profileImageManager;
        this.context = context;
        trySyncPendingProfile();
        loadAll();
    }

    public LiveData<UserProfileData> getProfile() { return _profile; }
    public LiveData<List<String>> getPreferences() { return _preferences; }
    public LiveData<List<String>> getCategories() { return _categories; }
    public LiveData<List<BookingSummaryItem>> getActivitySummary() { return _activitySummary; }
    public LiveData<UiMessage> getError() { return _error; }
    public LiveData<Boolean> isLoading() { return _loading; }
    public LiveData<Boolean> isSaveSuccess() { return _saveSuccess; }
    public LiveData<Uri> getSelectedPhotoUri() { return _selectedPhotoUri; }

    /**
     * Devuelve el archivo local de la imagen de perfil.
     * Puede no existir — verificar con File.exists() antes de cargar.
     */
    public File getLocalProfileImage() {
        return profileImageManager.getLocalFile(sessionManager.getUserId());
    }

    /**
     * Llamar cuando el usuario selecciona una nueva imagen desde la galería.
     * Actualiza la UI inmediatamente (via LiveData) y persiste en almacenamiento interno.
     */
    public void setSelectedPhotoUri(Uri uri) {
        _selectedPhotoUri.setValue(uri);
        // Persist to internal storage off the main thread
        ioExecutor.execute(() -> {
            try {
                profileImageManager.saveFromUri(
                        sessionManager.getUserId(), uri, context.getContentResolver());
            } catch (Exception ignored) { }
        });
    }

    public void loadAll() {
        final int[] pending = {4};
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
                // Summary es best-effort: falla silenciosamente
                _activitySummary.setValue(Collections.emptyList());
                if (--pending[0] <= 0) _loading.setValue(false);
            }
        });

        tourRepository.getCategories(new RepositoryCallback<List<String>>() {
            @Override public void onSuccess(List<String> data) {
                _categories.setValue(data);
                if (--pending[0] <= 0) _loading.setValue(false);
            }
            @Override public void onError(UiMessage error) {
                // Categorías best-effort: si falla, la UI queda sin chips pero no bloquea el perfil
                _categories.setValue(Collections.emptyList());
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

    /** Sincroniza el perfil pendiente si hay conexión y datos guardados offline. */
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
