package com.example.myapplication.data.repository;

import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.model.ActivitiesPageResponse;
import com.example.myapplication.data.model.ActivityDetailResponse;
import com.example.myapplication.data.model.ActivitySummaryResponse;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.network.ActivityService;
import com.example.myapplication.util.NetworkErrorParser;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Response;

@Singleton
public class TourRepository extends BaseRepository {

    private final ActivityService activityService;
    private final ConfigLoader configLoader;

    @Inject
    public TourRepository(ActivityService activityService, ConfigLoader configLoader,
                          NetworkErrorParser errorParser) {
        super(errorParser);
        this.activityService = activityService;
        this.configLoader = configLoader;
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
                callback.onSuccess(ExploreRepository.mapToTourActivities(data));
            }

            @Override
            public void onError(UiMessage error) {
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

    private void enqueuePage(Call<ActivitiesPageResponse> call,
                             RepositoryCallback<List<TourActivity>> callback,
                             int fallbackErrorResId) {
        activeCalls.add(call);
        call.enqueue(new retrofit2.Callback<ActivitiesPageResponse>() {
            @Override
            public void onResponse(Call<ActivitiesPageResponse> c, Response<ActivitiesPageResponse> response) {
                activeCalls.remove(c);
                if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                    callback.onSuccess(ExploreRepository.mapToTourActivities(response.body().items));
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
                    callback.onSuccess(response.body());
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

    private <T> AppConfig getConfig(RepositoryCallback<T> callback) {
        AppConfig config = configLoader.loadConfig();
        if (config == null || !config.hasValidBaseUrl()) {
            callback.onError(UiMessage.from(R.string.error_invalid_config));
            return null;
        }
        return config;
    }
}
