package com.example.myapplication.data.network;

import com.example.myapplication.data.model.BookingSummaryPageResponse;
import com.example.myapplication.data.model.UserPreferencesResponse;
import com.example.myapplication.data.model.UserProfileResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Url;

public interface ProfileService {

    @GET
    Call<UserProfileResponse> getProfile(@Url String url);

    /** Actualiza perfil con imagen opcional. El backend solo acepta multipart/form-data. */
    @Multipart
    @PUT
    Call<UserProfileResponse> updateProfile(
            @Url String url,
            @Part("data") RequestBody data,
            @Part MultipartBody.Part profilePhoto  // null = sin foto nueva
    );

    @GET
    Call<UserPreferencesResponse> getPreferences(@Url String url);

    @PUT
    Call<UserPreferencesResponse> updatePreferences(@Url String url, @Body PreferencesUpdateBody body);

    @GET
    Call<BookingSummaryPageResponse> getActivitySummary(@Url String url);

    // ── Request body POJOs ──────────────────────────────────────────────────────

    class PreferencesUpdateBody {
        public java.util.List<String> preferredCategories;

        public PreferencesUpdateBody(java.util.List<String> preferredCategories) {
            this.preferredCategories = preferredCategories;
        }
    }
}
