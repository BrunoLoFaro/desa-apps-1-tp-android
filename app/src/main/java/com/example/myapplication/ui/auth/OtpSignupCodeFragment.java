package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.myapplication.R;
import com.example.myapplication.data.model.OtpCodeVerificationRequest;
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

public class OtpSignupCodeFragment extends BaseAuthFragment {

    private TextInputEditText codeEditText;
    private MaterialButton verifyButton;
    private MaterialButton resendButton;
    private CircularProgressIndicator progressIndicator;
    private String email;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp_signup_code, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            email = getArguments().getString("email");
        }

        codeEditText = view.findViewById(R.id.otp_signup_code_edit_text);
        verifyButton = view.findViewById(R.id.otp_signup_verify_button);
        resendButton = view.findViewById(R.id.otp_signup_resend_button);
        progressIndicator = view.findViewById(R.id.otp_signup_code_progress_indicator);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(requireActivity(), toolbar);
        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        verifyButton.setOnClickListener(v -> verifyCode());
        resendButton.setOnClickListener(v -> resendCode());

        super.onViewCreated(view, savedInstanceState);

        if (email == null || email.isEmpty()) {
            navController.navigateUp();
        }
    }

    @Override
    protected void onConfigReady() {
        if (verifyButton != null) verifyButton.setEnabled(true);
        if (resendButton != null) resendButton.setEnabled(true);
    }

    private void verifyCode() {
        String code = codeEditText.getText() != null ? codeEditText.getText().toString().trim() : "";
        String codeError = AuthInputValidator.validateOtp(requireContext(), code);
        if (codeError != null) { showError(codeError); return; }

        setLoading(true);
        authService.verifySignupOtp(
                AuthEndpoints.signupOtpVerify(appConfig),
                new OtpCodeVerificationRequest(email, code))
                .enqueue(new Callback<OtpResponse>() {
                    @Override
                    public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            Bundle args = new Bundle();
                            args.putString("email", email);
                            args.putString("code", code);
                            navController.navigate(R.id.action_otpSignupCodeFragment_to_otpSignupCompleteFragment, args);
                        } else {
                            showError(NetworkErrorParser.getErrorMessage(
                                    response, getString(R.string.error_signup_otp_verify_default)));
                        }
                    }

                    @Override
                    public void onFailure(Call<OtpResponse> call, Throwable t) {
                        setLoading(false);
                        showError(NetworkErrorParser.getFailureMessage(t, getString(R.string.error_signup_otp_verify_default)));
                    }
                });
    }

    private void resendCode() {
        setLoading(true);
        authService.resendSignupOtp(AuthEndpoints.signupOtpResend(appConfig), new OtpRequest(email))
                .enqueue(new Callback<OtpResponse>() {
                    @Override
                    public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            showInfo(getString(R.string.signup_otp_resent_message));
                        } else {
                            showError(NetworkErrorParser.getErrorMessage(
                                    response, getString(R.string.error_signup_otp_resend_default)));
                        }
                    }

                    @Override
                    public void onFailure(Call<OtpResponse> call, Throwable t) {
                        setLoading(false);
                        showError(NetworkErrorParser.getFailureMessage(t, getString(R.string.error_signup_otp_resend_default)));
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        verifyButton.setEnabled(!isLoading);
        resendButton.setEnabled(!isLoading);
        codeEditText.setEnabled(!isLoading);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
