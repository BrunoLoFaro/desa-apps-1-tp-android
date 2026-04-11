package com.example.myapplication.data.usecase;

import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.data.model.User;
import com.example.myapplication.data.repository.AuthRepository;
import com.example.myapplication.data.repository.SessionRepository;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Orchestrates the forgot-password flow:
 * request → resend → verify → confirm (with optional session persistence).
 */
@Singleton
public class ForgotPasswordUseCase {

    private final AuthRepository authRepository;
    private final SessionRepository sessionRepository;

    @Inject
    public ForgotPasswordUseCase(AuthRepository authRepository, SessionRepository sessionRepository) {
        this.authRepository = authRepository;
        this.sessionRepository = sessionRepository;
    }

    public void requestReset(String email, AuthRepository.Callback<OtpResponse> callback) {
        authRepository.requestPasswordReset(email, callback);
    }

    public void resendReset(String email, AuthRepository.Callback<OtpResponse> callback) {
        authRepository.resendPasswordReset(email, callback);
    }

    public void verifyCode(String email, String code, AuthRepository.Callback<OtpResponse> callback) {
        authRepository.verifyPasswordResetCode(email, code, callback);
    }

    public void confirmNewPassword(String email, String code, String password,
                                   AuthRepository.Callback<LoginResponse> callback) {
        authRepository.confirmPasswordReset(email, code, password,
                new AuthRepository.Callback<LoginResponse>() {
                    @Override
                    public void onSuccess(LoginResponse data) {
                        // If the backend returns a session token after reset, persist it
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
                    public void onError(UiMessage error) {
                        callback.onError(error);
                    }
                });
    }
}
