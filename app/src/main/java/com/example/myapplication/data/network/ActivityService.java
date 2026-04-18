package com.example.myapplication.data.network;

import com.example.myapplication.data.model.ActivitiesPageResponse;
import com.example.myapplication.data.model.ActivityDetailResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface ActivityService {

    @GET
    Call<ActivitiesPageResponse> listActivities(@Url String url);

    @GET
    Call<ActivitiesPageResponse> listFeatured(@Url String url);

    @GET
    Call<ActivityDetailResponse> getActivityDetail(@Url String url);
}
