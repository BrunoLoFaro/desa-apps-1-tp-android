package com.example.myapplication.ui.home.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.model.TourActivity;
import com.example.myapplication.data.repository.SessionRepository;
import com.example.myapplication.data.repository.TourRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;
import javax.inject.Inject;

@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final SessionRepository sessionRepository;
    private final TourRepository tourRepository;

    private final MutableLiveData<List<TourActivity>> _featuredTours = new MutableLiveData<>();
    private final MutableLiveData<List<TourActivity>> _allTours = new MutableLiveData<>();

    @Inject
    public HomeViewModel(SessionRepository sessionRepository, TourRepository tourRepository) {
        this.sessionRepository = sessionRepository;
        this.tourRepository = tourRepository;
        loadTours();
    }

    public LiveData<List<TourActivity>> getFeaturedTours() { return _featuredTours; }
    public LiveData<List<TourActivity>> getAllTours() { return _allTours; }

    public boolean hasValidSession() {
        return sessionRepository.hasValidSession();
    }

    public void logout() {
        sessionRepository.clearSession();
    }

    private void loadTours() {
        _featuredTours.setValue(tourRepository.getFeaturedTours());
        _allTours.setValue(tourRepository.getAllTours());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
