package com.example.myapplication.ui.home.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.repository.BookingRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class CreateBookingViewModel extends ViewModel {

    private final BookingRepository bookingRepository;
    private final MutableLiveData<BookingResponse> _booking = new MutableLiveData<>();
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);

    @Inject
    public CreateBookingViewModel(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public LiveData<BookingResponse> getBooking() { return _booking; }
    public LiveData<UiMessage> getError() { return _error; }
    public LiveData<Boolean> isLoading() { return _loading; }

    public void clearBooking() {
        _booking.setValue(null);
    }

    public void create(Long sessionId, int participants) {
        if (sessionId == null || sessionId <= 0) return;
        if (participants < 1) {
            _error.setValue(UiMessage.from("Participantes invalidos"));
            return;
        }
        _loading.setValue(true);
        bookingRepository.createBooking(sessionId, participants, new RepositoryCallback<BookingResponse>() {
            @Override
            public void onSuccess(BookingResponse data) {
                _loading.setValue(false);
                _booking.setValue(data);
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
