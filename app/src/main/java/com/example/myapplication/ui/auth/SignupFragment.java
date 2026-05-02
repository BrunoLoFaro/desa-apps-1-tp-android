package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;
import com.example.myapplication.ui.auth.viewmodel.SignupViewModel;
import com.example.myapplication.util.AuthInputValidator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.widget.Toolbar;
import dagger.hilt.android.AndroidEntryPoint;

/** Pantalla de entrada de email para "Ingresar con código de un solo uso". */
@AndroidEntryPoint
public class SignupFragment extends BaseAuthFragment {

    private TextInputLayout emailInputLayout;
    private TextInputEditText emailEditText;
    private MaterialButton sendOtpButton;
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
        emailInputLayout = view.findViewById(R.id.signup_email_layout);
        emailEditText = view.findViewById(R.id.signup_email_edit_text);
        sendOtpButton = view.findViewById(R.id.signup_with_email_button);
        progressIndicator = view.findViewById(R.id.signup_progress_indicator);

        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SignupViewModel.class);

        sendOtpButton.setOnClickListener(v -> sendOtp());

        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                emailInputLayout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        viewModel.getSendOtpState().observe(getViewLifecycleOwner(), state -> {
            sendOtpButton.setEnabled(!state.isLoading);
            emailEditText.setEnabled(!state.isLoading);
            progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);

            if (state.isLoading) {
                emailInputLayout.setError(null);
            }

            if (state.error != null) {
                emailInputLayout.setError(state.error.resolve(requireContext()));
                viewModel.sendOtpErrorConsumed();
            }

            if (state.navigateToOtpCode) {
                String email = emailEditText.getText() != null
                        ? emailEditText.getText().toString().trim() : "";
                Bundle args = new Bundle();
                args.putString("email", email);
                args.putString("source", OtpSignupCodeFragment.SOURCE_OTP_LOGIN);
                navController.navigate(R.id.action_signupFragment_to_otpSignupCodeFragment, args);
                viewModel.sendOtpNavigationConsumed();
            }
        });

        Toolbar localToolbar = view.findViewById(R.id.local_toolbar);
        if (localToolbar != null) {
            localToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
            localToolbar.setNavigationOnClickListener(v -> navController.popBackStack(R.id.loginFragment, false));
        }
    }

    private void sendOtp() {
        String email = emailEditText.getText() != null
                ? emailEditText.getText().toString().trim() : "";
        String emailError = AuthInputValidator.validateEmail(requireContext(), email);
        if (emailError != null) {
            emailInputLayout.setError(emailError);
            return;
        }
        emailInputLayout.setError(null);
        viewModel.sendLoginOtp(email);
    }

    @Override
    public void onDestroyView() {
        emailInputLayout = null;
        emailEditText = null;
        sendOtpButton = null;
        progressIndicator = null;
        super.onDestroyView();
    }
}