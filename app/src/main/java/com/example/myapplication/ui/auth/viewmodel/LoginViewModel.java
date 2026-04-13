package com.example.myapplication.ui.auth.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.repository.SessionRepository;
import com.example.myapplication.data.usecase.LoginUseCase;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class LoginViewModel extends ViewModel {

    /** Immutable UI state for the login screen. */
    public static final class UiState {
        public final boolean isLoading;
        @Nullable public final UiMessage error;
        public final boolean navigateToHome;

        private UiState(boolean isLoading, @Nullable UiMessage error, boolean navigateToHome) {
            this.isLoading = isLoading;
            this.error = error;
            this.navigateToHome = navigateToHome;
        }

        static UiState idle() { return new UiState(false, null, false); }
        UiState loading() { return new UiState(true, null, false); }
        UiState success() { return new UiState(false, null, true); }
        UiState withError(UiMessage msg) { return new UiState(false, msg, false); }
        UiState errorConsumed() { return new UiState(isLoading, null, navigateToHome); }
        UiState navigationConsumed() { return new UiState(isLoading, error, false); }
    }

    private final LoginUseCase loginUseCase;
    private final SessionRepository sessionRepository;
    private final MutableLiveData<UiState> _uiState;

    @Inject
    public LoginViewModel(LoginUseCase loginUseCase, SessionRepository sessionRepository) {
        this.loginUseCase = loginUseCase;
        this.sessionRepository = sessionRepository;
        _uiState = new MutableLiveData<>(UiState.idle());
    }

    public LiveData<UiState> getUiState() { return _uiState; }

    public boolean hasValidSession() {
        return sessionRepository.hasValidSession();
    }

    public void login(String email, String password) {
        UiState current = _uiState.getValue();
        if (current == null) return;
        _uiState.setValue(current.loading());
        loginUseCase.execute(email, password, new RepositoryCallback<LoginResponse>() {
            @Override
            public void onSuccess(LoginResponse data) {
                UiState s = _uiState.getValue();
                if (s != null) _uiState.postValue(s.success());
            }

            @Override
            public void onError(UiMessage error) {
                UiState s = _uiState.getValue();
                if (s != null) _uiState.postValue(s.withError(error));
            }
        });
    }

    /** Called by the Fragment after it has displayed and dismissed the error. */
    public void errorConsumed() {
        UiState s = _uiState.getValue();
        if (s != null) _uiState.setValue(s.errorConsumed());
    }

    /** Called by the Fragment after it has handled the navigation event. */
    public void navigationConsumed() {
        UiState s = _uiState.getValue();
        if (s != null) _uiState.setValue(s.navigationConsumed());
    }

    @Override
    protected void onCleared() {
        loginUseCase.cancel();
        super.onCleared();
    }
}
