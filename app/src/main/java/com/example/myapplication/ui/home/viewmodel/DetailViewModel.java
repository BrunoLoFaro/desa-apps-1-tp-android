package com.example.myapplication.ui.home.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.local.CachedActivityDao;
import com.example.myapplication.data.local.CachedActivityEntity;
import com.example.myapplication.data.model.ActivityDetailResponse;
import com.example.myapplication.data.model.ActivitySessionResponse;
import com.example.myapplication.data.model.ItineraryPoint;
import com.example.myapplication.data.model.ItineraryPointResponse;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.repository.TourRepository;
import com.example.myapplication.util.FormatUtils;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Inject;

@HiltViewModel
public class DetailViewModel extends ViewModel {

    public interface FavoriteToggleCallback {
        void onCompleted(boolean success, UiMessage error);
    }

    private final TourRepository tourRepository;
    private final CachedActivityDao cachedActivityDao;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<TourActivity> _activity = new MutableLiveData<>();
    private final MutableLiveData<List<ActivitySessionResponse>> _sessions =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _offlineCacheMiss = new MutableLiveData<>();

    @Inject
    public DetailViewModel(TourRepository tourRepository, CachedActivityDao cachedActivityDao) {
        this.tourRepository = tourRepository;
        this.cachedActivityDao = cachedActivityDao;
    }

    public LiveData<TourActivity> getActivity() { return _activity; }
    public LiveData<List<ActivitySessionResponse>> getSessions() { return _sessions; }
    public LiveData<UiMessage> getError() { return _error; }
    public LiveData<Boolean> isLoading() { return _loading; }
    public LiveData<Boolean> isOfflineCacheMiss() { return _offlineCacheMiss; }

    public void toggleFavorite(long activityId, boolean targetFavorite, FavoriteToggleCallback callback) {
        tourRepository.toggleFavorite(activityId, targetFavorite, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                TourActivity current = _activity.getValue();
                if (current != null && current.getId() != null && current.getId() == activityId) {
                    current.setFavorite(targetFavorite);
                    _activity.setValue(current);
                }
                if (callback != null) callback.onCompleted(true, null);
            }

            @Override
            public void onError(UiMessage error) {
                _error.setValue(error != null ? error : UiMessage.from(R.string.error_network_generic));
                if (callback != null) callback.onCompleted(false, error);
            }
        });
    }

    public void loadFromCache(long activityId) {
        _offlineCacheMiss.setValue(null);
        dbExecutor.execute(() -> {
            CachedActivityEntity entity = cachedActivityDao.getById(activityId);
            if (entity != null) {
                _activity.postValue(TourRepository.mapFromCache(entity));
            } else {
                _offlineCacheMiss.postValue(true);
            }
        });
    }

    public void load(long activityId) {
        _offlineCacheMiss.setValue(null);
        _loading.setValue(true);
        tourRepository.getActivityDetail(activityId, new RepositoryCallback<ActivityDetailResponse>() {
            @Override
            public void onSuccess(ActivityDetailResponse data) {
                _loading.setValue(false);
                _sessions.setValue(data.sessions != null ? data.sessions : Collections.emptyList());
                _activity.setValue(mapToTourActivity(data));
            }

            @Override
            public void onError(UiMessage error) {
                _loading.setValue(false);
                _error.setValue(error != null ? error : UiMessage.from(R.string.error_network_generic));
            }
        });
    }

    @Override
    protected void onCleared() {
        tourRepository.cancelAll();
        super.onCleared();
    }

    private static TourActivity mapToTourActivity(ActivityDetailResponse data) {
        String destination = data.destination != null ? safe(data.destination.name) : "";
        String category = data.category != null ? data.category.replace("_", " ") : "";
        String duration = FormatUtils.formatDuration(data.durationMinutes);
        String price = FormatUtils.formatPrice(data.basePrice, data.currency);
        String guideName = data.guide != null ? safe(data.guide.fullName) : null;
        float rating = data.avgRating != null ? data.avgRating.floatValue() : 0f;
        int reviewCount = data.reviewCount != null ? data.reviewCount.intValue() : 0;

        // Fill "detail" fields the current UI already has.
        TourActivity activity = new TourActivity(
                safe(data.name),
                destination,
                category,
                duration,
                price,
                data.availableSpots,
                data.imageUrl,
                safe(data.description),
                rating,
                reviewCount,
                safe(data.includesText),
                safe(data.meetingPoint),
                guideName,
                safe(data.language),
                safe(data.cancellationPolicy)
        );
        activity.setId(data.id);
        activity.setFavorite(data.isFavorite);
        activity.setItineraryPoints(mapItineraryPoints(data.itineraryPoints));
        if (data.discountPercentage != null) activity.setDiscountPercentage(data.discountPercentage);
        if (data.galleryUrls != null && !data.galleryUrls.isEmpty()) {
            List<String> sanitized = new ArrayList<>();
            for (String url : data.galleryUrls) {
                if (url == null) continue;
                String trimmed = url.trim();
                if (trimmed.isEmpty()) continue;
                if (!sanitized.contains(trimmed)) sanitized.add(trimmed);
            }
            if (!sanitized.isEmpty()) activity.setGalleryUrls(sanitized);
        }
        return activity;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static List<ItineraryPoint> mapItineraryPoints(List<ItineraryPointResponse> points) {
        if (points == null || points.isEmpty()) return Collections.emptyList();
        List<ItineraryPointResponse> sorted = new ArrayList<>(points);
        sorted.sort(Comparator.comparingInt(p -> p.position != null ? p.position : 0));

        List<ItineraryPoint> result = new ArrayList<>();
        for (ItineraryPointResponse p : sorted) {
            String name = p.name != null ? p.name : "";
            String address = p.address != null ? p.address : "";
            int position = p.position != null ? p.position : 0;
            result.add(new ItineraryPoint(name, address, position));
        }
        return result;
    }

}
