package com.example.myapplication.data.repository;

import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.model.CreateReviewRequest;
import com.example.myapplication.data.model.ReviewSummaryResponse;
import com.example.myapplication.data.network.ReviewService;
import com.example.myapplication.util.NetworkErrorParser;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Response;

public class ReviewRepository extends BaseRepository {

    private final ReviewService reviewService;
    private final SessionRepository sessionRepository;

    @Inject
    public ReviewRepository(ReviewService reviewService, SessionRepository sessionRepository,
                            NetworkErrorParser errorParser) {
        super(errorParser);
        this.reviewService = reviewService;
        this.sessionRepository = sessionRepository;
    }

    public void createReview(Long bookingId, int activityRating, Integer guideRating, String comment,
                             RepositoryCallback<ReviewSummaryResponse> callback) {
        long userId = sessionRepository.getUserId();
        String url = "users/" + userId + "/reviews";
        CreateReviewRequest request = new CreateReviewRequest(bookingId, activityRating, guideRating, comment);
        enqueue(reviewService.createReview(url, request), callback, R.string.error_internal_server);
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
                        sessionRepository.clearSession();
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
}
