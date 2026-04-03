package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.OtpRegistrationCompleteRequest;
import com.example.myapplication.data.model.OtpRequest;
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.data.model.PasswordResetConfirmRequest;
import com.example.myapplication.data.network.AuthService;
import com.example.myapplication.data.network.RetrofitClient;
import com.example.myapplication.data.session.SessionManager;
import com.example.myapplication.util.AuthEndpoints;
import com.example.myapplication.util.AuthInputValidator;
import com.example.myapplication.util.NetworkErrorParser;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtpLoginActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_PREFILL_EMAIL = "prefill_email";
    public static final String MODE_SIGNUP = "signup";
    public static final String MODE_PASSWORD_RESET = "password_reset";

    private static final String STATE_REQUEST_COOLDOWN_END = "state_request_cooldown_end";
    private static final String STATE_RESEND_COOLDOWN_END = "state_resend_cooldown_end";
    private static final long OTP_COOLDOWN_MILLIS = 30_000L;

    private MaterialToolbar toolbar;
    private MaterialTextView subtitleTextView;
    private TextInputLayout passwordLayout;
    private View firstNameLayout;
    private View lastNameLayout;
    private View dniLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText otpEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText firstNameEditText;
    private TextInputEditText lastNameEditText;
    private TextInputEditText dniEditText;
    private MaterialButton requestOtpButton;
    private MaterialButton submitButton;
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
    private String mode = MODE_PASSWORD_RESET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp_login);

        if (savedInstanceState != null) {
            requestCooldownEndElapsedMs = savedInstanceState.getLong(STATE_REQUEST_COOLDOWN_END, 0L);
            resendCooldownEndElapsedMs = savedInstanceState.getLong(STATE_RESEND_COOLDOWN_END, 0L);
        }

        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (!MODE_SIGNUP.equals(mode)) {
            mode = MODE_PASSWORD_RESET;
        }

        configLoader = new ConfigLoader(this);
        sessionManager = new SessionManager(this);
        coordinator = findViewById(R.id.otp_coordinator);
        toolbar = findViewById(R.id.toolbar);
        subtitleTextView = findViewById(R.id.otp_subtitle);
        passwordLayout = findViewById(R.id.password_layout);
        firstNameLayout = findViewById(R.id.first_name_layout);
        lastNameLayout = findViewById(R.id.last_name_layout);
        dniLayout = findViewById(R.id.dni_layout);
        emailEditText = findViewById(R.id.otp_email_edit_text);
        otpEditText = findViewById(R.id.otp_code_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        firstNameEditText = findViewById(R.id.first_name_edit_text);
        lastNameEditText = findViewById(R.id.last_name_edit_text);
        dniEditText = findViewById(R.id.dni_edit_text);
        requestOtpButton = findViewById(R.id.request_otp_button);
        submitButton = findViewById(R.id.verify_otp_button);
        resendOtpButton = findViewById(R.id.resend_otp_button);
        progressIndicator = findViewById(R.id.otp_progress_indicator);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.otp_coordinator), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String prefillEmail = getIntent().getStringExtra(EXTRA_PREFILL_EMAIL);
        if (prefillEmail != null && !prefillEmail.isEmpty()) {
            emailEditText.setText(prefillEmail);
        }

        applyModeUi();

        requestOtpButton.setOnClickListener(v -> requestOtp());
        submitButton.setOnClickListener(v -> submitFlow());
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

    private void applyModeUi() {
        if (MODE_SIGNUP.equals(mode)) {
            toolbar.setTitle(R.string.otp_signup_title);
            subtitleTextView.setText(R.string.otp_signup_subtitle);
            submitButton.setText(R.string.otp_action_complete_signup);
            passwordLayout.setVisibility(View.VISIBLE);
            firstNameLayout.setVisibility(View.VISIBLE);
            lastNameLayout.setVisibility(View.VISIBLE);
            dniLayout.setVisibility(View.VISIBLE);
            passwordLayout.setHint(getString(R.string.password_hint));
        } else {
            toolbar.setTitle(R.string.password_reset_title);
            subtitleTextView.setText(R.string.password_reset_subtitle);
            submitButton.setText(R.string.password_reset_confirm_button);
            passwordLayout.setVisibility(View.VISIBLE);
            firstNameLayout.setVisibility(View.GONE);
            lastNameLayout.setVisibility(View.GONE);
            dniLayout.setVisibility(View.GONE);
            passwordLayout.setHint(getString(R.string.new_password_hint));
        }
    }

    private void loadConfiguration() {
        try {
            appConfig = configLoader.loadConfig();
            if (appConfig != null && appConfig.baseUrl != null && !appConfig.baseUrl.isEmpty()) {
                authService = RetrofitClient.getClient(appConfig).create(AuthService.class);
                updateRequestButtonState();
                submitButton.setEnabled(true);
                updateResendButtonState();
            } else {
                authService = null;
                requestOtpButton.setEnabled(false);
                submitButton.setEnabled(false);
                resendOtpButton.setEnabled(false);
                showError(getString(R.string.error_config_load));
            }
        } catch (Exception e) {
            authService = null;
            requestOtpButton.setEnabled(false);
            submitButton.setEnabled(false);
            resendOtpButton.setEnabled(false);
            showError(getString(R.string.error_invalid_config));
        }
    }

    private void requestOtp() {
        String email = getEmail();
        String emailError = AuthInputValidator.validateEmail(this, email);
        if (emailError != null) {
            showError(emailError);
            return;
        }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        Call<OtpResponse> requestCall = MODE_SIGNUP.equals(mode)
                ? authService.requestSignupOtp(AuthEndpoints.signupOtpRequest(appConfig), new OtpRequest(email))
                : authService.requestPasswordReset(AuthEndpoints.passwordResetRequest(appConfig), new OtpRequest(email));

        requestCall.enqueue(new Callback<OtpResponse>() {
            @Override
            public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    showInfo(getString(MODE_SIGNUP.equals(mode)
                            ? R.string.signup_otp_sent_message
                            : R.string.password_reset_sent_message));
                    startRequestCooldown(OTP_COOLDOWN_MILLIS);
                    startResendCooldown(OTP_COOLDOWN_MILLIS);
                } else {
                    String fallback = getString(MODE_SIGNUP.equals(mode)
                            ? R.string.error_signup_otp_request_default
                            : R.string.error_password_reset_request_default);
                    showError(NetworkErrorParser.getErrorMessage(response, fallback));
                }
            }

            @Override
            public void onFailure(Call<OtpResponse> call, Throwable t) {
                setLoading(false);
                String message = t != null && t.getLocalizedMessage() != null
                        ? t.getLocalizedMessage()
                        : getString(R.string.error_network_generic);
                showError(getString(R.string.generic_error, message));
            }
        });
    }

    private void submitFlow() {
        if (MODE_SIGNUP.equals(mode)) {
            completeSignupWithOtp();
        } else {
            confirmPasswordReset();
        }
    }

    private void completeSignupWithOtp() {
        String email = getEmail();
        String code = getOtpCode();
        String password = getPassword();
        String firstName = getFirstName();
        String lastName = getLastName();
        String dni = getDni();

        String validationError = validateSignupInput(email, code, password, firstName, lastName, dni);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        OtpRegistrationCompleteRequest request = new OtpRegistrationCompleteRequest(email, code, password, firstName, lastName, dni);
        authService.completeSignupWithOtp(AuthEndpoints.signupOtpComplete(appConfig), request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    handleLoginSuccess(response.body());
                } else {
                    String errorMessage = NetworkErrorParser.getErrorMessage(response, getString(R.string.error_signup_complete_default));
                    showError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                String message = t != null && t.getLocalizedMessage() != null
                        ? t.getLocalizedMessage()
                        : getString(R.string.error_network_generic);
                showError(getString(R.string.generic_error, message));
            }
        });
    }

    private void confirmPasswordReset() {
        String email = getEmail();
        String code = getOtpCode();
        String password = getPassword();

        String validationError = validatePasswordResetInput(email, code, password);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(email, code, password);
        authService.confirmPasswordReset(AuthEndpoints.passwordResetConfirm(appConfig), request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    handleLoginSuccess(response.body());
                } else {
                    String errorMessage = NetworkErrorParser.getErrorMessage(response, getString(R.string.error_password_reset_confirm_default));
                    showError(errorMessage);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                String message = t != null && t.getLocalizedMessage() != null
                        ? t.getLocalizedMessage()
                        : getString(R.string.error_network_generic);
                showError(getString(R.string.generic_error, message));
            }
        });
    }

    private void resendOtp() {
        String email = getEmail();
        String emailError = AuthInputValidator.validateEmail(this, email);
        if (emailError != null) {
            showError(emailError);
            return;
        }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        Call<OtpResponse> requestCall = MODE_SIGNUP.equals(mode)
                ? authService.resendSignupOtp(AuthEndpoints.signupOtpResend(appConfig), new OtpRequest(email))
                : authService.resendPasswordReset(AuthEndpoints.passwordResetResend(appConfig), new OtpRequest(email));

        requestCall.enqueue(new Callback<OtpResponse>() {
            @Override
            public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    showInfo(getString(MODE_SIGNUP.equals(mode)
                            ? R.string.signup_otp_resent_message
                            : R.string.password_reset_resent_message));
                    startRequestCooldown(OTP_COOLDOWN_MILLIS);
                    startResendCooldown(OTP_COOLDOWN_MILLIS);
                } else {
                    String fallback = getString(MODE_SIGNUP.equals(mode)
                            ? R.string.error_signup_otp_resend_default
                            : R.string.error_password_reset_resend_default);
                    showError(NetworkErrorParser.getErrorMessage(response, fallback));
                }
            }

            @Override
            public void onFailure(Call<OtpResponse> call, Throwable t) {
                setLoading(false);
                String message = t != null && t.getLocalizedMessage() != null
                        ? t.getLocalizedMessage()
                        : getString(R.string.error_network_generic);
                showError(getString(R.string.generic_error, message));
            }
        });
    }

    private String validateSignupInput(String email, String code, String password, String firstName, String lastName, String dni) {
        String emailError = AuthInputValidator.validateEmail(this, email);
        if (emailError != null) {
            return emailError;
        }

        String otpError = AuthInputValidator.validateOtp(this, code);
        if (otpError != null) {
            return otpError;
        }

        String passwordError = AuthInputValidator.validatePassword(this, password);
        if (passwordError != null) {
            return passwordError;
        }

        String firstNameError = AuthInputValidator.validateFirstName(this, firstName);
        if (firstNameError != null) {
            return firstNameError;
        }

        String lastNameError = AuthInputValidator.validateLastName(this, lastName);
        if (lastNameError != null) {
            return lastNameError;
        }

        return AuthInputValidator.validateDni(this, dni);
    }

    private String validatePasswordResetInput(String email, String code, String password) {
        String emailError = AuthInputValidator.validateEmail(this, email);
        if (emailError != null) {
            return emailError;
        }

        String otpError = AuthInputValidator.validateOtp(this, code);
        if (otpError != null) {
            return otpError;
        }

        return AuthInputValidator.validatePassword(this, password);
    }

    private String getEmail() {
        return emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
    }

    private String getOtpCode() {
        return otpEditText.getText() != null ? otpEditText.getText().toString().trim() : "";
    }

    private String getPassword() {
        return passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
    }

    private String getFirstName() {
        return firstNameEditText.getText() != null ? firstNameEditText.getText().toString().trim() : "";
    }

    private String getLastName() {
        return lastNameEditText.getText() != null ? lastNameEditText.getText().toString().trim() : "";
    }

    private String getDni() {
        return dniEditText.getText() != null ? dniEditText.getText().toString().trim() : "";
    }

    private void handleLoginSuccess(LoginResponse response) {
        if (response != null && response.token != null && !response.token.trim().isEmpty()) {
            sessionManager.saveAccessToken(response.token);
        }
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        this.isLoading = isLoading;
        emailEditText.setEnabled(!isLoading);
        otpEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        firstNameEditText.setEnabled(!isLoading);
        lastNameEditText.setEnabled(!isLoading);
        dniEditText.setEnabled(!isLoading);
        updateRequestButtonState();
        submitButton.setEnabled(!isLoading);
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
