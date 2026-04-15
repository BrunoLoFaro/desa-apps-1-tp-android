package com.example.myapplication.ui.bookings.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.repository.BookingRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public class BookingsViewModel extends ViewModel {

    private final BookingRepository bookingRepository;
    private final MutableLiveData<List<BookingResponse>> _bookings =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);

    @Inject
    public BookingsViewModel(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public LiveData<List<BookingResponse>> getBookings() { return _bookings; }
    public LiveData<UiMessage> getError() { return _error; }
    public LiveData<Boolean> isLoading() { return _loading; }

    public void loadMyBookings(String statusFilter) {
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
                loadMyBookings(null);
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
        super.onCleared();
    }
}

