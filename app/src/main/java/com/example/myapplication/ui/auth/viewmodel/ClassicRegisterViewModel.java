package com.example.myapplication.ui.auth.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.usecase.RegisterUseCase;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class ClassicRegisterViewModel extends ViewModel {

    public static final class UiState {
        public final boolean isLoading;
        @Nullable public final UiMessage error;
        public final boolean navigateToHome;

        private UiState(boolean isLoading,
                        @Nullable UiMessage error, boolean navigateToHome) {
            this.isLoading = isLoading;
            this.error = error;
            this.navigateToHome = navigateToHome;
        }

        static UiState idle() {
            return new UiState(false, null, false);
        }
        UiState loading() { return new UiState(true, null, false); }
        UiState success() { return new UiState(false, null, true); }
        UiState withError(UiMessage msg) { return new UiState(false, msg, false); }
        UiState errorConsumed() { return new UiState(isLoading, null, navigateToHome); }
        UiState navigationConsumed() { return new UiState(isLoading, error, false); }
    }

    private final RegisterUseCase registerUseCase;
    private final MutableLiveData<UiState> _uiState;

    @Inject
    public ClassicRegisterViewModel(RegisterUseCase registerUseCase) {
        this.registerUseCase = registerUseCase;
        _uiState = new MutableLiveData<>(UiState.idle());
    }

    public LiveData<UiState> getUiState() { return _uiState; }

    public void register(String email, String password, String firstName,
                         String lastName, String dni) {
        UiState current = _uiState.getValue();
        if (current == null) return;
        _uiState.setValue(current.loading());
        registerUseCase.execute(email, password, firstName, lastName, dni,
                new RepositoryCallback<LoginResponse>() {
                    @Override
                    public void onSuccess(LoginResponse data) {
                        UiState s = _uiState.getValue();
                        if (s != null) _uiState.postValue(s.success());
                    }

                    @Override
                    public void onError(UiMessage error) {
                        UiState s = _uiState.getValue();
                        if (s != null) _uiState.postValue(s.withError(error));
                    }
                });
    }

    public void errorConsumed() {
        UiState s = _uiState.getValue();
        if (s != null) _uiState.setValue(s.errorConsumed());
    }

    public void navigationConsumed() {
        UiState s = _uiState.getValue();
        if (s != null) _uiState.setValue(s.navigationConsumed());
    }

    @Override
    protected void onCleared() {
        registerUseCase.cancel();
        super.onCleared();
    }
}
