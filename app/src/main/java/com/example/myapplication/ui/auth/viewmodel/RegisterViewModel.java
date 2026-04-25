package com.example.myapplication.ui.auth.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.data.usecase.RegisterUseCase;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class RegisterViewModel extends ViewModel {

    public static final class UiState {
        public final boolean isLoading;
        @Nullable public final UiMessage error;
        public final boolean navigateToOtpCode;

        private UiState(boolean isLoading, @Nullable UiMessage error, boolean navigateToOtpCode) {
            this.isLoading = isLoading;
            this.error = error;
            this.navigateToOtpCode = navigateToOtpCode;
        }

        static UiState idle() { return new UiState(false, null, false); }
        UiState loading() { return new UiState(true, null, false); }
        UiState success() { return new UiState(false, null, true); }
        UiState withError(UiMessage m) { return new UiState(false, m, false); }
        UiState errorConsumed() { return new UiState(isLoading, null, navigateToOtpCode); }
        UiState navigationConsumed() { return new UiState(isLoading, error, false); }
    }

    private final RegisterUseCase registerUseCase;
    private final MutableLiveData<UiState> _uiState = new MutableLiveData<>(UiState.idle());

    @Inject
    public RegisterViewModel(RegisterUseCase registerUseCase) {
        this.registerUseCase = registerUseCase;
    }

    public LiveData<UiState> getUiState() { return _uiState; }

    public void register(String email, String password, String firstName, String lastName, String phone) {
        UiState s = _uiState.getValue();
        if (s == null) return;
        _uiState.setValue(s.loading());
        registerUseCase.execute(email, password, firstName, lastName, phone,
                new RepositoryCallback<OtpResponse>() {
                    @Override
                    public void onSuccess(OtpResponse data) {
                        UiState cur = _uiState.getValue();
                        if (cur != null) _uiState.postValue(cur.success());
                    }

                    @Override
                    public void onError(UiMessage error) {
                        UiState cur = _uiState.getValue();
                        if (cur != null) _uiState.postValue(cur.withError(error));
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