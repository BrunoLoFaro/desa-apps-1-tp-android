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

public class LoginUseCaseTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private RepositoryCallback<LoginResponse> outerCallback;

    private LoginUseCase loginUseCase;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        loginUseCase = new LoginUseCase(authRepository, sessionRepository);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void execute_onSuccessWithToken_savesSessionAndCallsBack() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        loginUseCase.execute("test@mail.com", "123456", outerCallback);
        verify(authRepository).login(eq("test@mail.com"), eq("123456"), captor.capture());

        LoginResponse response = new LoginResponse();
        response.token = "jwt-token";
        response.userId = 1L;
        response.email = "test@mail.com";
        response.firstName = "Test";
        response.lastName = "User";
        captor.getValue().onSuccess(response);

        verify(sessionRepository).saveSession(eq("jwt-token"), any(), argThat(user ->
                user.id == 1L
                        && "test@mail.com".equals(user.email)
                        && "Test".equals(user.firstName)
                        && "User".equals(user.lastName)
        ));
        verify(outerCallback).onSuccess(response);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void execute_onSuccessWithoutToken_doesNotSaveSession() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        loginUseCase.execute("test@mail.com", "123456", outerCallback);
        verify(authRepository).login(anyString(), anyString(), captor.capture());

        LoginResponse response = new LoginResponse();
        response.token = null;
        captor.getValue().onSuccess(response);

        verify(sessionRepository, never()).saveSession(anyString(), any(), any());
        verify(outerCallback).onSuccess(response);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void execute_onSuccessWithEmptyToken_doesNotSaveSession() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        loginUseCase.execute("test@mail.com", "123456", outerCallback);
        verify(authRepository).login(anyString(), anyString(), captor.capture());

        LoginResponse response = new LoginResponse();
        response.token = "   ";
        captor.getValue().onSuccess(response);

        verify(sessionRepository, never()).saveSession(anyString(), any(), any());
        verify(outerCallback).onSuccess(response);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void execute_onError_forwardsError() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        loginUseCase.execute("test@mail.com", "wrong", outerCallback);
        verify(authRepository).login(anyString(), anyString(), captor.capture());

        UiMessage errorMsg = UiMessage.from("Credenciales inválidas");
        captor.getValue().onError(errorMsg);

        verify(outerCallback).onError(errorMsg);
        verify(sessionRepository, never()).saveSession(anyString(), any(), any());
    }
}
