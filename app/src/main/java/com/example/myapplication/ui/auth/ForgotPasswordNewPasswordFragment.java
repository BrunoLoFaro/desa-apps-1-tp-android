package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;
import com.example.myapplication.ui.auth.viewmodel.ForgotPasswordViewModel;
import com.example.myapplication.util.AuthInputValidator;
import com.example.myapplication.util.BiometricHelper;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ForgotPasswordNewPasswordFragment extends BaseAuthFragment {

    private TextInputLayout passwordInputLayout;
    private TextInputLayout confirmPasswordInputLayout;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;
    private MaterialButton savePasswordButton;
    private CircularProgressIndicator progressIndicator;

    private String email;
    private String code;
    private ForgotPasswordViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password_new_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            email = getArguments().getString("email");
            code = getArguments().getString("code");
        }

        passwordInputLayout = view.findViewById(R.id.forgot_new_password_input_layout);
        confirmPasswordInputLayout = view.findViewById(R.id.forgot_confirm_password_input_layout);
        passwordEditText = view.findViewById(R.id.forgot_new_password_edit_text);
        confirmPasswordEditText = view.findViewById(R.id.forgot_confirm_password_edit_text);
        savePasswordButton = view.findViewById(R.id.forgot_save_password_button);
        progressIndicator = view.findViewById(R.id.forgot_new_password_progress_indicator);

        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ForgotPasswordViewModel.class);

        savePasswordButton.setOnClickListener(v -> confirmNewPassword());

        viewModel.getNewPasswordState().observe(getViewLifecycleOwner(), state -> {
            savePasswordButton.setEnabled(!state.isLoading);
            passwordEditText.setEnabled(!state.isLoading);
            confirmPasswordEditText.setEnabled(!state.isLoading);
            progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);

            if (state.error != null) {
                showError(state.error.resolve(requireContext()));
                viewModel.newPasswordErrorConsumed();
            }
            if (state.navigateToHome) {
                viewModel.newPasswordNavigationConsumed();
                View v = getView();
                if (v != null) {
                    Snackbar.make(v, getString(R.string.password_updated), Snackbar.LENGTH_SHORT)
                            .addCallback(new Snackbar.Callback() {
                                @Override
                                public void onDismissed(Snackbar transientBottomBar, int event) {
                                    if (isAdded()) navigateToHome();
                                }
                            })
                            .show();
                } else {
                    navigateToHome();
                }
            }
        });

        Toolbar localToolbar = view.findViewById(R.id.local_toolbar);
        if (localToolbar != null) {
            localToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white);
            localToolbar.setNavigationOnClickListener(v -> navController.navigateUp());
        }

        if (email == null || code == null) {
            navController.navigateUp();
        }
    }

    @Override
    protected void navigateToHome() {
        navController.navigate(R.id.action_forgotPasswordNewPasswordFragment_to_homeFragment);
    }

    private void confirmNewPassword() {
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);
        String password = passwordEditText.getText() != null
                ? passwordEditText.getText().toString() : "";
        String confirm = confirmPasswordEditText.getText() != null
                ? confirmPasswordEditText.getText().toString() : "";
        String passwordError = AuthInputValidator.validatePassword(requireContext(), password);
        if (passwordError != null) {
            passwordInputLayout.setError(passwordError);
            return;
        }
        if (!password.equals(confirm)) {
            confirmPasswordInputLayout.setError(getString(R.string.error_passwords_do_not_match));
            return;
        }
        viewModel.confirmNewPassword(email, code, password);
    }

    @Override
    public void onDestroyView() {
        passwordInputLayout = null;
        confirmPasswordInputLayout = null;
        passwordEditText = null;
        confirmPasswordEditText = null;
        savePasswordButton = null;
        progressIndicator = null;
        super.onDestroyView();
    }
}