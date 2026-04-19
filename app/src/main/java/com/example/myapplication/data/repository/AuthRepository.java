package com.example.myapplication.data.repository;

import com.example.myapplication.R;
import com.example.myapplication.data.common.RepositoryCallback;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.LoginRequest;
import com.example.myapplication.data.model.OtpCodeVerificationRequest;
import com.example.myapplication.data.model.OtpRegistrationCompleteRequest;
import com.example.myapplication.data.model.OtpRequest;
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.data.model.PasswordResetConfirmRequest;
import com.example.myapplication.data.model.RegisterRequest;
import com.example.myapplication.data.network.AuthService;
import com.example.myapplication.util.NetworkErrorParser;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AuthRepository extends BaseRepository {

    private final AuthService authService;
    private final ConfigLoader configLoader;

    @Inject
    public AuthRepository(AuthService authService, ConfigLoader configLoader,
                          NetworkErrorParser errorParser) {
        super(errorParser);
        this.authService = authService;
        this.configLoader = configLoader;
    }

    public void login(String email, String password, RepositoryCallback<LoginResponse> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueue(authService.login(config.loginEndpoint, new LoginRequest(email, password)),
                callback, R.string.error_login_failed_default);
    }

    public void register(String email, String password, String firstName, String lastName,
                         String dni, RepositoryCallback<LoginResponse> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueue(authService.register(config.registerEndpoint,
                        new RegisterRequest(email, password, firstName, lastName, dni)),
                callback, R.string.error_register_failed_default);
    }

    public void requestSignupOtp(String email, RepositoryCallback<OtpResponse> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueue(authService.requestSignupOtp(config.signupOtpRequestEndpoint, new OtpRequest(email)),
                callback, R.string.error_signup_otp_request_default);
    }

    public void resendSignupOtp(String email, RepositoryCallback<OtpResponse> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueue(authService.resendSignupOtp(config.signupOtpResendEndpoint, new OtpRequest(email)),
                callback, R.string.error_signup_otp_resend_default);
    }

    public void verifySignupOtp(String email, String code, RepositoryCallback<OtpResponse> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueue(authService.verifySignupOtp(config.signupOtpVerifyEndpoint,
                        new OtpCodeVerificationRequest(email, code)),
                callback, R.string.error_signup_otp_verify_default);
    }

    public void completeSignupWithOtp(String email, String code, String password,
                                      String firstName, String lastName, String dni,
                                      RepositoryCallback<LoginResponse> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueue(authService.completeSignupWithOtp(config.signupOtpCompleteEndpoint,
                        new OtpRegistrationCompleteRequest(email, code, password, firstName, lastName, dni)),
                callback, R.string.error_signup_complete_default);
    }

    public void requestPasswordReset(String email, RepositoryCallback<OtpResponse> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueue(authService.requestPasswordReset(config.passwordResetRequestEndpoint, new OtpRequest(email)),
                callback, R.string.error_password_reset_request_default);
    }

    public void resendPasswordReset(String email, RepositoryCallback<OtpResponse> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueue(authService.resendPasswordReset(config.passwordResetResendEndpoint, new OtpRequest(email)),
                callback, R.string.error_password_reset_resend_default);
    }

    public void verifyPasswordResetCode(String email, String code, RepositoryCallback<OtpResponse> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueue(authService.verifyPasswordResetCode(config.passwordResetVerifyEndpoint,
                        new OtpCodeVerificationRequest(email, code)),
                callback, R.string.error_password_reset_verify_default);
    }

    public void confirmPasswordReset(String email, String code, String password,
                                     RepositoryCallback<LoginResponse> callback) {
        AppConfig config = getConfig(callback);
        if (config == null) return;
        enqueue(authService.confirmPasswordReset(config.passwordResetConfirmEndpoint,
                        new PasswordResetConfirmRequest(email, code, password)),
                callback, R.string.error_password_reset_confirm_default);
    }

    private <T> AppConfig getConfig(RepositoryCallback<T> callback) {
        AppConfig config = configLoader.loadConfig();
        if (config == null || !config.hasValidBaseUrl()) {
            callback.onError(UiMessage.from(R.string.error_invalid_config));
            return null;
        }
        return config;
    }
}
