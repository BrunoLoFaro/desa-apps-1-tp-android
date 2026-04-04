package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.data.model.OtpRequest;
import com.example.myapplication.data.model.OtpResponse;
import com.example.myapplication.util.AuthEndpoints;
import com.example.myapplication.util.AuthInputValidator;
import com.example.myapplication.util.NetworkErrorParser;
import com.example.myapplication.util.ToolbarHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends BaseAuthActivity {

    private TextInputEditText emailEditText;
    private MaterialButton registerWithEmailButton;
    private MaterialButton classicRegisterButton;
    private CircularProgressIndicator progressIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        emailEditText = findViewById(R.id.signup_email_edit_text);
        registerWithEmailButton = findViewById(R.id.signup_with_email_button);
        classicRegisterButton = findViewById(R.id.signup_classic_button);
        progressIndicator = findViewById(R.id.signup_progress_indicator);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(this, toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(getRootView(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        registerWithEmailButton.setOnClickListener(v -> startOtpSignup());
        classicRegisterButton.setOnClickListener(v ->
                startActivity(new Intent(this, ClassicRegisterActivity.class)));
    }

    @Override
    protected View getRootView() {
        return findViewById(R.id.signup_coordinator);
    }

    @Override
    protected void onConfigReady() {
        registerWithEmailButton.setEnabled(true);
        classicRegisterButton.setEnabled(true);
    }

    @Override
    protected void onConfigError(String error) {
        registerWithEmailButton.setEnabled(false);
        classicRegisterButton.setEnabled(false);
        super.onConfigError(error);
    }

    private void startOtpSignup() {
        String email = emailEditText.getText() != null
                ? emailEditText.getText().toString().trim() : "";

        String emailError = AuthInputValidator.validateEmail(this, email);
        if (emailError != null) { showError(emailError); return; }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        authService.requestSignupOtp(AuthEndpoints.signupOtpRequest(appConfig), new OtpRequest(email))
                .enqueue(new Callback<OtpResponse>() {
                    @Override
                    public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            Intent intent = new Intent(SignupActivity.this, OtpSignupCodeActivity.class);
                            intent.putExtra(OtpSignupCodeActivity.EXTRA_EMAIL, email);
                            startActivity(intent);
                        } else {
                            showError(NetworkErrorParser.getErrorMessage(
                                    response, getString(R.string.error_signup_otp_request_default)));
                        }
                    }

                    @Override
                    public void onFailure(Call<OtpResponse> call, Throwable t) {
                        setLoading(false);
                        String msg = t != null && t.getLocalizedMessage() != null
                                ? t.getLocalizedMessage() : getString(R.string.error_network_generic);
                        showError(getString(R.string.generic_error, msg));
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        registerWithEmailButton.setEnabled(!isLoading);
        classicRegisterButton.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
