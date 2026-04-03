package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Patterns;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.OtpRequest;
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.data.model.OtpVerificationRequest;
import com.example.myapplication.data.network.AuthService;
import com.example.myapplication.data.network.RetrofitClient;
import com.example.myapplication.data.session.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtpLoginActivity extends AppCompatActivity {

    private static final String STATE_REQUEST_COOLDOWN_END = "state_request_cooldown_end";
    private static final String STATE_RESEND_COOLDOWN_END = "state_resend_cooldown_end";
    private static final long OTP_COOLDOWN_MILLIS = 30_000L;

    private TextInputEditText emailEditText;
    private TextInputEditText otpEditText;
    private MaterialButton requestOtpButton;
    private MaterialButton verifyOtpButton;
    private MaterialButton resendOtpButton;
    private CircularProgressIndicator progressIndicator;
    private View coordinator;

    private AppConfig appConfig;
    private AuthService authService;
    private ConfigLoader configLoader;
    private SessionManager sessionManager;
    private CountDownTimer requestCooldownTimer;
    private CountDownTimer resendCooldownTimer;
    private boolean requestCooldownActive = false;
    private boolean resendCooldownActive = false;
    private boolean isLoading = false;
    private long requestCooldownEndElapsedMs = 0L;
    private long resendCooldownEndElapsedMs = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp_login);

        if (savedInstanceState != null) {
            requestCooldownEndElapsedMs = savedInstanceState.getLong(STATE_REQUEST_COOLDOWN_END, 0L);
            resendCooldownEndElapsedMs = savedInstanceState.getLong(STATE_RESEND_COOLDOWN_END, 0L);
        }

        configLoader = new ConfigLoader(this);
        sessionManager = new SessionManager(this);
        coordinator = findViewById(R.id.otp_coordinator);
        emailEditText = findViewById(R.id.otp_email_edit_text);
        otpEditText = findViewById(R.id.otp_code_edit_text);
        requestOtpButton = findViewById(R.id.request_otp_button);
        verifyOtpButton = findViewById(R.id.verify_otp_button);
        resendOtpButton = findViewById(R.id.resend_otp_button);
        progressIndicator = findViewById(R.id.otp_progress_indicator);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.otp_coordinator), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        requestOtpButton.setOnClickListener(v -> requestOtp());
        verifyOtpButton.setOnClickListener(v -> verifyOtp());
        resendOtpButton.setOnClickListener(v -> resendOtp());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConfiguration();
        restoreCooldownsIfNeeded();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(STATE_REQUEST_COOLDOWN_END, requestCooldownEndElapsedMs);
        outState.putLong(STATE_RESEND_COOLDOWN_END, resendCooldownEndElapsedMs);
        super.onSaveInstanceState(outState);
    }

    private void loadConfiguration() {
        try {
            appConfig = configLoader.loadConfig();
            if (appConfig != null) {
                if (appConfig.otpRequestEndpoint == null || appConfig.otpRequestEndpoint.trim().isEmpty()) {
                    appConfig.otpRequestEndpoint = "api/v1/auth/otp/request";
                }
                if (appConfig.otpVerifyEndpoint == null || appConfig.otpVerifyEndpoint.trim().isEmpty()) {
                    appConfig.otpVerifyEndpoint = "api/v1/auth/otp/verify";
                }
                if (appConfig.otpResendEndpoint == null || appConfig.otpResendEndpoint.trim().isEmpty()) {
                    appConfig.otpResendEndpoint = "api/v1/auth/otp/resend";
                }
            }

            if (appConfig != null && appConfig.baseUrl != null && !appConfig.baseUrl.isEmpty()) {
                authService = RetrofitClient.getClient(appConfig).create(AuthService.class);
                updateRequestButtonState();
                verifyOtpButton.setEnabled(true);
                updateResendButtonState();
            } else {
                showError(getString(R.string.error_config_load));
                requestOtpButton.setEnabled(false);
                verifyOtpButton.setEnabled(false);
                resendOtpButton.setEnabled(false);
            }
        } catch (Exception e) {
            showError(getString(R.string.error_invalid_config));
            requestOtpButton.setEnabled(false);
            verifyOtpButton.setEnabled(false);
            resendOtpButton.setEnabled(false);
            authService = null;
        }
    }

    private void requestOtp() {
        String email = emailEditText.getText().toString().trim();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.error_invalid_email));
            return;
        }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        if (appConfig.otpRequestEndpoint == null || appConfig.otpRequestEndpoint.isEmpty()) {
            showError(getString(R.string.error_otp_request_endpoint_missing));
            return;
        }

        setLoading(true);
        authService.requestOtp(appConfig.otpRequestEndpoint, new OtpRequest(email)).enqueue(new Callback<OtpResponse>() {
            @Override
            public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    showInfo(getString(R.string.otp_sent_message));
                    startRequestCooldown(OTP_COOLDOWN_MILLIS);
                    startResendCooldown(OTP_COOLDOWN_MILLIS);
                } else {
                    String errorMsg = response.message().isEmpty() ? "No se pudo solicitar OTP" : response.message();
                    showError(getString(R.string.register_failed, errorMsg));
                }
            }

            @Override
            public void onFailure(Call<OtpResponse> call, Throwable t) {
                setLoading(false);
                String message = t != null && t.getLocalizedMessage() != null
                        ? t.getLocalizedMessage()
                        : "Network error";
                showError(getString(R.string.generic_error, message));
            }
        });
    }

    private void verifyOtp() {
        String email = emailEditText.getText().toString().trim();
        String code = otpEditText.getText().toString().trim();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.error_invalid_email));
            return;
        }

        if (!code.matches("^\\d{6}$")) {
            showError(getString(R.string.error_invalid_otp));
            return;
        }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        if (appConfig.otpVerifyEndpoint == null || appConfig.otpVerifyEndpoint.isEmpty()) {
            showError(getString(R.string.error_otp_verify_endpoint_missing));
            return;
        }

        setLoading(true);
        OtpVerificationRequest request = new OtpVerificationRequest(email, code);
        authService.verifyOtp(appConfig.otpVerifyEndpoint, request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().token != null && !response.body().token.trim().isEmpty()) {
                    sessionManager.saveAccessToken(response.body().token);
                    Intent intent = new Intent(OtpLoginActivity.this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMsg = response.message().isEmpty() ? "No se pudo iniciar sesión con OTP" : response.message();
                    showError(getString(R.string.register_failed, errorMsg));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                String message = t != null && t.getLocalizedMessage() != null
                        ? t.getLocalizedMessage()
                        : "Network error";
                showError(getString(R.string.generic_error, message));
            }
        });
    }

    private void resendOtp() {
        String email = emailEditText.getText().toString().trim();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.error_invalid_email));
            return;
        }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        if (appConfig.otpResendEndpoint == null || appConfig.otpResendEndpoint.isEmpty()) {
            showError(getString(R.string.error_otp_resend_endpoint_missing));
            return;
        }

        setLoading(true);
        authService.resendOtp(appConfig.otpResendEndpoint, new OtpRequest(email)).enqueue(new Callback<OtpResponse>() {
            @Override
            public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    showInfo(getString(R.string.otp_resent_message));
                    startRequestCooldown(OTP_COOLDOWN_MILLIS);
                    startResendCooldown(OTP_COOLDOWN_MILLIS);
                } else {
                    String errorMsg = response.message().isEmpty() ? "No se pudo reenviar OTP" : response.message();
                    showError(getString(R.string.register_failed, errorMsg));
                }
            }

            @Override
            public void onFailure(Call<OtpResponse> call, Throwable t) {
                setLoading(false);
                String message = t != null && t.getLocalizedMessage() != null
                        ? t.getLocalizedMessage()
                        : "Network error";
                showError(getString(R.string.generic_error, message));
            }
        });
    }

    private void setLoading(boolean isLoading) {
        this.isLoading = isLoading;
        emailEditText.setEnabled(!isLoading);
        otpEditText.setEnabled(!isLoading);
        updateRequestButtonState();
        verifyOtpButton.setEnabled(!isLoading);
        updateResendButtonState();
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void startRequestCooldown(long durationMillis) {
        if (durationMillis <= 0) {
            requestCooldownActive = false;
            requestCooldownEndElapsedMs = 0L;
            updateRequestButtonState();
            return;
        }

        if (requestCooldownTimer != null) {
            requestCooldownTimer.cancel();
        }

        requestCooldownActive = true;
        requestCooldownEndElapsedMs = SystemClock.elapsedRealtime() + durationMillis;
        updateRequestButtonState();

        requestCooldownTimer = new CountDownTimer(durationMillis, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = Math.max(1, millisUntilFinished / 1_000);
                requestOtpButton.setText(getString(R.string.otp_action_request_countdown, secondsLeft));
            }

            @Override
            public void onFinish() {
                requestCooldownActive = false;
                requestCooldownEndElapsedMs = 0L;
                requestOtpButton.setText(R.string.otp_action_request);
                updateRequestButtonState();
            }
        }.start();
    }

    private void updateRequestButtonState() {
        boolean enabled = !isLoading && !requestCooldownActive;
        requestOtpButton.setEnabled(enabled);
        if (!requestCooldownActive) {
            requestOtpButton.setText(R.string.otp_action_request);
        }
    }

    private void startResendCooldown(long durationMillis) {
        if (durationMillis <= 0) {
            resendCooldownActive = false;
            resendCooldownEndElapsedMs = 0L;
            updateResendButtonState();
            return;
        }

        if (resendCooldownTimer != null) {
            resendCooldownTimer.cancel();
        }

        resendCooldownActive = true;
        resendCooldownEndElapsedMs = SystemClock.elapsedRealtime() + durationMillis;
        updateResendButtonState();

        resendCooldownTimer = new CountDownTimer(durationMillis, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = Math.max(1, millisUntilFinished / 1_000);
                resendOtpButton.setText(getString(R.string.otp_action_resend_countdown, secondsLeft));
            }

            @Override
            public void onFinish() {
                resendCooldownActive = false;
                resendCooldownEndElapsedMs = 0L;
                resendOtpButton.setText(R.string.otp_action_resend);
                updateResendButtonState();
            }
        }.start();
    }

    private void restoreCooldownsIfNeeded() {
        long now = SystemClock.elapsedRealtime();
        long requestRemaining = requestCooldownEndElapsedMs - now;
        long resendRemaining = resendCooldownEndElapsedMs - now;

        if (requestRemaining > 0) {
            startRequestCooldown(requestRemaining);
        } else {
            requestCooldownActive = false;
            requestCooldownEndElapsedMs = 0L;
            updateRequestButtonState();
        }

        if (resendRemaining > 0) {
            startResendCooldown(resendRemaining);
        } else {
            resendCooldownActive = false;
            resendCooldownEndElapsedMs = 0L;
            updateResendButtonState();
        }
    }

    private void updateResendButtonState() {
        boolean enabled = !isLoading && !resendCooldownActive;
        resendOtpButton.setEnabled(enabled);
        if (!resendCooldownActive) {
            resendOtpButton.setText(R.string.otp_action_resend);
        }
    }

    private void showError(String message) {
        Snackbar.make(coordinator, message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.error, getTheme()))
                .setTextColor(getResources().getColor(R.color.onError, getTheme()))
                .show();
    }

    private void showInfo(String message) {
        Snackbar.make(coordinator, message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (requestCooldownTimer != null) {
            requestCooldownTimer.cancel();
        }
        if (resendCooldownTimer != null) {
            resendCooldownTimer.cancel();
        }
        super.onDestroy();
    }
}
