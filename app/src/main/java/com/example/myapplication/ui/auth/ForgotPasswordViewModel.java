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
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.data.repository.AuthRepository;
import com.example.myapplication.data.usecase.ForgotPasswordUseCase;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/**
 * Activity-scoped ViewModel for the forgot-password flow.
 * Shared by ForgotPasswordRequestFragment, ForgotPasswordCodeFragment,
 * and ForgotPasswordNewPasswordFragment.
 */
@HiltViewModel
public class ForgotPasswordViewModel extends ViewModel {

    // ─────────────── UiState for ForgotPasswordRequestFragment ──────────────

    public static final class RequestUiState {
        public final boolean isLoading;
        public final boolean configValid;
        @Nullable public final UiMessage error;
        public final boolean navigateToCode;

        private RequestUiState(boolean isLoading, boolean configValid,
                                @Nullable UiMessage error, boolean navigateToCode) {
            this.isLoading = isLoading;
            this.configValid = configValid;
            this.error = error;
            this.navigateToCode = navigateToCode;
        }

        static RequestUiState idle(boolean configValid) {
            return new RequestUiState(false, configValid, null, false);
        }
        RequestUiState loading() { return new RequestUiState(true, configValid, null, false); }
        RequestUiState navigateToCode() { return new RequestUiState(false, configValid, null, true); }
        RequestUiState withError(UiMessage m) { return new RequestUiState(false, configValid, m, false); }
        RequestUiState errorConsumed() { return new RequestUiState(isLoading, configValid, null, navigateToCode); }
        RequestUiState navigationConsumed() { return new RequestUiState(isLoading, configValid, error, false); }
    }

    // ─────────────── UiState for ForgotPasswordCodeFragment ─────────────────

    public static final class CodeUiState {
        public final boolean isLoading;
        @Nullable public final UiMessage error;
        public final boolean navigateToNewPassword;
        public final boolean codeResent;

        private CodeUiState(boolean isLoading, @Nullable UiMessage error,
                             boolean navigateToNewPassword, boolean codeResent) {
            this.isLoading = isLoading;
            this.error = error;
            this.navigateToNewPassword = navigateToNewPassword;
            this.codeResent = codeResent;
        }

        static CodeUiState idle() { return new CodeUiState(false, null, false, false); }
        CodeUiState loading() { return new CodeUiState(true, null, false, false); }
        CodeUiState navigateToNewPassword() { return new CodeUiState(false, null, true, false); }
        CodeUiState resent() { return new CodeUiState(false, null, false, true); }
        CodeUiState withError(UiMessage m) { return new CodeUiState(false, m, false, false); }
        CodeUiState errorConsumed() { return new CodeUiState(isLoading, null, navigateToNewPassword, codeResent); }
        CodeUiState navigationConsumed() { return new CodeUiState(isLoading, error, false, codeResent); }
        CodeUiState resentConsumed() { return new CodeUiState(isLoading, error, navigateToNewPassword, false); }
    }

    // ─────────────── UiState for ForgotPasswordNewPasswordFragment ───────────

    public static final class NewPasswordUiState {
        public final boolean isLoading;
        @Nullable public final UiMessage error;
        public final boolean navigateToHome;

        private NewPasswordUiState(boolean isLoading, @Nullable UiMessage error, boolean navigateToHome) {
            this.isLoading = isLoading;
            this.error = error;
            this.navigateToHome = navigateToHome;
        }

        static NewPasswordUiState idle() { return new NewPasswordUiState(false, null, false); }
        NewPasswordUiState loading() { return new NewPasswordUiState(true, null, false); }
        NewPasswordUiState success() { return new NewPasswordUiState(false, null, true); }
        NewPasswordUiState withError(UiMessage m) { return new NewPasswordUiState(false, m, false); }
        NewPasswordUiState errorConsumed() { return new NewPasswordUiState(isLoading, null, navigateToHome); }
        NewPasswordUiState navigationConsumed() { return new NewPasswordUiState(isLoading, error, false); }
    }

    // ─────────────────────────── ViewModel body ─────────────────────────────

    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final MutableLiveData<RequestUiState> _requestState;
    private final MutableLiveData<CodeUiState> _codeState = new MutableLiveData<>(CodeUiState.idle());
    private final MutableLiveData<NewPasswordUiState> _newPasswordState = new MutableLiveData<>(NewPasswordUiState.idle());

    @Inject
    public ForgotPasswordViewModel(ForgotPasswordUseCase forgotPasswordUseCase, ConfigLoader configLoader) {
        this.forgotPasswordUseCase = forgotPasswordUseCase;
        AppConfig config = configLoader.loadConfig();
        boolean configValid = config != null && config.hasValidBaseUrl();
        _requestState = new MutableLiveData<>(RequestUiState.idle(configValid));
        if (!configValid) {
            _requestState.setValue(RequestUiState.idle(false).withError(
                    UiMessage.from(R.string.error_invalid_config)));
        }
    }

    public LiveData<RequestUiState> getRequestState() { return _requestState; }
    public LiveData<CodeUiState> getCodeState() { return _codeState; }
    public LiveData<NewPasswordUiState> getNewPasswordState() { return _newPasswordState; }

    // ── ForgotPasswordRequestFragment actions ──

    public void requestReset(String email) {
        RequestUiState s = _requestState.getValue();
        if (s == null || !s.configValid) return;
        _requestState.setValue(s.loading());
        forgotPasswordUseCase.requestReset(email, new AuthRepository.Callback<OtpResponse>() {
            @Override
            public void onSuccess(OtpResponse data) {
                RequestUiState cur = _requestState.getValue();
                if (cur != null) _requestState.postValue(cur.navigateToCode());
            }
            @Override
            public void onError(UiMessage error) {
                RequestUiState cur = _requestState.getValue();
                if (cur != null) _requestState.postValue(cur.withError(error));
            }
        });
    }

    public void requestErrorConsumed() {
        RequestUiState s = _requestState.getValue();
        if (s != null) _requestState.setValue(s.errorConsumed());
    }

    public void requestNavigationConsumed() {
        RequestUiState s = _requestState.getValue();
        if (s != null) _requestState.setValue(s.navigationConsumed());
    }

    // ── ForgotPasswordCodeFragment actions ──

    public void verifyResetCode(String email, String code) {
        CodeUiState s = _codeState.getValue();
        if (s == null) return;
        _codeState.setValue(s.loading());
        forgotPasswordUseCase.verifyCode(email, code, new AuthRepository.Callback<OtpResponse>() {
            @Override
            public void onSuccess(OtpResponse data) {
                CodeUiState cur = _codeState.getValue();
                if (cur != null) _codeState.postValue(cur.navigateToNewPassword());
            }
            @Override
            public void onError(UiMessage error) {
                CodeUiState cur = _codeState.getValue();
                if (cur != null) _codeState.postValue(cur.withError(error));
            }
        });
    }

    public void resendReset(String email) {
        CodeUiState s = _codeState.getValue();
        if (s == null) return;
        _codeState.setValue(s.loading());
        forgotPasswordUseCase.resendReset(email, new AuthRepository.Callback<OtpResponse>() {
            @Override
            public void onSuccess(OtpResponse data) {
                CodeUiState cur = _codeState.getValue();
                if (cur != null) _codeState.postValue(cur.resent());
            }
            @Override
            public void onError(UiMessage error) {
                CodeUiState cur = _codeState.getValue();
                if (cur != null) _codeState.postValue(cur.withError(error));
            }
        });
    }

    public void codeErrorConsumed() {
        CodeUiState s = _codeState.getValue();
        if (s != null) _codeState.setValue(s.errorConsumed());
    }

    public void codeNavigationConsumed() {
        CodeUiState s = _codeState.getValue();
        if (s != null) _codeState.setValue(s.navigationConsumed());
    }

    public void codeResentConsumed() {
        CodeUiState s = _codeState.getValue();
        if (s != null) _codeState.setValue(s.resentConsumed());
    }

    // ── ForgotPasswordNewPasswordFragment actions ──

    public void confirmNewPassword(String email, String code, String password) {
        NewPasswordUiState s = _newPasswordState.getValue();
        if (s == null) return;
        _newPasswordState.setValue(s.loading());
        forgotPasswordUseCase.confirmNewPassword(email, code, password,
                new AuthRepository.Callback<LoginResponse>() {
                    @Override
                    public void onSuccess(LoginResponse data) {
                        NewPasswordUiState cur = _newPasswordState.getValue();
                        if (cur != null) _newPasswordState.postValue(cur.success());
                    }
                    @Override
                    public void onError(UiMessage error) {
                        NewPasswordUiState cur = _newPasswordState.getValue();
                        if (cur != null) _newPasswordState.postValue(cur.withError(error));
                    }
                });
    }

    public void newPasswordErrorConsumed() {
        NewPasswordUiState s = _newPasswordState.getValue();
        if (s != null) _newPasswordState.setValue(s.errorConsumed());
    }

    public void newPasswordNavigationConsumed() {
        NewPasswordUiState s = _newPasswordState.getValue();
        if (s != null) _newPasswordState.setValue(s.navigationConsumed());
    }
}
