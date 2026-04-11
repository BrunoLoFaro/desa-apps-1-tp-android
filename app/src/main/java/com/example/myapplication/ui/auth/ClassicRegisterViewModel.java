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
import com.example.myapplication.data.usecase.RegisterUseCase;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class ClassicRegisterViewModel extends ViewModel {

    public static final class UiState {
        public final boolean isLoading;
        public final boolean configValid;
        @Nullable public final UiMessage error;
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

    private final RegisterUseCase registerUseCase;
    private final MutableLiveData<UiState> _uiState;

    @Inject
    public ClassicRegisterViewModel(RegisterUseCase registerUseCase, ConfigLoader configLoader) {
        this.registerUseCase = registerUseCase;
        AppConfig config = configLoader.loadConfig();
        boolean configValid = config != null && config.hasValidBaseUrl();
        _uiState = new MutableLiveData<>(UiState.idle(configValid));
        if (!configValid) {
            _uiState.setValue(UiState.idle(false).withError(UiMessage.from(R.string.error_invalid_config)));
        }
    }

    public LiveData<UiState> getUiState() { return _uiState; }

    public void register(String email, String password, String firstName,
                         String lastName, String dni) {
        UiState current = _uiState.getValue();
        if (current == null || !current.configValid) return;
        _uiState.setValue(current.loading());
        registerUseCase.execute(email, password, firstName, lastName, dni,
                new AuthRepository.Callback<LoginResponse>() {
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

    public void errorConsumed() {
        UiState s = _uiState.getValue();
        if (s != null) _uiState.setValue(s.errorConsumed());
    }

    public void navigationConsumed() {
        UiState s = _uiState.getValue();
        if (s != null) _uiState.setValue(s.navigationConsumed());
    }
}
