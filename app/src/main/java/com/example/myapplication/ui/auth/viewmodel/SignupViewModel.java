package com.example.myapplication.ui.auth.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.data.usecase.OtpSignupUseCase;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/**
 * ViewModel compartido (activity-scoped) para:
 * - Flujo "Ingresar con código de un solo uso": SignupFragment → OtpSignupCodeFragment
 * - Verificación OTP post-registro: OtpSignupCodeFragment
 */
@HiltViewModel
public class SignupViewModel extends ViewModel {

    // ─────────────── UiState para SignupFragment (enviar OTP de login) ────────

    public static final class SendOtpUiState {
        public final boolean isLoading;
        @Nullable public final UiMessage error;
        public final boolean navigateToOtpCode;

        private SendOtpUiState(boolean isLoading, @Nullable UiMessage error, boolean navigateToOtpCode) {
            this.isLoading = isLoading;
            this.error = error;
            this.navigateToOtpCode = navigateToOtpCode;
        }

        static SendOtpUiState idle() { return new SendOtpUiState(false, null, false); }
        SendOtpUiState loading() { return new SendOtpUiState(true, null, false); }
        SendOtpUiState navigateToCode() { return new SendOtpUiState(false, null, true); }
        SendOtpUiState withError(UiMessage m) { return new SendOtpUiState(false, m, false); }
        SendOtpUiState errorConsumed() { return new SendOtpUiState(isLoading, null, navigateToOtpCode); }
        SendOtpUiState navigationConsumed() { return new SendOtpUiState(isLoading, error, false); }
    }

    // ────────────────── UiState para OtpSignupCodeFragment ───────────────────

    public static final class OtpCodeUiState {
        public final boolean isLoading;
        @Nullable public final UiMessage error;
        public final boolean navigateToHome;
        public final boolean otpResent;

        private OtpCodeUiState(boolean isLoading, @Nullable UiMessage error,
                                boolean navigateToHome, boolean otpResent) {
            this.isLoading = isLoading;
            this.error = error;
            this.navigateToHome = navigateToHome;
            this.otpResent = otpResent;
        }

        static OtpCodeUiState idle() { return new OtpCodeUiState(false, null, false, false); }
        OtpCodeUiState loading() { return new OtpCodeUiState(true, null, false, false); }
        OtpCodeUiState navigateToHome() { return new OtpCodeUiState(false, null, true, false); }
        OtpCodeUiState resent() { return new OtpCodeUiState(false, null, false, true); }
        OtpCodeUiState withError(UiMessage m) { return new OtpCodeUiState(false, m, false, false); }
        OtpCodeUiState errorConsumed() { return new OtpCodeUiState(isLoading, null, navigateToHome, otpResent); }
        OtpCodeUiState navigationConsumed() { return new OtpCodeUiState(isLoading, error, false, false); }
        OtpCodeUiState resentConsumed() { return new OtpCodeUiState(isLoading, error, navigateToHome, false); }
    }

    // ──────────────────────────── ViewModel body ─────────────────────────────

    private final OtpSignupUseCase otpSignupUseCase;
    private final MutableLiveData<SendOtpUiState> _sendOtpState;
    private final MutableLiveData<OtpCodeUiState> _otpCodeState;

    @Inject
    public SignupViewModel(OtpSignupUseCase otpSignupUseCase) {
        this.otpSignupUseCase = otpSignupUseCase;
        _sendOtpState = new MutableLiveData<>(SendOtpUiState.idle());
        _otpCodeState = new MutableLiveData<>(OtpCodeUiState.idle());
    }

    public LiveData<SendOtpUiState> getSendOtpState() { return _sendOtpState; }
    public LiveData<OtpCodeUiState> getOtpCodeState() { return _otpCodeState; }

    // ── SignupFragment actions (OTP login email entry) ──

    /** Envía OTP a un usuario existente activo para login con código. */
    public void sendLoginOtp(String email) {
        SendOtpUiState s = _sendOtpState.getValue();
        if (s == null) return;
        _sendOtpState.setValue(s.loading());
        otpSignupUseCase.sendLoginOtp(email, new RepositoryCallback<OtpResponse>() {
            @Override
            public void onSuccess(OtpResponse data) {
                SendOtpUiState cur = _sendOtpState.getValue();
                if (cur != null) _sendOtpState.postValue(cur.navigateToCode());
            }
            @Override
            public void onError(UiMessage error) {
                SendOtpUiState cur = _sendOtpState.getValue();
                if (cur != null) _sendOtpState.postValue(cur.withError(error));
            }
        });
    }

    public void sendOtpErrorConsumed() {
        SendOtpUiState s = _sendOtpState.getValue();
        if (s != null) _sendOtpState.setValue(s.errorConsumed());
    }

    public void sendOtpNavigationConsumed() {
        SendOtpUiState s = _sendOtpState.getValue();
        if (s != null) _sendOtpState.setValue(s.navigationConsumed());
    }

    // ── OtpSignupCodeFragment actions (compartido por registro y OTP login) ──

    /** Verifica OTP de REGISTRO (POST /auth/signup/otp/verify → activa usuario + JWT). */
    public void verifySignupOtp(String email, String code) {
        OtpCodeUiState s = _otpCodeState.getValue();
        if (s == null) return;
        _otpCodeState.setValue(s.loading());
        otpSignupUseCase.verifySignupOtp(email, code, new RepositoryCallback<LoginResponse>() {
            @Override
            public void onSuccess(LoginResponse data) {
                OtpCodeUiState cur = _otpCodeState.getValue();
                if (cur != null) _otpCodeState.postValue(cur.navigateToHome());
            }
            @Override
            public void onError(UiMessage error) {
                OtpCodeUiState cur = _otpCodeState.getValue();
                if (cur != null) _otpCodeState.postValue(cur.withError(error));
            }
        });
    }

    /** Verifica OTP de LOGIN OTP (POST /auth/otp/verify → JWT). */
    public void verifyLoginOtp(String email, String code) {
        OtpCodeUiState s = _otpCodeState.getValue();
        if (s == null) return;
        _otpCodeState.setValue(s.loading());
        otpSignupUseCase.verifyLoginOtp(email, code, new RepositoryCallback<LoginResponse>() {
            @Override
            public void onSuccess(LoginResponse data) {
                OtpCodeUiState cur = _otpCodeState.getValue();
                if (cur != null) _otpCodeState.postValue(cur.navigateToHome());
            }
            @Override
            public void onError(UiMessage error) {
                OtpCodeUiState cur = _otpCodeState.getValue();
                if (cur != null) _otpCodeState.postValue(cur.withError(error));
            }
        });
    }

    public void resendSignupOtp(String email) {
        OtpCodeUiState s = _otpCodeState.getValue();
        if (s == null) return;
        _otpCodeState.setValue(s.loading());
        otpSignupUseCase.resendOtp(email, new RepositoryCallback<OtpResponse>() {
            @Override
            public void onSuccess(OtpResponse data) {
                OtpCodeUiState cur = _otpCodeState.getValue();
                if (cur != null) _otpCodeState.postValue(cur.resent());
            }
            @Override
            public void onError(UiMessage error) {
                OtpCodeUiState cur = _otpCodeState.getValue();
                if (cur != null) _otpCodeState.postValue(cur.withError(error));
            }
        });
    }

    public void resendLoginOtp(String email) {
        OtpCodeUiState s = _otpCodeState.getValue();
        if (s == null) return;
        _otpCodeState.setValue(s.loading());
        otpSignupUseCase.resendLoginOtp(email, new RepositoryCallback<OtpResponse>() {
            @Override
            public void onSuccess(OtpResponse data) {
                OtpCodeUiState cur = _otpCodeState.getValue();
                if (cur != null) _otpCodeState.postValue(cur.resent());
            }
            @Override
            public void onError(UiMessage error) {
                OtpCodeUiState cur = _otpCodeState.getValue();
                if (cur != null) _otpCodeState.postValue(cur.withError(error));
            }
        });
    }

    public void otpCodeErrorConsumed() {
        OtpCodeUiState s = _otpCodeState.getValue();
        if (s != null) _otpCodeState.setValue(s.errorConsumed());
    }

    public void otpCodeNavigationConsumed() {
        OtpCodeUiState s = _otpCodeState.getValue();
        if (s != null) _otpCodeState.setValue(s.navigationConsumed());
    }

    public void otpResentConsumed() {
        OtpCodeUiState s = _otpCodeState.getValue();
        if (s != null) _otpCodeState.setValue(s.resentConsumed());
    }

    @Override
    protected void onCleared() {
        otpSignupUseCase.cancel();
        super.onCleared();
    }
}