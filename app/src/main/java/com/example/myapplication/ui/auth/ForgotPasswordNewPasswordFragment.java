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

public class ForgotPasswordNewPasswordFragment extends BaseAuthFragment {

    private TextInputEditText passwordEditText;
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

        passwordEditText = view.findViewById(R.id.forgot_new_password_edit_text);
        savePasswordButton = view.findViewById(R.id.forgot_save_password_button);
        progressIndicator = view.findViewById(R.id.forgot_new_password_progress_indicator);

        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(requireActivity(), toolbar);
        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        // Same Activity-scoped instance as ForgotPasswordRequestFragment and ForgotPasswordCodeFragment
        viewModel = new ViewModelProvider(requireActivity()).get(ForgotPasswordViewModel.class);

        savePasswordButton.setOnClickListener(v -> confirmNewPassword());

        viewModel.getNewPasswordState().observe(getViewLifecycleOwner(), state -> {
            savePasswordButton.setEnabled(!state.isLoading);
            passwordEditText.setEnabled(!state.isLoading);
            progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);

            if (state.error != null) {
                showError(state.error.resolve(requireContext()));
                viewModel.newPasswordErrorConsumed();
            }
            if (state.navigateToHome) {
                navigateToHome();
                viewModel.newPasswordNavigationConsumed();
            }
        });

        if (email == null || code == null) {
            navController.navigateUp();
        }
    }

    @Override
    protected void navigateToHome() {
        navController.navigate(R.id.action_forgotPasswordNewPasswordFragment_to_homeFragment);
    }

    private void confirmNewPassword() {
        String password = passwordEditText.getText() != null
                ? passwordEditText.getText().toString() : "";
        String passwordError = AuthInputValidator.validatePassword(requireContext(), password);
        if (passwordError != null) { showError(passwordError); return; }
        viewModel.confirmNewPassword(email, code, password);
    }
}
