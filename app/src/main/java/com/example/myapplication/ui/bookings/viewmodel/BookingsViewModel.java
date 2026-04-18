package com.example.myapplication.ui.bookings.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.repository.BookingRepository;
import com.example.myapplication.data.repository.ReviewRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public class BookingsViewModel extends ViewModel {

    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final MutableLiveData<List<BookingResponse>> _bookings =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    private final MutableLiveData<UiMessage> _message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    private String currentFilter = null;

    @Inject
    public BookingsViewModel(BookingRepository bookingRepository, ReviewRepository reviewRepository) {
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
    }

    public LiveData<List<BookingResponse>> getBookings() { return _bookings; }
    public LiveData<UiMessage> getError() { return _error; }
    public LiveData<UiMessage> getMessage() { return _message; }
    public LiveData<Boolean> isLoading() { return _loading; }

    public void clearMessage() {
        _message.setValue(null);
    }

    public void loadMyBookings(String statusFilter) {
        currentFilter = statusFilter;
        _loading.setValue(true);
        bookingRepository.listMyBookings(statusFilter, new RepositoryCallback<List<BookingResponse>>() {
            @Override
            public void onSuccess(List<BookingResponse> data) {
                _loading.setValue(false);
                _bookings.setValue(data != null ? data : Collections.emptyList());
            }

            @Override
            public void onError(UiMessage error) {
                _loading.setValue(false);
                _error.setValue(error);
            }
        });
    }

    public void cancelBooking(Long bookingId) {
        if (bookingId == null) return;
        _loading.setValue(true);
        bookingRepository.cancelBooking(bookingId, new RepositoryCallback<BookingResponse>() {
            @Override
            public void onSuccess(BookingResponse data) {
                _loading.setValue(false);
                loadMyBookings(currentFilter);
            }

            @Override
            public void onError(UiMessage error) {
                _loading.setValue(false);
                _error.setValue(error);
            }
        });
    }

    public void submitReview(Long bookingId, int activityRating, Integer guideRating, String comment) {
        if (bookingId == null) return;
        _loading.setValue(true);
        reviewRepository.createReview(bookingId, activityRating, guideRating, comment,
                new RepositoryCallback<com.example.myapplication.data.model.ReviewSummaryResponse>() {
                    @Override
                    public void onSuccess(com.example.myapplication.data.model.ReviewSummaryResponse data) {
                        _loading.setValue(false);
                        _message.setValue(UiMessage.from(R.string.review_thanks));
                        loadMyBookings(currentFilter);
                    }

                    @Override
                    public void onError(UiMessage error) {
                        _loading.setValue(false);
                        _error.setValue(error);
                    }
                });
    }

    @Override
    protected void onCleared() {
        bookingRepository.cancelAll();
        reviewRepository.cancelAll();
        super.onCleared();
    }
}
