package com.example.myapplication.data.repository;

import android.os.Handler;
import android.os.Looper;
import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.local.OfflineBookingDao;
import com.example.myapplication.data.local.OfflineBookingEntity;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.model.BookingsPageResponse;
import com.example.myapplication.data.model.CreateBookingRequest;
import com.example.myapplication.data.model.DestinationResponse;
import com.example.myapplication.data.network.BookingService;
import com.example.myapplication.util.NetworkErrorParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

public class BookingRepository extends BaseRepository {

    private final BookingService bookingService;
    private final SessionRepository sessionRepository;
    private final OfflineBookingDao offlineBookingDao;

    @Inject
    public BookingRepository(BookingService bookingService, SessionRepository sessionRepository,
                             NetworkErrorParser errorParser, OfflineBookingDao offlineBookingDao) {
        super(errorParser);
        this.bookingService = bookingService;
        this.sessionRepository = sessionRepository;
        this.offlineBookingDao = offlineBookingDao;
    }

    public void createBooking(Long sessionId, int participants, RepositoryCallback<BookingResponse> callback) {
        long userId = sessionRepository.getUserId();
        String url = "users/" + userId + "/bookings";
        enqueue(bookingService.createBooking(url, new CreateBookingRequest(sessionId, participants)),
                new RepositoryCallback<BookingResponse>() {
                    @Override
                    public void onSuccess(BookingResponse data) {
                        if (data != null) {
                            new Thread(() -> offlineBookingDao.insertAll(
                                    Collections.singletonList(toEntity(data, userId)))).start();
                        }
                        callback.onSuccess(data);
                    }

                    @Override
                    public void onError(com.example.myapplication.data.common.UiMessage error) {
                        callback.onError(error);
                    }
                }, R.string.error_internal_server);
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
                List<BookingResponse> items = data != null && data.items != null
                        ? data.items : Collections.emptyList();
                if ("CONFIRMED".equals(statusFilter)) {
                    List<OfflineBookingEntity> entities = new ArrayList<>();
                    for (BookingResponse b : items) entities.add(toEntity(b, userId));
                    new Thread(() -> offlineBookingDao.replaceConfirmed(userId, entities)).start();
                }
                callback.onSuccess(items);
            }

            @Override
            public void onError(com.example.myapplication.data.common.UiMessage error) {
                callback.onError(error);
            }
        }, R.string.error_internal_server);
    }

    public void loadCachedConfirmedBookings(RepositoryCallback<List<BookingResponse>> callback) {
        long userId = sessionRepository.getUserId();
        new Thread(() -> {
            List<OfflineBookingEntity> entities = offlineBookingDao.getConfirmedByUser(userId);
            List<BookingResponse> result = new ArrayList<>();
            for (OfflineBookingEntity e : entities) result.add(fromEntity(e));
            new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(result));
        }).start();
    }

    public void cancelBooking(Long bookingId, RepositoryCallback<BookingResponse> callback) {
        long userId = sessionRepository.getUserId();
        String url = "users/" + userId + "/bookings/" + bookingId;
        enqueue(bookingService.cancelBooking(url), callback, R.string.error_internal_server);
    }

    public void cancelBookingLocally(Long bookingId, Runnable onDone) {
        new Thread(() -> {
            if (bookingId != null) offlineBookingDao.markPendingCancel(bookingId);
            new Handler(Looper.getMainLooper()).post(onDone);
        }).start();
    }

    public void syncPendingCancellations(Runnable onComplete) {
        long userId = sessionRepository.getUserId();
        new Thread(() -> {
            List<OfflineBookingEntity> pending = offlineBookingDao.getPendingCancellations(userId);
            for (OfflineBookingEntity e : pending) {
                try {
                    retrofit2.Response<BookingResponse> resp =
                            bookingService.cancelBooking("users/" + userId + "/bookings/" + e.id).execute();
                    if (resp.isSuccessful()) offlineBookingDao.deleteById(e.id);
                } catch (Exception ignored) {}
            }
            new Handler(Looper.getMainLooper()).post(onComplete);
        }).start();
    }

    private static OfflineBookingEntity toEntity(BookingResponse b, long userId) {
        OfflineBookingEntity e = new OfflineBookingEntity();
        e.id = b.id != null ? b.id : 0;
        e.userId = userId;
        e.sessionId = b.sessionId;
        e.activityId = b.activityId;
        e.activityName = b.activityName;
        e.destinationName = b.destination != null ? b.destination.name : null;
        e.guideName = b.guideName;
        e.sessionStartTime = b.sessionStartTime;
        e.durationMinutes = b.durationMinutes;
        e.participants = b.participants;
        e.totalPrice = b.totalPrice;
        e.currency = b.currency;
        e.status = b.status;
        e.cancellationPolicy = b.cancellationPolicy;
        e.createdAt = b.createdAt;
        e.cancelledAt = b.cancelledAt;
        e.canReview = b.canReview;
        return e;
    }

    private static BookingResponse fromEntity(OfflineBookingEntity e) {
        BookingResponse b = new BookingResponse();
        b.id = e.id;
        b.sessionId = e.sessionId;
        b.activityId = e.activityId;
        b.activityName = e.activityName;
        if (e.destinationName != null) {
            b.destination = new DestinationResponse();
            b.destination.name = e.destinationName;
        }
        b.guideName = e.guideName;
        b.sessionStartTime = e.sessionStartTime;
        b.durationMinutes = e.durationMinutes;
        b.participants = e.participants;
        b.totalPrice = e.totalPrice;
        b.currency = e.currency;
        b.status = e.status;
        b.cancellationPolicy = e.cancellationPolicy;
        b.createdAt = e.createdAt;
        b.cancelledAt = e.cancelledAt;
        b.canReview = e.canReview;
        return b;
    }
}
