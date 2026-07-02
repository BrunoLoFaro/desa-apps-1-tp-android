package com.example.myapplication.ui.home.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.ReviewResponse;
import com.example.myapplication.data.repository.ProfileRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class HistoryReviewViewModel extends ViewModel {

    private final ProfileRepository profileRepository;
    private final MutableLiveData<ReviewResponse> _review = new MutableLiveData<>();

    @Inject
    public HistoryReviewViewModel(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public LiveData<ReviewResponse> getReview() { return _review; }

    public void loadReview(long bookingId) {
        profileRepository.getReviewByBookingId(bookingId, new RepositoryCallback<ReviewResponse>() {
            @Override
            public void onSuccess(ReviewResponse data) {
                _review.setValue(data); // null = sin reseña
            }
            @Override
            public void onError(UiMessage error) {
                _review.setValue(null); // best-effort: falla silenciosamente
            }
        });
    }

    @Override
    protected void onCleared() {
        profileRepository.cancelAll();
        super.onCleared();
    }
}
