package com.example.myapplication.data.usecase;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.repository.AuthRepository;
import com.example.myapplication.data.repository.SessionRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RegisterUseCaseTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private RepositoryCallback<LoginResponse> outerCallback;

    private RegisterUseCase registerUseCase;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        registerUseCase = new RegisterUseCase(authRepository, sessionRepository);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void execute_onSuccessWithToken_savesSessionAndCallsBack() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        registerUseCase.execute("a@b.com", "123456", "Ana", "Lopez", "12345678", outerCallback);
        verify(authRepository).register(
                eq("a@b.com"), eq("123456"), eq("Ana"), eq("Lopez"), eq("12345678"),
                captor.capture());

        LoginResponse response = new LoginResponse();
        response.token = "jwt-token";
        response.userId = 2L;
        response.email = "a@b.com";
        response.firstName = "Ana";
        response.lastName = "Lopez";
        captor.getValue().onSuccess(response);

        verify(sessionRepository).saveSession(eq("jwt-token"), any(), argThat(user ->
                user.id == 2L
                        && "a@b.com".equals(user.email)
                        && "Ana".equals(user.firstName)
                        && "Lopez".equals(user.lastName)
        ));
        verify(outerCallback).onSuccess(response);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void execute_onSuccessWithoutToken_doesNotSaveSession() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        registerUseCase.execute("a@b.com", "123456", "Ana", "Lopez", "12345678", outerCallback);
        verify(authRepository).register(anyString(), anyString(), anyString(), anyString(),
                anyString(), captor.capture());

        LoginResponse response = new LoginResponse();
        response.token = null;
        captor.getValue().onSuccess(response);

        verify(sessionRepository, never()).saveSession(anyString(), any(), any());
        verify(outerCallback).onSuccess(response);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void execute_onError_forwardsError() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        registerUseCase.execute("a@b.com", "123456", "Ana", "Lopez", "12345678", outerCallback);
        verify(authRepository).register(anyString(), anyString(), anyString(), anyString(),
                anyString(), captor.capture());

        UiMessage errorMsg = UiMessage.from("Email ya registrado");
        captor.getValue().onError(errorMsg);

        verify(outerCallback).onError(errorMsg);
        verify(sessionRepository, never()).saveSession(anyString(), any(), any());
    }
}
