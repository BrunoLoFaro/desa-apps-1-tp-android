package com.example.myapplication.data.repository;

import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.model.ActivitiesPageResponse;
import com.example.myapplication.data.model.ActivitySummaryResponse;
import com.example.myapplication.data.model.DestinationResponse;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.network.ActivityService;
import com.example.myapplication.data.network.CatalogMetaService;
import com.example.myapplication.data.session.SessionManager;
import com.example.myapplication.util.FormatUtils;
import com.example.myapplication.util.NetworkErrorParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.HttpUrl;
import retrofit2.Call;
import retrofit2.Response;

@Singleton
public class ExploreRepository extends BaseRepository {

    public static final int DEFAULT_PAGE_SIZE = 10;

    private final ActivityService activityService;
    private final CatalogMetaService metaService;
    private final ConfigLoader configLoader;
    private final SessionManager sessionManager;

    @Inject
    public ExploreRepository(ActivityService activityService, CatalogMetaService metaService,
                             ConfigLoader configLoader, SessionManager sessionManager,
                             NetworkErrorParser errorParser) {
        super(errorParser);
        this.activityService = activityService;
        this.metaService = metaService;
        this.configLoader = configLoader;
        this.sessionManager = sessionManager;
    }

    public void listActivities(
            Integer page,
            Integer size,
            Long destinationId,
            String category,
            String dateIso,
            String minPrice,
            String maxPrice,
            RepositoryCallback<ActivitiesPageResponse> callback
    ) {
        AppConfig config = getConfig(callback);
        if (config == null) return;

        int safePage = page == null ? 0 : Math.max(0, page);
        int safeSize = size == null ? DEFAULT_PAGE_SIZE : Math.max(1, size);

        HttpUrl url = buildActivitiesUrl(config, safePage, safeSize, destinationId, category, dateIso, minPrice, maxPrice);
        if (url == null) {
            callback.onError(UiMessage.from(R.string.error_invalid_config));
            return;
        }

        enqueue(activityService.listActivities(url.toString()), callback, R.string.error_load_activities);
    }

    public void listDestinations(RepositoryCallback<List<DestinationResponse>> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        HttpUrl url = buildUrl(config.baseUrl, "destinations");
        if (url == null) {
            callback.onError(UiMessage.from(R.string.error_invalid_config));
            return;
        }
        enqueue(metaService.listDestinations(url.toString()), callback, R.string.error_network_generic);
    }

    public void listCategories(RepositoryCallback<List<String>> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        HttpUrl url = buildUrl(config.baseUrl, "categories");
        if (url == null) {
            callback.onError(UiMessage.from(R.string.error_invalid_config));
            return;
        }
        enqueue(metaService.listCategories(url.toString()), callback, R.string.error_network_generic);
    }

    public static List<TourActivity> mapToTourActivities(List<ActivitySummaryResponse> items) {
        if (items == null) return Collections.emptyList();
        List<TourActivity> result = new ArrayList<>(items.size());
        for (ActivitySummaryResponse item : items) {
            String destination = item.destination != null ? item.destination.name : "";
            String normalizedCategory = item.category != null ? item.category.replace("_", " ") : "";
            String duration = FormatUtils.formatDuration(item.durationMinutes);
            String price = FormatUtils.formatPrice(item.price, item.currency);

            TourActivity activity = new TourActivity(item.name, destination, normalizedCategory, duration, price,
                    item.availableSpots, null);
            activity.setId(item.id);
            if (item.avgRating != null) activity.setRating(item.avgRating.floatValue());
            if (item.reviewCount != null) activity.setReviewsCount(item.reviewCount.intValue());

            result.add(activity);
        }
        return result;
    }

    private HttpUrl buildActivitiesUrl(
            AppConfig config,
            int page,
            int size,
            Long destinationId,
            String category,
            String dateIso,
            String minPrice,
            String maxPrice
    ) {
        HttpUrl base = buildUrl(config.baseUrl, config.activitiesEndpoint);
        if (base == null) return null;

        HttpUrl.Builder b = base.newBuilder()
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("size", String.valueOf(size));

        if (destinationId != null) b.addQueryParameter("destinationId", String.valueOf(destinationId));
        if (category != null && !category.trim().isEmpty()) b.addQueryParameter("category", category);
        if (dateIso != null && !dateIso.trim().isEmpty()) b.addQueryParameter("date", dateIso);
        if (minPrice != null && !minPrice.trim().isEmpty()) b.addQueryParameter("minPrice", minPrice);
        if (maxPrice != null && !maxPrice.trim().isEmpty()) b.addQueryParameter("maxPrice", maxPrice);

        return b.build();
    }

    private static HttpUrl buildUrl(String baseUrl, String relativePath) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) return null;
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        String path = relativePath != null ? relativePath : "";
        return HttpUrl.parse(base + path);
    }

    @Override
    protected <T> void enqueue(Call<T> call, RepositoryCallback<T> callback, int fallbackResId) {
        activeCalls.add(call);
        call.enqueue(new retrofit2.Callback<T>() {
            @Override
            public void onResponse(Call<T> c, Response<T> response) {
                activeCalls.remove(c);
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    if (response.code() == 401) {
                        sessionManager.clearSession();
                    }
                    callback.onError(errorParser.getErrorMessage(response, fallbackResId));
                }
            }

            @Override
            public void onFailure(Call<T> c, Throwable t) {
                activeCalls.remove(c);
                callback.onError(errorParser.getFailureMessage(t, fallbackResId));
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
