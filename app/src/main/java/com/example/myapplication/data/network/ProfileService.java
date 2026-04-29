package com.example.myapplication.data.network;

import com.example.myapplication.data.model.BookingSummaryPageResponse;
import com.example.myapplication.data.model.ReviewResponse;
import com.example.myapplication.data.model.UserPreferencesResponse;
import com.example.myapplication.data.model.UserProfileResponse;
import java.util.List;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Url;

public interface ProfileService {

    @GET
    Call<UserProfileResponse> getProfile(@Url String url);

    /** Actualiza datos de perfil (texto). La imagen se gestiona localmente en Android. */
    @PUT
    Call<UserProfileResponse> updateProfile(
            @Url String url,
            @Body RequestBody data
    );

    @GET
    Call<UserPreferencesResponse> getPreferences(@Url String url);

    @PUT
    Call<UserPreferencesResponse> updatePreferences(@Url String url, @Body PreferencesUpdateBody body);

    @GET
    Call<BookingSummaryPageResponse> getActivitySummary(@Url String url);

    @GET
    Call<List<ReviewResponse>> getMyReviews(@Url String url);

    /** GET /api/v1/users/{userId}/reviews/booking/{bookingId} */
    @GET
    Call<ReviewResponse> getReviewByBooking(@Url String url);

    // ── Request body POJOs ──────────────────────────────────────────────────────

    class PreferencesUpdateBody {
        public java.util.List<String> preferredCategories;

        public PreferencesUpdateBody(java.util.List<String> preferredCategories) {
            this.preferredCategories = preferredCategories;
        }
    }
}
