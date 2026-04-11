package com.example.myapplication.data.repository;

import android.util.Log;
import com.example.myapplication.R;
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
import com.squareup.moshi.Moshi;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * Single source of truth for all authentication network calls.
 *
 * Fixes vs. previous version:
 * - Does NOT expose Retrofit's Call<T> to callers (bad practice #4).
 * - Accepts primitives; creates DTOs internally (bad practice #6).
 * - Loads AppConfig itself via ConfigLoader; Fragment/ViewModel never touch config (bad practice #5).
 * - @Singleton via Hilt — one instance for the whole app (bad practice #7, #8).
 */
@Singleton
public class AuthRepository {

    private static final String TAG = "AuthRepository";

    /**
     * Callback interface that the data layer returns results through.
     * The ViewModel receives either a success value or a UiMessage describing
     * the error — no Retrofit types leak upward.
     */
    public interface Callback<T> {
        void onSuccess(T data);
        void onError(UiMessage error);
    }

    private final ConfigLoader configLoader;
    private final OkHttpClient okHttpClient;

    private Retrofit retrofit;
    private String currentBaseUrl;
    private final List<Call<?>> activeCalls = new ArrayList<>();

    @Inject
    public AuthRepository(ConfigLoader configLoader, OkHttpClient okHttpClient) {
        this.configLoader = configLoader;
        this.okHttpClient = okHttpClient;
    }

    // ─────────────────────────── Auth operations ────────────────────────────

    public void login(String email, String password, Callback<LoginResponse> callback) {
        AuthService service = getService(callback);
        if (service == null) return;
        AppConfig config = configLoader.loadConfig();
        enqueue(service.login(config.loginEndpoint, new LoginRequest(email, password)),
                callback, R.string.error_login_failed_default);
    }

    public void register(String email, String password, String firstName, String lastName,
                         String dni, Callback<LoginResponse> callback) {
        AuthService service = getService(callback);
        if (service == null) return;
        AppConfig config = configLoader.loadConfig();
        enqueue(service.register(config.registerEndpoint,
                        new RegisterRequest(email, password, firstName, lastName, dni)),
                callback, R.string.error_register_failed_default);
    }

    public void requestSignupOtp(String email, Callback<OtpResponse> callback) {
        AuthService service = getService(callback);
        if (service == null) return;
        AppConfig config = configLoader.loadConfig();
        enqueue(service.requestSignupOtp(config.signupOtpRequestEndpoint, new OtpRequest(email)),
                callback, R.string.error_signup_otp_request_default);
    }

    public void resendSignupOtp(String email, Callback<OtpResponse> callback) {
        AuthService service = getService(callback);
        if (service == null) return;
        AppConfig config = configLoader.loadConfig();
        enqueue(service.resendSignupOtp(config.signupOtpResendEndpoint, new OtpRequest(email)),
                callback, R.string.error_signup_otp_resend_default);
    }

    public void verifySignupOtp(String email, String code, Callback<OtpResponse> callback) {
        AuthService service = getService(callback);
        if (service == null) return;
        AppConfig config = configLoader.loadConfig();
        enqueue(service.verifySignupOtp(config.signupOtpVerifyEndpoint,
                        new OtpCodeVerificationRequest(email, code)),
                callback, R.string.error_signup_otp_verify_default);
    }

    public void completeSignupWithOtp(String email, String code, String password,
                                      String firstName, String lastName, String dni,
                                      Callback<LoginResponse> callback) {
        AuthService service = getService(callback);
        if (service == null) return;
        AppConfig config = configLoader.loadConfig();
        enqueue(service.completeSignupWithOtp(config.signupOtpCompleteEndpoint,
                        new OtpRegistrationCompleteRequest(email, code, password, firstName, lastName, dni)),
                callback, R.string.error_signup_complete_default);
    }

    public void requestPasswordReset(String email, Callback<OtpResponse> callback) {
        AuthService service = getService(callback);
        if (service == null) return;
        AppConfig config = configLoader.loadConfig();
        enqueue(service.requestPasswordReset(config.passwordResetRequestEndpoint, new OtpRequest(email)),
                callback, R.string.error_password_reset_request_default);
    }

    public void resendPasswordReset(String email, Callback<OtpResponse> callback) {
        AuthService service = getService(callback);
        if (service == null) return;
        AppConfig config = configLoader.loadConfig();
        enqueue(service.resendPasswordReset(config.passwordResetResendEndpoint, new OtpRequest(email)),
                callback, R.string.error_password_reset_resend_default);
    }

    public void verifyPasswordResetCode(String email, String code, Callback<OtpResponse> callback) {
        AuthService service = getService(callback);
        if (service == null) return;
        AppConfig config = configLoader.loadConfig();
        enqueue(service.verifyPasswordResetCode(config.passwordResetVerifyEndpoint,
                        new OtpCodeVerificationRequest(email, code)),
                callback, R.string.error_password_reset_verify_default);
    }

    public void confirmPasswordReset(String email, String code, String password,
                                     Callback<LoginResponse> callback) {
        AuthService service = getService(callback);
        if (service == null) return;
        AppConfig config = configLoader.loadConfig();
        enqueue(service.confirmPasswordReset(config.passwordResetConfirmEndpoint,
                        new PasswordResetConfirmRequest(email, code, password)),
                callback, R.string.error_password_reset_confirm_default);
    }

    public void cancelAll() {
        Iterator<Call<?>> it = activeCalls.iterator();
        while (it.hasNext()) {
            Call<?> call = it.next();
            if (!call.isCanceled()) call.cancel();
            it.remove();
        }
    }

    // ──────────────────────────── Internal helpers ───────────────────────────

    /**
     * Builds or reuses a Retrofit instance based on the current AppConfig.
     * Rebuilds automatically when the base URL changes (e.g., user updated settings).
     * Returns null and calls callback.onError() if config is invalid.
     */
    private synchronized <T> AuthService getService(Callback<T> callback) {
        AppConfig config = configLoader.loadConfig();
        if (config == null || !config.hasValidBaseUrl()) {
            callback.onError(UiMessage.from(R.string.error_invalid_config));
            return null;
        }

        if (!config.baseUrl.equals(currentBaseUrl)) {
            Log.d(TAG, "Base URL cambió, reconstruyendo Retrofit: " + config.baseUrl);
            currentBaseUrl = config.baseUrl;
            retrofit = new Retrofit.Builder()
                    .baseUrl(config.baseUrl)
                    .addConverterFactory(MoshiConverterFactory.create(new Moshi.Builder().build()))
                    .client(okHttpClient)
                    .build();
        }

        return retrofit.create(AuthService.class);
    }

    /**
     * Enqueues a Retrofit call, tracks it for cancellation, and routes results
     * through the provided Callback — keeping all Retrofit types inside this class.
     */
    private <T> void enqueue(Call<T> call, Callback<T> callback, int fallbackErrorResId) {
        activeCalls.add(call);
        call.enqueue(new retrofit2.Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                activeCalls.remove(call);
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(NetworkErrorParser.getErrorMessage(response, fallbackErrorResId));
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                activeCalls.remove(call);
                callback.onError(NetworkErrorParser.getFailureMessage(t, R.string.error_network_generic));
            }
        });
    }
}
