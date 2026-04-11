package com.example.myapplication.ui.auth;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.R;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.repository.AuthRepository;
import com.example.myapplication.data.repository.SessionRepository;
import com.example.myapplication.data.usecase.LoginUseCase;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class LoginViewModel extends ViewModel {

    /** Immutable UI state for the login screen. */
    public static final class UiState {
        public final boolean isLoading;
        public final boolean configValid;
        /** Non-null when there is a pending error to display. Fragment clears it by calling errorConsumed(). */
        @Nullable public final UiMessage error;
        /** True when login succeeded and the Fragment should navigate to Home. */
        public final boolean navigateToHome;

        private UiState(boolean isLoading, boolean configValid,
                        @Nullable UiMessage error, boolean navigateToHome) {
            this.isLoading = isLoading;
            this.configValid = configValid;
            this.error = error;
            this.navigateToHome = navigateToHome;
        }

        static UiState idle(boolean configValid) {
            return new UiState(false, configValid, null, false);
        }
        UiState loading() { return new UiState(true, configValid, null, false); }
        UiState success() { return new UiState(false, configValid, null, true); }
        UiState withError(UiMessage msg) { return new UiState(false, configValid, msg, false); }
        UiState errorConsumed() { return new UiState(isLoading, configValid, null, navigateToHome); }
        UiState navigationConsumed() { return new UiState(isLoading, configValid, error, false); }
    }

    private final LoginUseCase loginUseCase;
    private final SessionRepository sessionRepository;
    private final MutableLiveData<UiState> _uiState;

    @Inject
    public LoginViewModel(LoginUseCase loginUseCase, SessionRepository sessionRepository,
                          ConfigLoader configLoader) {
        this.loginUseCase = loginUseCase;
        this.sessionRepository = sessionRepository;
        AppConfig config = configLoader.loadConfig();
        boolean configValid = config != null && config.hasValidBaseUrl();
        _uiState = new MutableLiveData<>(UiState.idle(configValid));
        if (!configValid) {
            _uiState.setValue(UiState.idle(false).withError(UiMessage.from(R.string.error_invalid_config)));
        }
    }

    public LiveData<UiState> getUiState() { return _uiState; }

    public boolean hasValidSession() {
        return sessionRepository.hasValidSession();
    }

    public void login(String email, String password) {
        UiState current = _uiState.getValue();
        if (current == null || !current.configValid) return;
        _uiState.setValue(current.loading());
        loginUseCase.execute(email, password, new AuthRepository.Callback<LoginResponse>() {
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
        // Callback-based API means no active calls to cancel here;
        // the repository handles lifecycle via its own cancelAll() if needed.
    }
}
