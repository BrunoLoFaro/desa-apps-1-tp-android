package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;
import com.example.myapplication.util.AuthInputValidator;
import com.example.myapplication.util.ToolbarHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

public class SignupFragment extends BaseAuthFragment {

    private TextInputEditText emailEditText;
    private MaterialButton registerWithEmailButton;
    private MaterialButton classicRegisterButton;
    private CircularProgressIndicator progressIndicator;

    private SignupViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        emailEditText = view.findViewById(R.id.signup_email_edit_text);
        registerWithEmailButton = view.findViewById(R.id.signup_with_email_button);
        classicRegisterButton = view.findViewById(R.id.signup_classic_button);
        progressIndicator = view.findViewById(R.id.signup_progress_indicator);

        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(requireActivity(), toolbar);
        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        // Activity-scoped so the email state survives navigation to OtpSignupCodeFragment
        viewModel = new ViewModelProvider(requireActivity()).get(SignupViewModel.class);

        registerWithEmailButton.setOnClickListener(v -> startOtpSignup());
        classicRegisterButton.setOnClickListener(v ->
                navController.navigate(R.id.action_signupFragment_to_classicRegisterFragment));

        viewModel.getRequestOtpState().observe(getViewLifecycleOwner(), state -> {
            registerWithEmailButton.setEnabled(!state.isLoading && state.configValid);
            classicRegisterButton.setEnabled(!state.isLoading && state.configValid);
            emailEditText.setEnabled(!state.isLoading);
            progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);

            if (state.error != null) {
                showError(state.error.resolve(requireContext()));
                viewModel.requestOtpErrorConsumed();
            }
            if (state.navigateToOtpCode) {
                String email = emailEditText.getText() != null
                        ? emailEditText.getText().toString().trim() : "";
                Bundle args = new Bundle();
                args.putString("email", email);
                navController.navigate(R.id.action_signupFragment_to_otpSignupCodeFragment, args);
                viewModel.requestOtpNavigationConsumed();
            }
        });
    }

    private void startOtpSignup() {
        String email = emailEditText.getText() != null
                ? emailEditText.getText().toString().trim() : "";
        String emailError = AuthInputValidator.validateEmail(requireContext(), email);
        if (emailError != null) { showError(emailError); return; }
        viewModel.requestSignupOtp(email);
    }
}
