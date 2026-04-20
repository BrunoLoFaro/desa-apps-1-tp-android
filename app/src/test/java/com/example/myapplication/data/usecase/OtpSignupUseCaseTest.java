package com.example.myapplication.data.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @Test
    public void requestOtp_callsRepository() {
        otpSignupUseCase.requestOtp("test@mail.com", otpCallback);
        verify(authRepository).requestSignupOtp(eq("test@mail.com"), eq(otpCallback));
    }

    @Test
    public void resendOtp_callsRepository() {
        otpSignupUseCase.resendOtp("test@mail.com", otpCallback);
        verify(authRepository).resendSignupOtp(eq("test@mail.com"), eq(otpCallback));
    }

    @Test
    public void verifyOtp_callsRepository() {
        otpSignupUseCase.verifyOtp("test@mail.com", "123456", otpCallback);
        verify(authRepository).verifySignupOtp(eq("test@mail.com"), eq("123456"), eq(otpCallback));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void completeSignup_onSuccessWithToken_savesSessionAndCallsBack() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        otpSignupUseCase.completeSignup("test@mail.com", "123456", "pass123", "John", "Doe", "12345678", loginCallback);
        
        verify(authRepository).completeSignupWithOtp(
                eq("test@mail.com"), eq("123456"), eq("pass123"), eq("John"), eq("Doe"), eq("12345678"), captor.capture());

        LoginResponse response = new LoginResponse();
        response.token = "jwt-token";
        response.userId = 1L;
        response.email = "test@mail.com";
        response.firstName = "John";
        response.lastName = "Doe";
        
        captor.getValue().onSuccess(response);

        verify(sessionRepository).saveSession(eq("jwt-token"), any(), argThat(user ->
                user.id == 1L && "test@mail.com".equals(user.email)
        ));
        verify(loginCallback).onSuccess(response);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void completeSignup_onError_forwardsError() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        otpSignupUseCase.completeSignup("test@mail.com", "wrong", "pass", "J", "D", "1", loginCallback);
        verify(authRepository).completeSignupWithOtp(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), captor.capture());

        UiMessage errorMsg = UiMessage.from("Error");
        captor.getValue().onError(errorMsg);

        verify(loginCallback).onError(errorMsg);
        verify(sessionRepository, never()).saveSession(anyString(), any(), any());
    }

    @Test
    public void cancel_cancelsRepositoryCalls() {
        otpSignupUseCase.cancel();
        verify(authRepository).cancelAll();
    }
}
