package com.example.myapplication.data.repository;

import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.data.model.BookingSummaryItemResponse;
import com.example.myapplication.data.model.BookingSummaryPageResponse;
import com.example.myapplication.data.model.UserPreferencesResponse;
import com.example.myapplication.data.model.UserProfileData;
import com.example.myapplication.data.model.UserProfileResponse;
import com.example.myapplication.data.network.ProfileService;
import com.example.myapplication.util.FormatUtils;
import com.example.myapplication.util.NetworkErrorParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Response;

@Singleton
public class ProfileRepository {

    private final ProfileService profileService;
    private final ConfigLoader configLoader;
    private final NetworkErrorParser errorParser;
    private final List<Call<?>> activeCalls = new CopyOnWriteArrayList<>();

    @Inject
    public ProfileRepository(ProfileService profileService, ConfigLoader configLoader,
                             NetworkErrorParser errorParser) {
        this.profileService = profileService;
        this.configLoader = configLoader;
        this.errorParser = errorParser;
    }

    public void getProfile(RepositoryCallback<UserProfileData> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        Call<UserProfileResponse> call = profileService.getProfile(config.profileEndpoint);
        enqueueProfile(call, callback);
    }

    public void updateProfile(String firstName, String lastName, String phone,
                              String profilePhotoUrl,
                              RepositoryCallback<UserProfileData> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        ProfileService.ProfileUpdateBody body =
                new ProfileService.ProfileUpdateBody(firstName, lastName, phone, profilePhotoUrl);
        Call<UserProfileResponse> call = profileService.updateProfile(config.profileEndpoint, body);
        enqueueProfile(call, callback);
    }

    public void getPreferences(RepositoryCallback<List<String>> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        Call<UserPreferencesResponse> call = profileService.getPreferences(config.preferencesEndpoint);
        activeCalls.add(call);
        call.enqueue(new retrofit2.Callback<UserPreferencesResponse>() {
            @Override
            public void onResponse(Call<UserPreferencesResponse> c, Response<UserPreferencesResponse> response) {
                activeCalls.remove(c);
                if (response.isSuccessful() && response.body() != null) {
                    List<String> categories = response.body().preferredCategories;
                    callback.onSuccess(categories != null ? categories : Collections.emptyList());
                } else {
                    callback.onError(errorParser.getErrorMessage(response, R.string.error_load_profile));
                }
            }

            @Override
            public void onFailure(Call<UserPreferencesResponse> c, Throwable t) {
                activeCalls.remove(c);
                callback.onError(errorParser.getFailureMessage(t, R.string.error_network_generic));
            }
        });
    }

    public void updatePreferences(List<String> preferredCategories,
                                  RepositoryCallback<List<String>> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        ProfileService.PreferencesUpdateBody body =
                new ProfileService.PreferencesUpdateBody(preferredCategories);
        Call<UserPreferencesResponse> call =
                profileService.updatePreferences(config.preferencesEndpoint, body);
        activeCalls.add(call);
        call.enqueue(new retrofit2.Callback<UserPreferencesResponse>() {
            @Override
            public void onResponse(Call<UserPreferencesResponse> c, Response<UserPreferencesResponse> response) {
                activeCalls.remove(c);
                if (response.isSuccessful() && response.body() != null) {
                    List<String> categories = response.body().preferredCategories;
                    callback.onSuccess(categories != null ? categories : Collections.emptyList());
                } else {
                    callback.onError(errorParser.getErrorMessage(response, R.string.error_save_profile));
                }
            }

            @Override
            public void onFailure(Call<UserPreferencesResponse> c, Throwable t) {
                activeCalls.remove(c);
                callback.onError(errorParser.getFailureMessage(t, R.string.error_network_generic));
            }
        });
    }

    public void getActivitySummary(RepositoryCallback<List<BookingSummaryItem>> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        Call<BookingSummaryPageResponse> call =
                profileService.getActivitySummary(config.activitySummaryEndpoint);
        activeCalls.add(call);
        call.enqueue(new retrofit2.Callback<BookingSummaryPageResponse>() {
            @Override
            public void onResponse(Call<BookingSummaryPageResponse> c,
                                   Response<BookingSummaryPageResponse> response) {
                activeCalls.remove(c);
                if (response.isSuccessful() && response.body() != null
                        && response.body().items != null) {
                    callback.onSuccess(mapToSummaryItems(response.body().items));
                } else {
                    callback.onError(errorParser.getErrorMessage(response, R.string.error_load_profile));
                }
            }

            @Override
            public void onFailure(Call<BookingSummaryPageResponse> c, Throwable t) {
                activeCalls.remove(c);
                callback.onError(errorParser.getFailureMessage(t, R.string.error_network_generic));
            }
        });
    }

    public void cancelAll() {
        for (Call<?> call : activeCalls) {
            if (!call.isCanceled()) call.cancel();
        }
        activeCalls.clear();
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    private void enqueueProfile(Call<UserProfileResponse> call,
                                RepositoryCallback<UserProfileData> callback) {
        activeCalls.add(call);
        call.enqueue(new retrofit2.Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> c, Response<UserProfileResponse> response) {
                activeCalls.remove(c);
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(mapToProfileData(response.body()));
                } else {
                    callback.onError(errorParser.getErrorMessage(response, R.string.error_load_profile));
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> c, Throwable t) {
                activeCalls.remove(c);
                callback.onError(errorParser.getFailureMessage(t, R.string.error_network_generic));
            }
        });
    }

    private UserProfileData mapToProfileData(UserProfileResponse r) {
        return new UserProfileData(
                r.email,
                r.firstName != null ? r.firstName : "",
                r.lastName != null ? r.lastName : "",
                r.phone != null ? r.phone : "",
                r.profilePhotoUrl,
                r.preferredCategories != null ? r.preferredCategories : Collections.emptyList(),
                r.confirmedBookings,
                r.completedBookings,
                r.cancelledBookings
        );
    }

    private List<BookingSummaryItem> mapToSummaryItems(List<BookingSummaryItemResponse> items) {
        List<BookingSummaryItem> result = new ArrayList<>(items.size());
        for (BookingSummaryItemResponse item : items) {
            String date = formatDate(item.sessionStartTime);
            String price = FormatUtils.formatPrice(item.totalPrice, item.currency);
            result.add(new BookingSummaryItem(item.id, item.activityName, item.status, date, price));
        }
        return result;
    }

    private static String formatDate(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.length() < 10) return "";
        return isoDateTime.substring(0, 10);
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
