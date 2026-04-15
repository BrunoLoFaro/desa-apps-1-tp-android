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
import com.example.myapplication.data.session.SessionManager;
import com.example.myapplication.util.NetworkErrorParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Response;

@Singleton
public class TourRepository {

    private final ActivityService activityService;
    private final ConfigLoader configLoader;
    private final SessionManager sessionManager;
    private final NetworkErrorParser errorParser;
    private final List<Call<?>> activeCalls = new CopyOnWriteArrayList<>();

    @Inject
    public TourRepository(ActivityService activityService, ConfigLoader configLoader,
                          SessionManager sessionManager, NetworkErrorParser errorParser) {
        this.activityService = activityService;
        this.configLoader = configLoader;
        this.sessionManager = sessionManager;
        this.errorParser = errorParser;
    }

    public void getFeaturedTours(RepositoryCallback<List<TourActivity>> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueuePage(activityService.listFeatured(config.activitiesFeaturedEndpoint), callback, R.string.error_load_featured);
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

    public void cancelAll() {
        for (Call<?> call : activeCalls) {
            if (!call.isCanceled()) call.cancel();
        }
        activeCalls.clear();
    }

    private void enqueuePage(Call<ActivitiesPageResponse> call, RepositoryCallback<List<TourActivity>> callback,
                             int fallbackErrorResId) {
        activeCalls.add(call);
        call.enqueue(new retrofit2.Callback<ActivitiesPageResponse>() {
            @Override
            public void onResponse(Call<ActivitiesPageResponse> c, Response<ActivitiesPageResponse> response) {
                activeCalls.remove(c);
                if (response.isSuccessful() && response.body() != null && response.body().items != null) {
                    callback.onSuccess(mapToTourActivities(response.body().items));
                } else {
                    if (response.code() == 401) {
                        sessionManager.clearSession();
                    }
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

    private void enqueueDetail(Call<ActivityDetailResponse> call, RepositoryCallback<ActivityDetailResponse> callback,
                               int fallbackErrorResId) {
        activeCalls.add(call);
        call.enqueue(new retrofit2.Callback<ActivityDetailResponse>() {
            @Override
            public void onResponse(Call<ActivityDetailResponse> c, Response<ActivityDetailResponse> response) {
                activeCalls.remove(c);
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    if (response.code() == 401) {
                        sessionManager.clearSession();
                    }
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

    private List<TourActivity> mapToTourActivities(List<ActivitySummaryResponse> items) {
        List<TourActivity> result = new ArrayList<>(items.size());
        for (ActivitySummaryResponse item : items) {
            String destination = item.destination != null ? item.destination.name : "";
            String category = item.category != null ? item.category.replace("_", " ") : "";
            String duration = formatDuration(item.durationMinutes);
            String price = formatPrice(item.price, item.currency);
            TourActivity activity = new TourActivity(item.name, destination, category, duration, price,
                    item.availableSpots, null);
            activity.setId(item.id);
            result.add(activity);
        }
        return result;
    }

    private static String formatDuration(int minutes) {
        if (minutes < 60) return minutes + " min";
        int hours = minutes / 60;
        int remaining = minutes % 60;
        if (remaining == 0) return hours + (hours == 1 ? " hora" : " horas");
        return hours + " h " + remaining + " min";
    }

    private static String formatPrice(double price, String currency) {
        if (price <= 0) return "Gratis";
        String symbol = "ARS".equals(currency) ? "$" : currency + " ";
        return symbol + String.format(Locale.US, "%.2f", price);
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

