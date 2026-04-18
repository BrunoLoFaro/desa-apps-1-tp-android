package com.example.myapplication.data.network;

import com.example.myapplication.data.model.CreateReviewRequest;
import com.example.myapplication.data.model.ReviewSummaryResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface ReviewService {
    @POST
    Call<ReviewSummaryResponse> createReview(@Url String url, @Body CreateReviewRequest request);
}

