package com.example.myapplication.data.network;

import com.example.myapplication.data.model.ActivitiesPageResponse;
import com.example.myapplication.data.model.ActivityDetailResponse;
import com.example.myapplication.data.model.ActivitySummaryResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface ActivityService {

    @GET
    Call<ActivitiesPageResponse> listActivities(@Url String url);

    @GET
    Call<ActivitiesPageResponse> listFeatured(@Url String url);

    @GET
    Call<ActivityDetailResponse> getActivityDetail(@Url String url);

    @GET
    Call<List<String>> getCategories(@Url String url);

    @GET("favorites")
    Call<List<ActivitySummaryResponse>> getFavorites();

    @POST("favorites/{activityId}")
    Call<Void> addFavorite(@Path("activityId") long activityId);

    @DELETE("favorites/{activityId}")
    Call<Void> removeFavorite(@Path("activityId") long activityId);
}
