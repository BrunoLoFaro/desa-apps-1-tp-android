package com.example.myapplication.data.usecase;

import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.User;
import com.example.myapplication.data.repository.AuthRepository;
import com.example.myapplication.data.repository.SessionRepository;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Orchestrates the login flow: calls AuthRepository, and on success persists
 * the session via SessionRepository before reporting back to the ViewModel.
 */
@Singleton
public class LoginUseCase {

    private final AuthRepository authRepository;
    private final SessionRepository sessionRepository;

    @Inject
    public LoginUseCase(AuthRepository authRepository, SessionRepository sessionRepository) {
        this.authRepository = authRepository;
        this.sessionRepository = sessionRepository;
    }

    public void execute(String email, String password, AuthRepository.Callback<LoginResponse> callback) {
        authRepository.login(email, password, new AuthRepository.Callback<LoginResponse>() {
            @Override
            public void onSuccess(LoginResponse data) {
                if (data.token != null && !data.token.trim().isEmpty()) {
                    User user = new User(
                            data.userId != null ? data.userId : -1L,
                            data.email, data.firstName, data.lastName
                    );
                    sessionRepository.saveSession(data.token, user);
                }
                callback.onSuccess(data);
            }

            @Override
            public void onError(com.example.myapplication.data.common.UiMessage error) {
                callback.onError(error);
            }
        });
    }
}
