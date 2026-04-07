package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.R;
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

public class ForgotPasswordRequestFragment extends BaseAuthFragment {

    private TextInputEditText emailEditText;
    private MaterialButton sendCodeButton;
    private CircularProgressIndicator progressIndicator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        emailEditText = view.findViewById(R.id.forgot_request_email_edit_text);
        sendCodeButton = view.findViewById(R.id.forgot_request_send_code_button);
        progressIndicator = view.findViewById(R.id.forgot_request_progress_indicator);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(requireActivity(), toolbar);
        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        if (getArguments() != null) {
            String prefillEmail = getArguments().getString("prefill_email");
            if (prefillEmail != null && !prefillEmail.isEmpty()) {
                emailEditText.setText(prefillEmail);
            }
        }

        sendCodeButton.setOnClickListener(v -> requestCode());

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected void onConfigReady() {
        if (sendCodeButton != null) sendCodeButton.setEnabled(true);
    }

    private void requestCode() {
        String email = emailEditText.getText() != null
                ? emailEditText.getText().toString().trim() : "";

        String emailError = AuthInputValidator.validateEmail(requireContext(), email);
        if (emailError != null) {
            showError(emailError);
            return;
        }

        if (authService == null || appConfig == null) {
            showError(getString(R.string.error_service_not_initialized));
            return;
        }

        setLoading(true);
        authService.requestPasswordReset(AuthEndpoints.passwordResetRequest(appConfig), new OtpRequest(email))
                .enqueue(new Callback<OtpResponse>() {
                    @Override
                    public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            Bundle args = new Bundle();
                            args.putString("email", email);
                            navController.navigate(R.id.action_forgotPasswordRequestFragment_to_forgotPasswordCodeFragment, args);
                        } else {
                            showError(NetworkErrorParser.getErrorMessage(
                                    response, getString(R.string.error_password_reset_request_default)));
                        }
                    }

                    @Override
                    public void onFailure(Call<OtpResponse> call, Throwable t) {
                        setLoading(false);
                        showError(NetworkErrorParser.getFailureMessage(t, getString(R.string.error_network_generic)));
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        sendCodeButton.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
