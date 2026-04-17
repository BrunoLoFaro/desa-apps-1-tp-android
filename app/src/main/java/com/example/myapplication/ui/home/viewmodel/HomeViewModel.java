package com.example.myapplication.ui.home.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.local.ProfileImageManager;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.repository.SessionRepository;
import com.example.myapplication.data.repository.TourRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final SessionRepository sessionRepository;
    private final TourRepository tourRepository;
    private final ProfileImageManager profileImageManager;

    private final MutableLiveData<List<TourActivity>> _featuredTours = new MutableLiveData<>();
    private final MutableLiveData<List<TourActivity>> _allTours = new MutableLiveData<>();
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(true);
    private int pendingCalls = 0;

    @Inject
    public HomeViewModel(SessionRepository sessionRepository, TourRepository tourRepository,
                         ProfileImageManager profileImageManager) {
        this.sessionRepository = sessionRepository;
        this.tourRepository = tourRepository;
        this.profileImageManager = profileImageManager;
        loadTours();
    }

    public LiveData<List<TourActivity>> getFeaturedTours() { return _featuredTours; }
    public LiveData<List<TourActivity>> getAllTours() { return _allTours; }
    public LiveData<UiMessage> getError() { return _error; }
    public LiveData<Boolean> isLoading() { return _loading; }

    public boolean hasValidSession() {
        return sessionRepository.hasValidSession();
    }

    public void logout() {
        profileImageManager.delete(sessionRepository.getUserId());
        sessionRepository.clearSession();
    }

    public void reloadRecommended() {
        tourRepository.getRecommendedTours(new RepositoryCallback<List<TourActivity>>() {
            @Override
            public void onSuccess(List<TourActivity> data) {
                _featuredTours.setValue(data);
            }
            @Override
            public void onError(UiMessage error) {
                // best-effort: silent on background refresh
            }
        });
    }

    private void loadTours() {
        pendingCalls = 2;
        _loading.setValue(true);

        tourRepository.getRecommendedTours(new RepositoryCallback<List<TourActivity>>() {
            @Override
            public void onSuccess(List<TourActivity> data) {
                _featuredTours.setValue(data);
                onCallFinished();
            }

            @Override
            public void onError(UiMessage error) {
                _error.setValue(error);
                onCallFinished();
            }
        });

        tourRepository.getAllTours(new RepositoryCallback<List<TourActivity>>() {
            @Override
            public void onSuccess(List<TourActivity> data) {
                _allTours.setValue(data);
                onCallFinished();
            }

            @Override
            public void onError(UiMessage error) {
                _error.setValue(error);
                onCallFinished();
            }
        });
    }

    private void onCallFinished() {
        pendingCalls--;
        if (pendingCalls <= 0) {
            _loading.setValue(false);
        }
    }

    @Override
    protected void onCleared() {
        tourRepository.cancelAll();
        super.onCleared();
    }
}
