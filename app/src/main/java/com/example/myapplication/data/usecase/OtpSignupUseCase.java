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
 * Orchestrates the OTP-based registration flow:
 * request → resend → verify → complete (with session persistence).
 */
@Singleton
public class OtpSignupUseCase {

    private final AuthRepository authRepository;
    private final SessionRepository sessionRepository;

    @Inject
    public OtpSignupUseCase(AuthRepository authRepository, SessionRepository sessionRepository) {
        this.authRepository = authRepository;
        this.sessionRepository = sessionRepository;
    }

    public void requestOtp(String email, AuthRepository.Callback<OtpResponse> callback) {
        authRepository.requestSignupOtp(email, callback);
    }

    public void resendOtp(String email, AuthRepository.Callback<OtpResponse> callback) {
        authRepository.resendSignupOtp(email, callback);
    }

    public void verifyOtp(String email, String code, AuthRepository.Callback<OtpResponse> callback) {
        authRepository.verifySignupOtp(email, code, callback);
    }

    public void completeSignup(String email, String code, String password,
                               String firstName, String lastName, String dni,
                               AuthRepository.Callback<LoginResponse> callback) {
        authRepository.completeSignupWithOtp(email, code, password, firstName, lastName, dni,
                new AuthRepository.Callback<LoginResponse>() {
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
                    public void onError(UiMessage error) {
                        callback.onError(error);
                    }
                });
    }
}
