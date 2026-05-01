package com.example.myapplication.ui.home.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.local.CachedActivityDao;
import com.example.myapplication.data.local.CachedActivityEntity;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.repository.SessionRepository;
import com.example.myapplication.data.repository.TourRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Inject;

@HiltViewModel
public class HomeViewModel extends ViewModel {

    public interface FavoriteToggleCallback {
        void onCompleted(boolean success, UiMessage error);
    }

    private final SessionRepository sessionRepository;
    private final TourRepository tourRepository;
    private final CachedActivityDao cachedActivityDao;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<TourActivity>> _featuredTours = new MutableLiveData<>();
    private final MutableLiveData<List<TourActivity>> _allTours = new MutableLiveData<>();
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(true);
    private int pendingCalls = 0;

    @Inject
    public HomeViewModel(SessionRepository sessionRepository, TourRepository tourRepository,
                         CachedActivityDao cachedActivityDao) {
        this.sessionRepository = sessionRepository;
        this.tourRepository = tourRepository;
        this.cachedActivityDao = cachedActivityDao;
        refreshTours();
    }

    public LiveData<List<TourActivity>> getFeaturedTours() { return _featuredTours; }
    public LiveData<List<TourActivity>> getAllTours() { return _allTours; }
    public LiveData<UiMessage> getError() { return _error; }
    public LiveData<Boolean> isLoading() { return _loading; }

    public boolean hasValidSession() {
        return sessionRepository.hasValidSession();
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

    public void refreshTours() {
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
                loadAllToursFromCache();
                onCallFinished();
            }
        });
    }

    private void loadAllToursFromCache() {
        dbExecutor.execute(() -> {
            List<CachedActivityEntity> cached = cachedActivityDao.getAll();
            if (cached != null && !cached.isEmpty()) {
                List<TourActivity> result = new ArrayList<>(cached.size());
                for (CachedActivityEntity e : cached) result.add(TourRepository.mapFromCache(e));
                _allTours.postValue(result);
            }
        });
    }

    public void toggleFavorite(long activityId, boolean targetFavorite, FavoriteToggleCallback callback) {
        applyFavoriteToLists(activityId, targetFavorite);
        tourRepository.toggleFavorite(activityId, targetFavorite, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                if (callback != null) callback.onCompleted(true, null);
            }

            @Override
            public void onError(UiMessage error) {
                applyFavoriteToLists(activityId, !targetFavorite);
                _error.setValue(error);
                if (callback != null) callback.onCompleted(false, error);
            }
        });
    }

    private void applyFavoriteToLists(long activityId, boolean favorite) {
        _featuredTours.setValue(applyFavorite(_featuredTours.getValue(), activityId, favorite));
        _allTours.setValue(applyFavorite(_allTours.getValue(), activityId, favorite));
    }

    private static List<TourActivity> applyFavorite(List<TourActivity> list, long activityId, boolean favorite) {
        if (list == null || list.isEmpty()) return list;
        for (TourActivity item : list) {
            if (item.getId() != null && item.getId() == activityId) {
                item.setFavorite(favorite);
            }
        }
        return list;
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
