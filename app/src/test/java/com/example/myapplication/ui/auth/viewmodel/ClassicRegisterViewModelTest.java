package com.example.myapplication.ui.auth.viewmodel;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.data.usecase.RegisterUseCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ClassicRegisterViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private RegisterUseCase registerUseCase;

    private RegisterViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new RegisterViewModel(registerUseCase);
    }

    @Test
    public void initialState_isIdle() {
        RegisterViewModel.UiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.isLoading);
        assertNull(state.error);
        assertFalse(state.navigateToOtpCode);
    }

    @Test
    public void register_setsLoadingState() {
        viewModel.register("a@b.com", "123456", "Ana", "Lopez", "12345678");

        RegisterViewModel.UiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue(state.isLoading);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void register_onSuccess_setsNavigateToOtpCode() {
        ArgumentCaptor<RepositoryCallback<OtpResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.register("a@b.com", "123456", "Ana", "Lopez", "12345678");
        verify(registerUseCase).execute(
                eq("a@b.com"), eq("123456"), eq("Ana"), eq("Lopez"), eq("12345678"),
                captor.capture());

        OtpResponse response = new OtpResponse();
        response.email = "a@b.com";
        captor.getValue().onSuccess(response);

        RegisterViewModel.UiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.isLoading);
        assertTrue(state.navigateToOtpCode);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void register_onError_setsError() {
        ArgumentCaptor<RepositoryCallback<OtpResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.register("a@b.com", "123456", "Ana", "Lopez", "12345678");
        verify(registerUseCase).execute(anyString(), anyString(), anyString(), anyString(),
                anyString(), captor.capture());

        captor.getValue().onError(UiMessage.from("Email ya registrado"));

        RegisterViewModel.UiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.isLoading);
        assertNotNull(state.error);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void errorConsumed_clearsError() {
        ArgumentCaptor<RepositoryCallback<OtpResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.register("a@b.com", "123456", "Ana", "Lopez", "12345678");
        verify(registerUseCase).execute(anyString(), anyString(), anyString(), anyString(),
                anyString(), captor.capture());
        captor.getValue().onError(UiMessage.from("Error"));

        viewModel.errorConsumed();

        RegisterViewModel.UiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertNull(state.error);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void navigationConsumed_clearsNavigateFlag() {
        ArgumentCaptor<RepositoryCallback<OtpResponse>> captor =
                ArgumentCaptor.forClass(RepositoryCallback.class);

        viewModel.register("a@b.com", "123456", "Ana", "Lopez", "12345678");
        verify(registerUseCase).execute(anyString(), anyString(), anyString(), anyString(),
                anyString(), captor.capture());

        OtpResponse response = new OtpResponse();
        captor.getValue().onSuccess(response);

        viewModel.navigationConsumed();

        RegisterViewModel.UiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.navigateToOtpCode);
    }
}