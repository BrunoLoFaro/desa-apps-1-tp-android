package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;
import com.example.myapplication.ui.auth.viewmodel.SignupViewModel;
import com.example.myapplication.util.AuthInputValidator;
import com.example.myapplication.util.ToolbarHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class OtpSignupCodeFragment extends BaseAuthFragment {

    private TextInputEditText codeEditText;
    private MaterialButton verifyButton;
    private MaterialButton resendButton;
    private CircularProgressIndicator progressIndicator;

    private String email;
    private SignupViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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

        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(requireActivity(), toolbar);
        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        // Same Activity-scoped instance as SignupFragment
        viewModel = new ViewModelProvider(requireActivity()).get(SignupViewModel.class);

        verifyButton.setOnClickListener(v -> verifyCode());
        resendButton.setOnClickListener(v -> resendCode());

        viewModel.getOtpCodeState().observe(getViewLifecycleOwner(), state -> {
            verifyButton.setEnabled(!state.isLoading);
            resendButton.setEnabled(!state.isLoading);
            codeEditText.setEnabled(!state.isLoading);
            progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);

            if (state.error != null) {
                showError(state.error.resolve(requireContext()));
                viewModel.otpCodeErrorConsumed();
            }
            if (state.otpResent) {
                showInfo(getString(R.string.signup_otp_resent_message));
                viewModel.otpResentConsumed();
            }
            if (state.navigateToComplete) {
                String code = codeEditText.getText() != null
                        ? codeEditText.getText().toString().trim() : "";
                Bundle args = new Bundle();
                args.putString("email", email);
                args.putString("code", code);
                navController.navigate(R.id.action_otpSignupCodeFragment_to_otpSignupCompleteFragment, args);
                viewModel.otpCodeNavigationConsumed();
            }
        });

        if (email == null || email.isEmpty()) {
            navController.navigateUp();
        }
    }

    private void verifyCode() {
        String code = codeEditText.getText() != null
                ? codeEditText.getText().toString().trim() : "";
        String codeError = AuthInputValidator.validateOtp(requireContext(), code);
        if (codeError != null) { showError(codeError); return; }
        viewModel.verifySignupOtp(email, code);
    }

    private void resendCode() {
        viewModel.resendSignupOtp(email);
    }

    @Override
    public void onDestroyView() {
        codeEditText = null;
        verifyButton = null;
        resendButton = null;
        progressIndicator = null;
        super.onDestroyView();
    }
}
