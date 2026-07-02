package com.example.myapplication.data.usecase;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.data.repository.AuthRepository;
import com.example.myapplication.data.repository.SessionRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OtpSignupUseCaseTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private RepositoryCallback<OtpResponse> otpCallback;

    @Mock
    private RepositoryCallback<LoginResponse> loginCallback;

    private OtpSignupUseCase otpSignupUseCase;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        otpSignupUseCase = new OtpSignupUseCase(authRepository, sessionRepository);
    }

    // ── signup OTP ─────────────────────────────────────────────────────────────

    @Test
    public void resendOtp_callsRepository() {
        otpSignupUseCase.resendOtp("test@mail.com", otpCallback);
        verify(authRepository).resendSignupOtp(eq("test@mail.com"), eq(otpCallback));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifySignupOtp_onSuccessWithToken_savesSessionAndCallsBack() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        otpSignupUseCase.verifySignupOtp("test@mail.com", "123456", loginCallback);
        verify(authRepository).verifySignupOtp(eq("test@mail.com"), eq("123456"), captor.capture());

        LoginResponse response = new LoginResponse();
        response.token = "jwt-token";
        response.userId = 1L;
        response.email = "test@mail.com";
        response.firstName = "John";
        response.lastName = "Doe";
        captor.getValue().onSuccess(response);

        verify(sessionRepository).saveSession(eq("jwt-token"), any(), argThat(user ->
                user.id == 1L && "test@mail.com".equals(user.email)));
        verify(loginCallback).onSuccess(response);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifySignupOtp_onError_forwardsError() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        otpSignupUseCase.verifySignupOtp("test@mail.com", "wrong", loginCallback);
        verify(authRepository).verifySignupOtp(anyString(), anyString(), captor.capture());

        UiMessage errorMsg = UiMessage.from("Código inválido");
        captor.getValue().onError(errorMsg);

        verify(loginCallback).onError(errorMsg);
        verify(sessionRepository, never()).saveSession(anyString(), any(), any());
    }

    // ── login OTP ──────────────────────────────────────────────────────────────

    @Test
    public void sendLoginOtp_callsRepository() {
        otpSignupUseCase.sendLoginOtp("test@mail.com", otpCallback);
        verify(authRepository).sendLoginOtp(eq("test@mail.com"), eq(otpCallback));
    }

    @Test
    public void resendLoginOtp_callsRepository() {
        otpSignupUseCase.resendLoginOtp("test@mail.com", otpCallback);
        verify(authRepository).resendLoginOtp(eq("test@mail.com"), eq(otpCallback));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyLoginOtp_onSuccessWithToken_savesSessionAndCallsBack() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        otpSignupUseCase.verifyLoginOtp("active@mail.com", "123456", loginCallback);
        verify(authRepository).verifyLoginOtp(eq("active@mail.com"), eq("123456"), captor.capture());

        LoginResponse response = new LoginResponse();
        response.token = "jwt-token";
        response.userId = 2L;
        response.email = "active@mail.com";
        response.firstName = "Jane";
        response.lastName = "Doe";
        captor.getValue().onSuccess(response);

        verify(sessionRepository).saveSession(eq("jwt-token"), any(), argThat(user ->
                user.id == 2L && "active@mail.com".equals(user.email)));
        verify(loginCallback).onSuccess(response);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyLoginOtp_onError_forwardsError() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        otpSignupUseCase.verifyLoginOtp("active@mail.com", "wrong", loginCallback);
        verify(authRepository).verifyLoginOtp(anyString(), anyString(), captor.capture());

        UiMessage errorMsg = UiMessage.from("Código inválido");
        captor.getValue().onError(errorMsg);

        verify(loginCallback).onError(errorMsg);
        verify(sessionRepository, never()).saveSession(anyString(), any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyLoginOtp_onSuccessWithoutToken_doesNotSaveSession() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        otpSignupUseCase.verifyLoginOtp("active@mail.com", "123456", loginCallback);
        verify(authRepository).verifyLoginOtp(anyString(), anyString(), captor.capture());

        LoginResponse response = new LoginResponse();
        response.token = null;
        captor.getValue().onSuccess(response);

        verify(sessionRepository, never()).saveSession(anyString(), any(), any());
        verify(loginCallback).onSuccess(response);
    }

    @Test
    public void cancel_cancelsRepositoryCalls() {
        otpSignupUseCase.cancel();
        verify(authRepository).cancelAll();
    }
}