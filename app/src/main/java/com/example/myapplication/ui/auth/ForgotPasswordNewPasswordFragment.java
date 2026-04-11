package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.R;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.model.PasswordResetConfirmRequest;
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

public class ForgotPasswordNewPasswordFragment extends BaseAuthFragment {

    private TextInputEditText passwordEditText;
    private MaterialButton savePasswordButton;
    private CircularProgressIndicator progressIndicator;

    private String email;
    private String code;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password_new_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            email = getArguments().getString("email");
            code = getArguments().getString("code");
        }

        passwordEditText = view.findViewById(R.id.forgot_new_password_edit_text);
        savePasswordButton = view.findViewById(R.id.forgot_save_password_button);
        progressIndicator = view.findViewById(R.id.forgot_new_password_progress_indicator);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(requireActivity(), toolbar);
        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        savePasswordButton.setOnClickListener(v -> confirmNewPassword());

        super.onViewCreated(view, savedInstanceState);

        if (email == null || code == null) {
            navController.navigateUp();
        }
    }

    @Override
    protected void onConfigReady() {
        if (savePasswordButton != null) savePasswordButton.setEnabled(true);
    }

    private void confirmNewPassword() {
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
        String passwordError = AuthInputValidator.validatePassword(requireContext(), password);
        if (passwordError != null) {
            showError(passwordError);
            return;
        }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        authService.confirmPasswordReset(
                AuthEndpoints.passwordResetConfirm(appConfig),
                new PasswordResetConfirmRequest(email, code, password))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            handleLoginSuccess(response.body());
                        } else {
                            showError(NetworkErrorParser.getErrorMessage(
                                    response, getString(R.string.error_password_reset_confirm_default)));
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
        navController.navigate(R.id.action_forgotPasswordNewPasswordFragment_to_homeFragment);
    }

    private void setLoading(boolean isLoading) {
        savePasswordButton.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
