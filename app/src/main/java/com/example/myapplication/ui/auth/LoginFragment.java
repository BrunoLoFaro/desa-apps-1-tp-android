package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.myapplication.R;
import com.example.myapplication.data.model.LoginRequest;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.util.AuthEndpoints;
import com.example.myapplication.util.AuthInputValidator;
import com.example.myapplication.util.NetworkErrorParser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends BaseAuthFragment {

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private MaterialButton forgotPasswordButton;
    private MaterialButton signUpButton;
    private CircularProgressIndicator progressIndicator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        emailEditText = view.findViewById(R.id.email_edit_text);
        passwordEditText = view.findViewById(R.id.password_edit_text);
        loginButton = view.findViewById(R.id.login_button);
        forgotPasswordButton = view.findViewById(R.id.forgot_password_button);
        signUpButton = view.findViewById(R.id.sign_up_button);
        progressIndicator = view.findViewById(R.id.progress_indicator);

        super.onViewCreated(view, savedInstanceState);

        loginButton.setOnClickListener(v -> attemptLogin());
        signUpButton.setOnClickListener(v -> navController.navigate(R.id.action_loginFragment_to_signupFragment));
        forgotPasswordButton.setOnClickListener(v -> navController.navigate(R.id.action_loginFragment_to_forgotPasswordRequestFragment));
        
        // Si ya hay sesión válida, ir a Home
        if (sessionManager.hasValidSession()) {
            navigateToHome();
        }
    }

    @Override
    protected void onConfigReady() {
        if (loginButton != null) loginButton.setEnabled(true);
        if (forgotPasswordButton != null) forgotPasswordButton.setEnabled(true);
        if (signUpButton != null) signUpButton.setEnabled(true);
    }

    @Override
    protected void onConfigError(String error) {
        if (loginButton != null) loginButton.setEnabled(false);
        if (forgotPasswordButton != null) forgotPasswordButton.setEnabled(false);
        super.onConfigError(error);
    }

    private void attemptLogin() {
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";

        String emailError = AuthInputValidator.validateEmail(requireContext(), email);
        if (emailError != null) { showError(emailError); return; }

        String passwordError = AuthInputValidator.validatePassword(requireContext(), password);
        if (passwordError != null) { showError(passwordError); return; }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        authService.login(AuthEndpoints.login(appConfig), new LoginRequest(email, password))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            handleLoginSuccess(response.body());
                        } else {
                            String msg = NetworkErrorParser.getErrorMessage(
                                    response, getString(R.string.error_login_failed_default));
                            showError(getString(R.string.login_failed, msg));
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        setLoading(false);
                        showError(NetworkErrorParser.getFailureMessage(t, getString(R.string.error_network_generic)));
                    }
                });
    }

    @Override
    protected void navigateToHome() {
        navController.navigate(R.id.action_loginFragment_to_homeFragment);
    }

    private void setLoading(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        forgotPasswordButton.setEnabled(!isLoading);
        signUpButton.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
