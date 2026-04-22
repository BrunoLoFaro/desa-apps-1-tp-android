package com.example.myapplication.data.usecase;

import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.data.repository.AuthRepository;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Orquesta el registro clásico: crea usuario pendiente y dispara el OTP.
 */
@Singleton
public class RegisterUseCase {

    private final AuthRepository authRepository;

    @Inject
    public RegisterUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public void cancel() {
        authRepository.cancelAll();
    }

    public void execute(String email, String password, String firstName, String lastName,
                        String phone, RepositoryCallback<OtpResponse> callback) {
        authRepository.register(email, password, firstName, lastName, phone, callback);
    }
}