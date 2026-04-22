package com.example.myapplication.ui.explore.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.ActivitiesPageResponse;
import com.example.myapplication.data.model.DestinationResponse;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.repository.ExploreRepository;
import com.example.myapplication.data.repository.TourRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public class ExploreViewModel extends ViewModel {

    public interface FavoriteToggleCallback {
        void onCompleted(boolean success, UiMessage error);
    }

    private final ExploreRepository exploreRepository;
    private final TourRepository tourRepository;

    private final MutableLiveData<List<TourActivity>> _activities =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<DestinationResponse>> _destinations =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<String>> _categories =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);

    private int currentPage = 0;
    private int totalPages = 1;
    private int pageSize = ExploreRepository.DEFAULT_PAGE_SIZE;

    private Long destinationId = null;
    private String category = null;
    private String dateIso = null;
    private String minPrice = null;
    private String maxPrice = null;

    @Inject
    public ExploreViewModel(ExploreRepository exploreRepository, TourRepository tourRepository) {
        this.exploreRepository = exploreRepository;
        this.tourRepository = tourRepository;
    }

    public LiveData<List<TourActivity>> getActivities() { return _activities; }
    public LiveData<List<DestinationResponse>> getDestinations() { return _destinations; }
    public LiveData<List<String>> getCategories() { return _categories; }
    public LiveData<UiMessage> getError() { return _error; }
    public LiveData<Boolean> isLoading() { return _loading; }

    public void loadMeta() {
        exploreRepository.listDestinations(new RepositoryCallback<List<DestinationResponse>>() {
            @Override
            public void onSuccess(List<DestinationResponse> data) {
                _destinations.setValue(data != null ? data : Collections.emptyList());
            }

            @Override
            public void onError(UiMessage error) {
                _error.setValue(error);
            }
        });

        exploreRepository.listCategories(new RepositoryCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> data) {
                _categories.setValue(data != null ? data : Collections.emptyList());
            }

            @Override
            public void onError(UiMessage error) {
                _error.setValue(error);
            }
        });
    }

    public void applyFilters(Long destinationId, String category, String dateIso, String minPrice, String maxPrice) {
        this.destinationId = destinationId;
        this.category = category;
        this.dateIso = dateIso;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        currentPage = 0;
        totalPages = 1;
        _activities.setValue(Collections.emptyList());
        loadPage(0, true);
    }

    public void loadFirstPage() {
        applyFilters(destinationId, category, dateIso, minPrice, maxPrice);
    }

    public void loadNextPageIfAvailable() {
        if (Boolean.TRUE.equals(_loading.getValue())) return;
        if (currentPage + 1 >= totalPages) return;
        loadPage(currentPage + 1, false);
    }

    public void toggleFavorite(long activityId, boolean targetFavorite, FavoriteToggleCallback callback) {
        applyFavoriteLocally(activityId, targetFavorite);
        tourRepository.toggleFavorite(activityId, targetFavorite, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                if (callback != null) callback.onCompleted(true, null);
            }

            @Override
            public void onError(UiMessage error) {
                applyFavoriteLocally(activityId, !targetFavorite);
                _error.setValue(error);
                if (callback != null) callback.onCompleted(false, error);
            }
        });
    }

    private void loadPage(int page, boolean replace) {
        _loading.setValue(true);
        exploreRepository.listActivities(page, pageSize, destinationId, category, dateIso, minPrice, maxPrice,
                new RepositoryCallback<ActivitiesPageResponse>() {
                    @Override
                    public void onSuccess(ActivitiesPageResponse data) {
                        _loading.setValue(false);
                        currentPage = data != null ? data.page : page;
                        totalPages = data != null ? Math.max(1, data.totalPages) : 1;

                        List<TourActivity> mapped = data != null
                                ? ExploreRepository.mapToTourActivities(data.items)
                                : Collections.emptyList();

                        if (replace) {
                            _activities.setValue(mapped);
                        } else {
                            List<TourActivity> current = _activities.getValue();
                            List<TourActivity> merged = new ArrayList<>(current != null ? current : Collections.emptyList());
                            merged.addAll(mapped);
                            _activities.setValue(merged);
                        }
                    }

                    @Override
                    public void onError(UiMessage error) {
                        _loading.setValue(false);
                        _error.setValue(error);
                    }
                });
    }

    @Override
    protected void onCleared() {
        exploreRepository.cancelAll();
        tourRepository.cancelAll();
        super.onCleared();
    }

    private void applyFavoriteLocally(long activityId, boolean targetFavorite) {
        List<TourActivity> current = _activities.getValue();
        if (current == null || current.isEmpty()) return;
        for (TourActivity item : current) {
            if (item.getId() != null && item.getId() == activityId) {
                item.setFavorite(targetFavorite);
            }
        }
        _activities.setValue(current);
    }
}

