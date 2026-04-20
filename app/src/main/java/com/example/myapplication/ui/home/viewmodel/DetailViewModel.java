package com.example.myapplication.ui.home.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.ActivityDetailResponse;
import com.example.myapplication.data.model.ActivitySessionResponse;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.repository.TourRepository;
import com.example.myapplication.util.FormatUtils;
import dagger.hilt.android.lifecycle.HiltViewModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public class DetailViewModel extends ViewModel {

    private final TourRepository tourRepository;

    private final MutableLiveData<TourActivity> _activity = new MutableLiveData<>();
    private final MutableLiveData<List<ActivitySessionResponse>> _sessions =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<UiMessage> _error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);

    @Inject
    public DetailViewModel(TourRepository tourRepository) {
        this.tourRepository = tourRepository;
    }

    public LiveData<TourActivity> getActivity() { return _activity; }
    public LiveData<List<ActivitySessionResponse>> getSessions() { return _sessions; }
    public LiveData<UiMessage> getError() { return _error; }
    public LiveData<Boolean> isLoading() { return _loading; }

    public void load(long activityId) {
        _loading.setValue(true);
        tourRepository.getActivityDetail(activityId, new RepositoryCallback<ActivityDetailResponse>() {
            @Override
            public void onSuccess(ActivityDetailResponse data) {
                _loading.setValue(false);
                
                List<ActivitySessionResponse> sessions = data.sessions != null ? data.sessions : new ArrayList<>();
                
                // Mock para "Free tour por telmo"
                if (data.name != null && data.name.toLowerCase().contains("telmo")) {
                    if (sessions.isEmpty()) {
                        sessions = createMockSessions(data.id, data.basePrice);
                    }
                }
                
                _sessions.setValue(sessions);
                _activity.setValue(mapToTourActivity(data));
            }

            @Override
            public void onError(UiMessage error) {
                _loading.setValue(false);
                _error.setValue(error != null ? error : UiMessage.from(R.string.error_network_generic));
            }
        });
    }

    private List<ActivitySessionResponse> createMockSessions(Long activityId, Double price) {
        List<ActivitySessionResponse> mocks = new ArrayList<>();
        // Usamos LocalDateTime.now() asumiendo que el dispositivo tiene API 26+ 
        // o desugaring habilitado.
        LocalDateTime now = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        
        for (int i = 0; i < 3; i++) {
            ActivitySessionResponse s = new ActivitySessionResponse();
            s.id = 2000L + i; // ID temporal para evitar colisiones
            s.startTime = now.plusDays(i).toString();
            s.availableSpots = 10;
            s.price = price != null ? price : 0.0;
            mocks.add(s);
        }
        return mocks;
    }

    @Override
    protected void onCleared() {
        tourRepository.cancelAll();
        super.onCleared();
    }

    private static TourActivity mapToTourActivity(ActivityDetailResponse data) {
        String destination = data.destination != null ? safe(data.destination.name) : "";
        String category = data.category != null ? data.category.replace("_", " ") : "";
        String duration = FormatUtils.formatDuration(data.durationMinutes);
        String price = FormatUtils.formatPrice(data.basePrice, data.currency);
        String guideName = data.guide != null ? safe(data.guide.fullName) : null;
        float rating = data.avgRating != null ? data.avgRating.floatValue() : 0f;
        int reviewCount = data.reviewCount != null ? data.reviewCount.intValue() : 0;

        TourActivity activity = new TourActivity(
                safe(data.name),
                destination,
                category,
                duration,
                price,
                data.availableSpots,
                null,
                safe(data.description),
                rating,
                reviewCount,
                safe(data.includesText),
                safe(data.meetingPoint),
                guideName,
                safe(data.language),
                safe(data.cancellationPolicy),
                null
        );
        activity.setId(data.id);
        return activity;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

}
