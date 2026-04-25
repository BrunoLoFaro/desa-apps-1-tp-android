package com.example.myapplication.data.network;

import com.example.myapplication.data.model.NewsDetailResponse;
import com.example.myapplication.data.model.NewsPageResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface NewsService {

    @GET
    Call<NewsPageResponse> listNews(@Url String url, @Query("page") Integer page, @Query("size") Integer size);

    @GET
    Call<NewsDetailResponse> getNewsDetail(@Url String url);
}
