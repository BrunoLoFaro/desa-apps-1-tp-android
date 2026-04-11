package com.example.myapplication.ui.auth;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.repository.SessionRepository;
import com.example.myapplication.data.usecase.LoginUseCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LoginViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private LoginUseCase loginUseCase;

    @Mock
    private SessionRepository sessionRepository;

    private LoginViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new LoginViewModel(loginUseCase, sessionRepository);
    }

    @Test
    public void initialState_isIdle() {
        LoginViewModel.UiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.isLoading);
        assertNull(state.error);
        assertFalse(state.navigateToHome);
    }

    @Test
    public void login_setsLoadingState() {
        viewModel.login("test@mail.com", "123456");

        // Capture the callback but don't invoke it yet — state should be loading
        LoginViewModel.UiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue(state.isLoading);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void login_onSuccess_setsNavigateToHome() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.login("test@mail.com", "123456");
        verify(loginUseCase).execute(eq("test@mail.com"), eq("123456"), captor.capture());

        LoginResponse response = new LoginResponse();
        response.token = "jwt-token";
        response.email = "test@mail.com";
        captor.getValue().onSuccess(response);

        LoginViewModel.UiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.isLoading);
        assertTrue(state.navigateToHome);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void login_onError_setsError() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.login("test@mail.com", "wrong");
        verify(loginUseCase).execute(eq("test@mail.com"), eq("wrong"), captor.capture());

        UiMessage errorMsg = UiMessage.from("Credenciales inválidas");
        captor.getValue().onError(errorMsg);

        LoginViewModel.UiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.isLoading);
        assertNotNull(state.error);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorConsumed_clearsError() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.login("test@mail.com", "wrong");
        verify(loginUseCase).execute(anyString(), anyString(), captor.capture());
        captor.getValue().onError(UiMessage.from("Error"));

        viewModel.errorConsumed();

        LoginViewModel.UiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertNull(state.error);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void navigationConsumed_clearsNavigateFlag() {
        ArgumentCaptor<RepositoryCallback<LoginResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.login("test@mail.com", "123456");
        verify(loginUseCase).execute(anyString(), anyString(), captor.capture());

        LoginResponse response = new LoginResponse();
        response.token = "token";
        captor.getValue().onSuccess(response);

        viewModel.navigationConsumed();

        LoginViewModel.UiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.navigateToHome);
    }

    @Test
    public void hasValidSession_delegatesToSessionRepository() {
        when(sessionRepository.hasValidSession()).thenReturn(true);
        assertTrue(viewModel.hasValidSession());

        when(sessionRepository.hasValidSession()).thenReturn(false);
        assertFalse(viewModel.hasValidSession());
    }
}
