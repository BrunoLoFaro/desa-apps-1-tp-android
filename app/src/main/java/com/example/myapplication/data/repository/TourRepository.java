package com.example.myapplication.data.repository;

import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.local.CachedActivityDao;
import com.example.myapplication.data.local.CachedActivityEntity;
import com.example.myapplication.data.model.ActivitiesPageResponse;
import com.example.myapplication.data.model.ActivityDetailResponse;
import com.example.myapplication.data.model.ActivitySummaryResponse;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.network.ActivityService;
import com.example.myapplication.util.FormatUtils;
import com.example.myapplication.util.NetworkErrorParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Response;

@Singleton
public class TourRepository extends BaseRepository {

    private final ActivityService activityService;
    private final ConfigLoader configLoader;
    private final CachedActivityDao cachedActivityDao;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    @Inject
    public TourRepository(ActivityService activityService, ConfigLoader configLoader,
                          NetworkErrorParser errorParser, CachedActivityDao cachedActivityDao) {
        super(errorParser);
        this.activityService = activityService;
        this.configLoader = configLoader;
        this.cachedActivityDao = cachedActivityDao;
    }

    public void getFeaturedTours(RepositoryCallback<List<TourActivity>> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueuePage(activityService.listFeatured(config.activitiesFeaturedEndpoint), callback, R.string.error_load_featured);
    }

    public void getRecommendedTours(RepositoryCallback<List<TourActivity>> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueuePage(activityService.listActivities(config.activitiesRecommendedEndpoint), callback, R.string.error_load_recommended);
    }

    public void getAllTours(RepositoryCallback<List<TourActivity>> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueuePage(activityService.listActivities(config.activitiesEndpoint), callback, R.string.error_load_activities);
    }

    public void getActivityDetail(long activityId, RepositoryCallback<ActivityDetailResponse> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        String endpoint = config.activitiesEndpoint + "/" + activityId;
        enqueueDetail(activityService.getActivityDetail(endpoint), callback, R.string.error_load_activities);
    }

    public void getFavorites(RepositoryCallback<List<TourActivity>> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueue(activityService.getFavorites(), new RepositoryCallback<List<ActivitySummaryResponse>>() {
            @Override
            public void onSuccess(List<ActivitySummaryResponse> data) {
                android.util.Log.d("TourRepository", "getFavorites raw count: " + (data != null ? data.size() : "null"));
                if (data != null && !data.isEmpty()) {
                    ActivitySummaryResponse first = data.get(0);
                    android.util.Log.d("TourRepository", "first item → id=" + first.id
                            + " name=" + first.name
                            + " imageUrl=" + first.imageUrl
                            + " destination=" + (first.destination != null ? first.destination.name : "null"));
                }
                callback.onSuccess(ExploreRepository.mapToTourActivities(data));
            }

            @Override
            public void onError(UiMessage error) {
                android.util.Log.e("TourRepository", "getFavorites error: " + error);
                callback.onError(error);
            }
        }, R.string.error_load_favorites);
    }

    public void addFavorite(long activityId, RepositoryCallback<Void> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueueVoid(activityService.addFavorite(activityId), callback, R.string.error_add_favorite);
    }

    public void removeFavorite(long activityId, RepositoryCallback<Void> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueueVoid(activityService.removeFavorite(activityId), callback, R.string.error_remove_favorite);
    }

    public void toggleFavorite(long activityId, boolean targetFavorite, RepositoryCallback<Void> callback) {
        if (targetFavorite) {
            addFavorite(activityId, callback);
        } else {
            removeFavorite(activityId, callback);
        }
    }

    public void getCategories(RepositoryCallback<List<String>> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueue(activityService.getCategories(config.categoriesEndpoint), callback, R.string.error_load_activities);
    }

    public void getCachedActivity(long activityId, RepositoryCallback<TourActivity> callback) {
        dbExecutor.execute(() -> {
            CachedActivityEntity entity = cachedActivityDao.getById(activityId);
            if (entity != null) {
                callback.onSuccess(mapFromCache(entity));
            } else {
                callback.onError(UiMessage.from(R.string.error_load_activities));
            }
        });
    }

    public void getCachedActivities(RepositoryCallback<List<TourActivity>> callback) {
        dbExecutor.execute(() -> {
            List<CachedActivityEntity> entities = cachedActivityDao.getAll();
            if (entities != null && !entities.isEmpty()) {
                List<TourActivity> result = new ArrayList<>(entities.size());
                for (CachedActivityEntity e : entities) result.add(mapFromCache(e));
                callback.onSuccess(result);
            } else {
                callback.onError(UiMessage.from(R.string.error_load_activities));
            }
        });
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private void enqueuePage(Call<ActivitiesPageResponse> call,
                             RepositoryCallback<List<TourActivity>> callback,
                             int fallbackErrorResId) {
        activeCalls.add(call);
        call.enqueue(new retrofit2.Callback<ActivitiesPageResponse>() {
            @Override
            public void onResponse(Call<ActivitiesPageResponse> c, Response<ActivitiesPageResponse> response) {
                activeCalls.remove(c);
                if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                    List<ActivitySummaryResponse> items = response.body().items;
                    cacheActivitiesAsync(items);
                    callback.onSuccess(ExploreRepository.mapToTourActivities(items));
                } else {
                    callback.onError(errorParser.getErrorMessage(response, fallbackErrorResId));
                }
            }

            @Override
            public void onFailure(Call<ActivitiesPageResponse> c, Throwable t) {
                activeCalls.remove(c);
                callback.onError(errorParser.getFailureMessage(t, R.string.error_network_generic));
            }
        });
    }

    private void enqueueDetail(Call<ActivityDetailResponse> call,
                               RepositoryCallback<ActivityDetailResponse> callback,
                               int fallbackErrorResId) {
        activeCalls.add(call);
        call.enqueue(new retrofit2.Callback<ActivityDetailResponse>() {
            @Override
            public void onResponse(Call<ActivityDetailResponse> c, Response<ActivityDetailResponse> response) {
                activeCalls.remove(c);
                if (response.isSuccessful() && response.body() != null) {
                    ActivityDetailResponse data = response.body();
                    cacheDetailAsync(data);
                    callback.onSuccess(data);
                } else {
                    callback.onError(errorParser.getErrorMessage(response, fallbackErrorResId));
                }
            }

            @Override
            public void onFailure(Call<ActivityDetailResponse> c, Throwable t) {
                activeCalls.remove(c);
                callback.onError(errorParser.getFailureMessage(t, R.string.error_network_generic));
            }
        });
    }

    private void cacheActivitiesAsync(List<ActivitySummaryResponse> items) {
        if (items == null || items.isEmpty()) return;
        dbExecutor.execute(() -> {
            List<CachedActivityEntity> entities = new ArrayList<>(items.size());
            for (ActivitySummaryResponse item : items) {
                if (item.id != null) entities.add(fromSummary(item));
            }
            if (!entities.isEmpty()) cachedActivityDao.upsertAll(entities);
        });
    }

    private void cacheDetailAsync(ActivityDetailResponse data) {
        if (data == null || data.id == null) return;
        dbExecutor.execute(() -> cachedActivityDao.upsert(fromDetail(data)));
    }

    // ── Conversions ──────────────────────────────────────────────────────────

    static CachedActivityEntity fromSummary(ActivitySummaryResponse s) {
        CachedActivityEntity e = new CachedActivityEntity();
        e.id = s.id;
        e.name = s.name;
        e.destinationName = s.destination != null ? s.destination.name : null;
        e.category = s.category;
        e.imageUrl = s.imageUrl;
        e.durationMinutes = s.durationMinutes;
        e.basePrice = s.price;
        e.currency = s.currency;
        e.availableSpots = s.availableSpots;
        e.avgRating = s.avgRating != null ? s.avgRating.floatValue() : 0f;
        e.reviewCount = s.reviewCount != null ? s.reviewCount.intValue() : 0;
        return e;
    }

    static CachedActivityEntity fromDetail(ActivityDetailResponse d) {
        CachedActivityEntity e = new CachedActivityEntity();
        e.id = d.id;
        e.name = d.name;
        e.destinationName = d.destination != null ? d.destination.name : null;
        e.category = d.category;
        e.imageUrl = d.imageUrl;
        e.durationMinutes = d.durationMinutes;
        e.basePrice = d.basePrice;
        e.currency = d.currency;
        e.availableSpots = d.availableSpots;
        e.avgRating = d.avgRating != null ? d.avgRating.floatValue() : 0f;
        e.reviewCount = d.reviewCount != null ? d.reviewCount.intValue() : 0;
        e.description = d.description;
        e.guideName = d.guide != null ? d.guide.fullName : null;
        e.meetingPoint = d.meetingPoint;
        e.language = d.language;
        e.includesText = d.includesText;
        e.cancellationPolicy = d.cancellationPolicy;
        return e;
    }

    public static TourActivity mapFromCache(CachedActivityEntity e) {
        String category = e.category != null ? e.category.replace("_", " ") : "";
        String duration = FormatUtils.formatDuration(e.durationMinutes);
        String price = FormatUtils.formatPrice(e.basePrice, e.currency);
        TourActivity a = new TourActivity(
                e.name != null ? e.name : "",
                e.destinationName != null ? e.destinationName : "",
                category,
                duration,
                price,
                e.availableSpots,
                e.imageUrl,
                e.description != null ? e.description : "",
                e.avgRating,
                e.reviewCount,
                 e.includesText != null ? e.includesText : "",
                 e.meetingPoint != null ? e.meetingPoint : "",
                 e.guideName != null ? e.guideName : "",
                 e.language != null ? e.language : "",
                 e.cancellationPolicy != null ? e.cancellationPolicy : "",
                 false
         );
        a.setId(e.id);
        return a;
    }

    private <T> AppConfig getConfig(RepositoryCallback<T> callback) {
        AppConfig config = configLoader.loadConfig();
        if (config == null || !config.hasValidBaseUrl()) {
            if (callback != null) callback.onError(UiMessage.from(R.string.error_invalid_config));
            return null;
        }
        return config;
    }
}
