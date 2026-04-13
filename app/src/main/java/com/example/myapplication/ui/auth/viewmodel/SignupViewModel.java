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
 * Activity-scoped ViewModel for the OTP signup flow.
 * Shared by SignupFragment, OtpSignupCodeFragment, and OtpSignupCompleteFragment.
 *
 * Exposes three separate LiveData streams — one per screen — so each Fragment
 * observes only the state relevant to it.
 */
@HiltViewModel
public class SignupViewModel extends ViewModel {

    // ─────────────── UiState for SignupFragment (request OTP) ───────────────

    public static final class RequestOtpUiState {
        public final boolean isLoading;
        @Nullable public final UiMessage error;
        public final boolean navigateToOtpCode;

        private RequestOtpUiState(boolean isLoading,
                                   @Nullable UiMessage error, boolean navigateToOtpCode) {
            this.isLoading = isLoading;
            this.error = error;
            this.navigateToOtpCode = navigateToOtpCode;
        }

        static RequestOtpUiState idle() {
            return new RequestOtpUiState(false, null, false);
        }
        RequestOtpUiState loading() { return new RequestOtpUiState(true, null, false); }
        RequestOtpUiState navigateToCode() { return new RequestOtpUiState(false, null, true); }
        RequestOtpUiState withError(UiMessage m) { return new RequestOtpUiState(false, m, false); }
        RequestOtpUiState errorConsumed() { return new RequestOtpUiState(isLoading, null, navigateToOtpCode); }
        RequestOtpUiState navigationConsumed() { return new RequestOtpUiState(isLoading, error, false); }
    }

    // ──────────────── UiState for OtpSignupCodeFragment ─────────────────────

    public static final class OtpCodeUiState {
        public final boolean isLoading;
        @Nullable public final UiMessage error;
        public final boolean navigateToComplete;
        public final boolean otpResent;

        private OtpCodeUiState(boolean isLoading, @Nullable UiMessage error,
                                boolean navigateToComplete, boolean otpResent) {
            this.isLoading = isLoading;
            this.error = error;
            this.navigateToComplete = navigateToComplete;
            this.otpResent = otpResent;
        }

        static OtpCodeUiState idle() { return new OtpCodeUiState(false, null, false, false); }
        OtpCodeUiState loading() { return new OtpCodeUiState(true, null, false, false); }
        OtpCodeUiState navigateToComplete() { return new OtpCodeUiState(false, null, true, false); }
        OtpCodeUiState resent() { return new OtpCodeUiState(false, null, false, true); }
        OtpCodeUiState withError(UiMessage m) { return new OtpCodeUiState(false, m, false, false); }
        OtpCodeUiState errorConsumed() { return new OtpCodeUiState(isLoading, null, navigateToComplete, otpResent); }
        OtpCodeUiState navigationConsumed() { return new OtpCodeUiState(isLoading, error, false, false); }
        OtpCodeUiState resentConsumed() { return new OtpCodeUiState(isLoading, error, navigateToComplete, false); }
    }

    // ─────────────── UiState for OtpSignupCompleteFragment ──────────────────

    public static final class OtpCompleteUiState {
        public final boolean isLoading;
        @Nullable public final UiMessage error;
        public final boolean navigateToHome;

        private OtpCompleteUiState(boolean isLoading, @Nullable UiMessage error, boolean navigateToHome) {
            this.isLoading = isLoading;
            this.error = error;
            this.navigateToHome = navigateToHome;
        }

        static OtpCompleteUiState idle() { return new OtpCompleteUiState(false, null, false); }
        OtpCompleteUiState loading() { return new OtpCompleteUiState(true, null, false); }
        OtpCompleteUiState success() { return new OtpCompleteUiState(false, null, true); }
        OtpCompleteUiState withError(UiMessage m) { return new OtpCompleteUiState(false, m, false); }
        OtpCompleteUiState errorConsumed() { return new OtpCompleteUiState(isLoading, null, navigateToHome); }
        OtpCompleteUiState navigationConsumed() { return new OtpCompleteUiState(isLoading, error, false); }
    }

    // ─────────────────────────── ViewModel body ─────────────────────────────

    private final OtpSignupUseCase otpSignupUseCase;
    private final MutableLiveData<RequestOtpUiState> _requestOtpState;
    private final MutableLiveData<OtpCodeUiState> _otpCodeState = new MutableLiveData<>(OtpCodeUiState.idle());
    private final MutableLiveData<OtpCompleteUiState> _otpCompleteState = new MutableLiveData<>(OtpCompleteUiState.idle());

    @Inject
    public SignupViewModel(OtpSignupUseCase otpSignupUseCase) {
        this.otpSignupUseCase = otpSignupUseCase;
        _requestOtpState = new MutableLiveData<>(RequestOtpUiState.idle());
    }

    public LiveData<RequestOtpUiState> getRequestOtpState() { return _requestOtpState; }
    public LiveData<OtpCodeUiState> getOtpCodeState() { return _otpCodeState; }
    public LiveData<OtpCompleteUiState> getOtpCompleteState() { return _otpCompleteState; }

    // ── SignupFragment actions ──

    public void requestSignupOtp(String email) {
        RequestOtpUiState s = _requestOtpState.getValue();
        if (s == null) return;
        _requestOtpState.setValue(s.loading());
        otpSignupUseCase.requestOtp(email, new RepositoryCallback<OtpResponse>() {
            @Override
            public void onSuccess(OtpResponse data) {
                RequestOtpUiState cur = _requestOtpState.getValue();
                if (cur != null) _requestOtpState.postValue(cur.navigateToCode());
            }
            @Override
            public void onError(UiMessage error) {
                RequestOtpUiState cur = _requestOtpState.getValue();
                if (cur != null) _requestOtpState.postValue(cur.withError(error));
            }
        });
    }

    public void requestOtpErrorConsumed() {
        RequestOtpUiState s = _requestOtpState.getValue();
        if (s != null) _requestOtpState.setValue(s.errorConsumed());
    }

    public void requestOtpNavigationConsumed() {
        RequestOtpUiState s = _requestOtpState.getValue();
        if (s != null) _requestOtpState.setValue(s.navigationConsumed());
    }

    // ── OtpSignupCodeFragment actions ──

    public void verifySignupOtp(String email, String code) {
        OtpCodeUiState s = _otpCodeState.getValue();
        if (s == null) return;
        _otpCodeState.setValue(s.loading());
        otpSignupUseCase.verifyOtp(email, code, new RepositoryCallback<OtpResponse>() {
            @Override
            public void onSuccess(OtpResponse data) {
                OtpCodeUiState cur = _otpCodeState.getValue();
                if (cur != null) _otpCodeState.postValue(cur.navigateToComplete());
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

    // ── OtpSignupCompleteFragment actions ──

    public void completeSignupWithOtp(String email, String code, String password,
                                      String firstName, String lastName, String dni) {
        OtpCompleteUiState s = _otpCompleteState.getValue();
        if (s == null) return;
        _otpCompleteState.setValue(s.loading());
        otpSignupUseCase.completeSignup(email, code, password, firstName, lastName, dni,
                new RepositoryCallback<LoginResponse>() {
                    @Override
                    public void onSuccess(LoginResponse data) {
                        OtpCompleteUiState cur = _otpCompleteState.getValue();
                        if (cur != null) _otpCompleteState.postValue(cur.success());
                    }
                    @Override
                    public void onError(UiMessage error) {
                        OtpCompleteUiState cur = _otpCompleteState.getValue();
                        if (cur != null) _otpCompleteState.postValue(cur.withError(error));
                    }
                });
    }

    public void otpCompleteErrorConsumed() {
        OtpCompleteUiState s = _otpCompleteState.getValue();
        if (s != null) _otpCompleteState.setValue(s.errorConsumed());
    }

    public void otpCompleteNavigationConsumed() {
        OtpCompleteUiState s = _otpCompleteState.getValue();
        if (s != null) _otpCompleteState.setValue(s.navigationConsumed());
    }

    @Override
    protected void onCleared() {
        otpSignupUseCase.cancel();
        super.onCleared();
    }
}
