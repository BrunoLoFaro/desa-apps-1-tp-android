package com.example.myapplication.data.usecase;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.data.repository.AuthRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RegisterUseCaseTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private RepositoryCallback<OtpResponse> outerCallback;

    private RegisterUseCase registerUseCase;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        registerUseCase = new RegisterUseCase(authRepository);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void execute_delegatesToRepository() {
        registerUseCase.execute("a@b.com", "123456", "Ana", "Lopez", "12345678", outerCallback);

        verify(authRepository).register(
                eq("a@b.com"), eq("123456"), eq("Ana"), eq("Lopez"), eq("12345678"),
                eq(outerCallback));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void execute_onSuccess_forwardsToCallback() {
        ArgumentCaptor<RepositoryCallback<OtpResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        registerUseCase.execute("a@b.com", "123456", "Ana", "Lopez", "12345678", outerCallback);
        verify(authRepository).register(anyString(), anyString(), anyString(), anyString(),
                anyString(), captor.capture());

        OtpResponse response = new OtpResponse();
        captor.getValue().onSuccess(response);

        verify(outerCallback).onSuccess(response);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void execute_onError_forwardsToCallback() {
        ArgumentCaptor<RepositoryCallback<OtpResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        registerUseCase.execute("a@b.com", "123456", "Ana", "Lopez", "12345678", outerCallback);
        verify(authRepository).register(anyString(), anyString(), anyString(), anyString(),
                anyString(), captor.capture());

        UiMessage errorMsg = UiMessage.from("Email ya registrado");
        captor.getValue().onError(errorMsg);

        verify(outerCallback).onError(errorMsg);
    }

    @Test
    public void cancel_cancelsRepositoryCalls() {
        registerUseCase.cancel();
        verify(authRepository).cancelAll();
    }
}