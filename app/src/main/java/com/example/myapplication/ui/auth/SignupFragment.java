package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.BuildConfig;
import com.example.myapplication.R;
import com.example.myapplication.data.common.UiMessage;
import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.ui.auth.viewmodel.SignupViewModel;
import com.example.myapplication.util.AuthInputValidator;
import com.example.myapplication.util.ToolbarHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class SignupFragment extends BaseAuthFragment {

    @Inject
    ConfigLoader configLoader;

    private TextInputLayout emailInputLayout;
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
        emailInputLayout = view.findViewById(R.id.signup_email_layout);
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

        setupTextWatchers();

        viewModel.getRequestOtpState().observe(getViewLifecycleOwner(), state -> {
            registerWithEmailButton.setEnabled(!state.isLoading);
            classicRegisterButton.setEnabled(!state.isLoading);
            emailEditText.setEnabled(!state.isLoading);
            progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);

            if (state.isLoading) {
                emailInputLayout.setError(null);
            }

            if (state.error != null) {
                showOtpDiagnosticIfDebug(state.error);
                emailInputLayout.setError(state.error.resolve(requireContext()));
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

    private void setupTextWatchers() {
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                emailInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void startOtpSignup() {
        String email = emailEditText.getText() != null
                ? emailEditText.getText().toString().trim() : "";
        String emailError = AuthInputValidator.validateEmail(requireContext(), email);
        if (emailError != null) {
            emailInputLayout.setError(emailError);
            return;
        }
        emailInputLayout.setError(null);
        viewModel.requestSignupOtp(email);
    }

    private void showOtpDiagnosticIfDebug(UiMessage error) {
        if (!BuildConfig.DEBUG || getContext() == null) {
            return;
        }

        AppConfig config = configLoader.loadConfig();
        String endpoint = "N/A";
        if (config != null && config.baseUrl != null && config.signupOtpRequestEndpoint != null) {
            endpoint = config.baseUrl + config.signupOtpRequestEndpoint;
        }

        String message = error.resolve(requireContext());
        String diagnostic = "Mini diagnostico OTP\n\n"
                + "Endpoint: " + endpoint + "\n"
                + "Error recibido: " + message + "\n\n"
                + "Checks sugeridos:\n"
                + "1) Backend levantado y alcanzable desde emulador (10.0.2.2).\n"
                + "2) Endpoint /auth/signup/otp/request existe y acepta {email}.\n"
                + "3) Servicio de mail/SMTP del backend configurado.\n"
                + "4) Revisar stacktrace backend en el mismo timestamp.";

        new AlertDialog.Builder(requireContext())
                .setTitle("Diagnostico rapido")
                .setMessage(diagnostic)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        emailInputLayout = null;
        emailEditText = null;
        registerWithEmailButton = null;
        classicRegisterButton = null;
        progressIndicator = null;
        super.onDestroyView();
    }
}
