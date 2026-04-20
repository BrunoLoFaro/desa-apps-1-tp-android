package com.example.myapplication.data.usecase;

import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.User;
import com.example.myapplication.data.repository.AuthRepository;
import com.example.myapplication.data.repository.SessionRepository;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Orchestrates classic registration: calls AuthRepository and persists
 * the session on success.
 */
@Singleton
public class RegisterUseCase {

    private final AuthRepository authRepository;
    private final SessionRepository sessionRepository;

    @Inject
    public RegisterUseCase(AuthRepository authRepository, SessionRepository sessionRepository) {
        this.authRepository = authRepository;
        this.sessionRepository = sessionRepository;
    }

    public void cancel() {
        authRepository.cancelAll();
    }

    public void execute(String email, String password, String firstName, String lastName,
                        String dni, RepositoryCallback<LoginResponse> callback) {
        authRepository.register(email, password, firstName, lastName, dni,
                new RepositoryCallback<LoginResponse>() {
                    @Override
                    public void onSuccess(LoginResponse data) {
                        if (data.token != null && !data.token.trim().isEmpty()) {
                            User user = new User(
                                    data.userId != null ? data.userId : -1L,
                                    data.email, data.firstName, data.lastName
                            );
                            sessionRepository.saveSession(data.token, data.refreshToken, user);
                        }
                        callback.onSuccess(data);
                    }

                    @Override
                    public void onError(UiMessage error) {
                        callback.onError(error);
                    }
                });
    }
}
