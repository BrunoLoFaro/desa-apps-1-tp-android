package com.example.myapplication.ui.home;

import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.repository.SessionRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class HomeViewModel extends ViewModel {

    private final SessionRepository sessionRepository;

    @Inject
    public HomeViewModel(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public boolean hasValidSession() {
        return sessionRepository.hasValidSession();
    }

    public void logout() {
        sessionRepository.clearSession();
    }
}
