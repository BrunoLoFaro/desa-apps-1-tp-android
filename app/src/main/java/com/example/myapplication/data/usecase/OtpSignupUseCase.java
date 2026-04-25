package com.example.myapplication.data.usecase;

import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.data.model.User;
import com.example.myapplication.data.repository.AuthRepository;
import com.example.myapplication.data.repository.SessionRepository;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Maneja el flujo OTP compartido entre registro y login con código de un solo uso.
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

    public void cancel() {
        authRepository.cancelAll();
    }

    /** Envía OTP para login (usuario ya registrado y activo). */
    public void sendLoginOtp(String email, RepositoryCallback<OtpResponse> callback) {
        authRepository.sendLoginOtp(email, callback);
    }

    public void resendOtp(String email, RepositoryCallback<OtpResponse> callback) {
        authRepository.resendSignupOtp(email, callback);
    }

    public void resendLoginOtp(String email, RepositoryCallback<OtpResponse> callback) {
        authRepository.resendLoginOtp(email, callback);
    }

    /** Verifica OTP de registro y crea sesión (el backend activa el usuario). */
    public void verifySignupOtp(String email, String code, RepositoryCallback<LoginResponse> callback) {
        authRepository.verifySignupOtp(email, code, new RepositoryCallback<LoginResponse>() {
            @Override
            public void onSuccess(LoginResponse data) {
                saveSession(data);
                callback.onSuccess(data);
            }

            @Override
            public void onError(UiMessage error) {
                callback.onError(error);
            }
        });
    }

    /** Verifica OTP de login y crea sesión. */
    public void verifyLoginOtp(String email, String code, RepositoryCallback<LoginResponse> callback) {
        authRepository.verifyLoginOtp(email, code, new RepositoryCallback<LoginResponse>() {
            @Override
            public void onSuccess(LoginResponse data) {
                saveSession(data);
                callback.onSuccess(data);
            }

            @Override
            public void onError(UiMessage error) {
                callback.onError(error);
            }
        });
    }

    private void saveSession(LoginResponse data) {
        if (data.token != null && !data.token.trim().isEmpty()) {
            User user = new User(
                    data.userId != null ? data.userId : -1L,
                    data.email, data.firstName, data.lastName
            );
            sessionRepository.saveSession(data.token, data.refreshToken, user);
        }
    }
}