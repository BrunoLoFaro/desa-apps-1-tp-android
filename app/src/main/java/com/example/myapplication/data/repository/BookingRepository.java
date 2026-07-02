package com.example.myapplication.data.repository;

import android.util.Log;
import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.local.OfflineBookingDao;
import com.example.myapplication.data.local.OfflineBookingEntity;
import com.example.myapplication.data.model.BookingResponse;
import com.example.myapplication.data.model.BookingsPageResponse;
import com.example.myapplication.data.model.BookingSummaryItem;
import com.example.myapplication.data.model.CreateBookingRequest;
import com.example.myapplication.data.model.DestinationResponse;
import com.example.myapplication.util.FormatUtils;
import com.example.myapplication.util.MainThreadUtils;
import com.example.myapplication.data.network.BookingService;
import com.example.myapplication.util.NetworkErrorParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Inject;

public class BookingRepository extends BaseRepository {

    private static final String TAG = "BookingRepository";

    private final BookingService bookingService;
    private final SessionRepository sessionRepository;
    private final OfflineBookingDao offlineBookingDao;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

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
                            dbExecutor.execute(() -> {
                                offlineBookingDao.insertAll(Collections.singletonList(toEntity(data, userId)));
                                MainThreadUtils.post(() -> callback.onSuccess(data));
                            });
                        } else {
                            callback.onSuccess(null);
                        }
                    }

                    @Override
                    public void onError(UiMessage error) {
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
                    dbExecutor.execute(() -> {
                        offlineBookingDao.replaceConfirmed(userId, entities);
                        List<OfflineBookingEntity> roomData = offlineBookingDao.getConfirmedByUser(userId);
                        List<BookingResponse> result = new ArrayList<>();
                        for (OfflineBookingEntity e : roomData) result.add(fromEntity(e));
                        MainThreadUtils.post(() -> callback.onSuccess(result));
                    });
                } else {
                    callback.onSuccess(items);
                }
            }

            @Override
            public void onError(UiMessage error) {
                callback.onError(error);
            }
        }, R.string.error_internal_server);
    }

    public void loadCachedConfirmedBookings(RepositoryCallback<List<BookingResponse>> callback) {
        long userId = sessionRepository.getUserId();
        dbExecutor.execute(() -> {
            List<OfflineBookingEntity> entities = offlineBookingDao.getConfirmedByUser(userId);
            List<BookingResponse> result = new ArrayList<>();
            for (OfflineBookingEntity e : entities) result.add(fromEntity(e));
            MainThreadUtils.post(() -> callback.onSuccess(result));
        });
    }

    public void loadCachedHistorial(RepositoryCallback<List<BookingSummaryItem>> callback) {
        long userId = sessionRepository.getUserId();
        dbExecutor.execute(() -> {
            List<OfflineBookingEntity> entities = offlineBookingDao.getHistorialByUser(userId);
            List<BookingSummaryItem> result = new ArrayList<>(entities.size());
            for (OfflineBookingEntity e : entities) {
                result.add(new BookingSummaryItem(
                        e.id > 0 ? e.id : null,
                        e.activityId,
                        e.activityName,
                        e.status,
                        FormatUtils.formatDate(e.sessionStartTime),
                        FormatUtils.formatPrice(e.totalPrice, e.currency),
                        e.destinationName != null ? e.destinationName : "",
                        e.guideName != null ? e.guideName : "",
                        e.durationMinutes,
                        null,
                        FormatUtils.extractTime(e.sessionStartTime),
                        false,
                        e.sessionStartTime));
            }
            MainThreadUtils.post(() -> callback.onSuccess(result));
        });
    }

    public void cancelBooking(Long bookingId, RepositoryCallback<BookingResponse> callback) {
        long userId = sessionRepository.getUserId();
        String url = "users/" + userId + "/bookings/" + bookingId;
        enqueue(bookingService.cancelBooking(url), new RepositoryCallback<BookingResponse>() {
            @Override
            public void onSuccess(BookingResponse data) {
                if (bookingId != null) {
                    dbExecutor.execute(() -> offlineBookingDao.deleteById(bookingId));
                }
                callback.onSuccess(data);
            }

            @Override
            public void onError(UiMessage error) {
                callback.onError(error);
            }
        }, R.string.error_internal_server);
    }

    public void loadCachedPendingCancellations(RepositoryCallback<List<BookingResponse>> callback) {
        long userId = sessionRepository.getUserId();
        dbExecutor.execute(() -> {
            List<OfflineBookingEntity> entities = offlineBookingDao.getPendingCancellations(userId);
            List<BookingResponse> result = new ArrayList<>();
            for (OfflineBookingEntity e : entities) result.add(fromEntity(e));
            MainThreadUtils.post(() -> callback.onSuccess(result));
        });
    }

    public void cancelBookingLocally(Long bookingId, Runnable onDone) {
        dbExecutor.execute(() -> {
            if (bookingId != null) offlineBookingDao.markPendingCancel(bookingId);
            MainThreadUtils.post(onDone);
        });
    }

    public void syncPendingCancellations(Runnable onComplete) {
        long userId = sessionRepository.getUserId();
        dbExecutor.execute(() -> {
            List<OfflineBookingEntity> pending = offlineBookingDao.getPendingCancellations(userId);
            for (OfflineBookingEntity e : pending) {
                try {
                    retrofit2.Response<BookingResponse> resp =
                            bookingService.cancelBooking("users/" + userId + "/bookings/" + e.id).execute();
                    if (resp.isSuccessful()) {
                        offlineBookingDao.deleteById(e.id);
                    } else {
                        Log.w(TAG, "Sync cancelación fallida para booking " + e.id + " — HTTP " + resp.code());
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Error sincronizando cancelación pendiente para booking " + e.id, ex);
                }
            }
            MainThreadUtils.post(onComplete);
        });
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
        e.voucherCode = b.voucherCode;
        e.meetingPoint = b.meetingPoint;
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
        b.status = e.pendingCancel ? "PENDING_CANCEL" : e.status;
        b.cancellationPolicy = e.cancellationPolicy;
        b.createdAt = e.createdAt;
        b.cancelledAt = e.cancelledAt;
        b.canReview = e.canReview;
        b.voucherCode = e.voucherCode;
        b.meetingPoint = e.meetingPoint;
        return b;
    }
}
