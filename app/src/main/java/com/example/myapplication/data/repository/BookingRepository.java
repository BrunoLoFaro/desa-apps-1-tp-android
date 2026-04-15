package com.example.myapplication.data.repository;

import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.model.BookingsPageResponse;
import com.example.myapplication.data.model.CreateBookingRequest;
import com.example.myapplication.data.network.BookingService;
import com.example.myapplication.util.NetworkErrorParser;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Response;

public class BookingRepository {

    private final BookingService bookingService;
    private final SessionRepository sessionRepository;
    private final NetworkErrorParser errorParser;
    private final List<Call<?>> activeCalls = new CopyOnWriteArrayList<>();

    @Inject
    public BookingRepository(BookingService bookingService, SessionRepository sessionRepository,
                             NetworkErrorParser errorParser) {
        this.bookingService = bookingService;
        this.sessionRepository = sessionRepository;
        this.errorParser = errorParser;
    }

    public void createBooking(Long sessionId, int participants, RepositoryCallback<BookingResponse> callback) {
        long userId = sessionRepository.getUserId();
        String url = "users/" + userId + "/bookings";
        enqueue(bookingService.createBooking(url, new CreateBookingRequest(sessionId, participants)),
                callback, R.string.error_internal_server);
    }

    public void listMyBookings(String statusFilter, RepositoryCallback<List<BookingResponse>> callback) {
        long userId = sessionRepository.getUserId();
        String url = "users/" + userId + "/bookings";
        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            url += "?status=" + statusFilter;
        }
        enqueue(bookingService.listBookings(url), new RepositoryCallback<BookingsPageResponse>() {
            @Override
            public void onSuccess(BookingsPageResponse data) {
                callback.onSuccess(data != null && data.items != null ? data.items : java.util.Collections.emptyList());
            }

            @Override
            public void onError(com.example.myapplication.data.common.UiMessage error) {
                callback.onError(error);
            }
        }, R.string.error_internal_server);
    }

    public void cancelBooking(Long bookingId, RepositoryCallback<BookingResponse> callback) {
        long userId = sessionRepository.getUserId();
        String url = "users/" + userId + "/bookings/" + bookingId;
        enqueue(bookingService.cancelBooking(url), callback, R.string.error_internal_server);
    }

    public void cancelAll() {
        for (Call<?> call : activeCalls) {
            if (!call.isCanceled()) call.cancel();
        }
        activeCalls.clear();
    }

    private <T> void enqueue(Call<T> call, RepositoryCallback<T> callback, int fallbackResId) {
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
