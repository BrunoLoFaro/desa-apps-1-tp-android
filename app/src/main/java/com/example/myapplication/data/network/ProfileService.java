package com.example.myapplication.data.network;

import com.example.myapplication.data.model.BookingSummaryPageResponse;
import com.example.myapplication.data.model.UserPreferencesResponse;
import com.example.myapplication.data.model.UserProfileResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Url;

public interface ProfileService {

    @GET
    Call<UserProfileResponse> getProfile(@Url String url);

    @PUT
    Call<UserProfileResponse> updateProfile(@Url String url, @Body ProfileUpdateBody body);

    @GET
    Call<UserPreferencesResponse> getPreferences(@Url String url);

    @PUT
    Call<UserPreferencesResponse> updatePreferences(@Url String url, @Body PreferencesUpdateBody body);

    @GET
    Call<BookingSummaryPageResponse> getActivitySummary(@Url String url);

    // ── Request body POJOs ──────────────────────────────────────────────────────

    class ProfileUpdateBody {
        public String firstName;
        public String lastName;
        public String phone;
        public String profilePhotoUrl;

        public ProfileUpdateBody(String firstName, String lastName,
                                 String phone, String profilePhotoUrl) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.phone = phone;
            this.profilePhotoUrl = profilePhotoUrl;
        }
    }

    class PreferencesUpdateBody {
        public java.util.List<String> preferredCategories;

        public PreferencesUpdateBody(java.util.List<String> preferredCategories) {
            this.preferredCategories = preferredCategories;
        }
    }
}
