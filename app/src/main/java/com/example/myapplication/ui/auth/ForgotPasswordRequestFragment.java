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
import com.example.myapplication.util.ToolbarHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ForgotPasswordRequestFragment extends BaseAuthFragment {

    private TextInputEditText emailEditText;
    private MaterialButton sendCodeButton;
    private CircularProgressIndicator progressIndicator;

    private ForgotPasswordViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        emailEditText = view.findViewById(R.id.forgot_request_email_edit_text);
        sendCodeButton = view.findViewById(R.id.forgot_request_send_code_button);
        progressIndicator = view.findViewById(R.id.forgot_request_progress_indicator);

        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        ToolbarHelper.setupBackToolbar(requireActivity(), toolbar);
        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        if (getArguments() != null) {
            String prefill = getArguments().getString("prefill_email");
            if (prefill != null && !prefill.isEmpty()) {
                emailEditText.setText(prefill);
            }
        }

        // Activity-scoped — shared with ForgotPasswordCodeFragment and ForgotPasswordNewPasswordFragment
        viewModel = new ViewModelProvider(requireActivity()).get(ForgotPasswordViewModel.class);

        sendCodeButton.setOnClickListener(v -> requestCode());

        viewModel.getRequestState().observe(getViewLifecycleOwner(), state -> {
            sendCodeButton.setEnabled(!state.isLoading);
            emailEditText.setEnabled(!state.isLoading);
            progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);

            if (state.error != null) {
                showError(state.error.resolve(requireContext()));
                viewModel.requestErrorConsumed();
            }
            if (state.navigateToCode) {
                String email = emailEditText.getText() != null
                        ? emailEditText.getText().toString().trim() : "";
                Bundle args = new Bundle();
                args.putString("email", email);
                navController.navigate(
                        R.id.action_forgotPasswordRequestFragment_to_forgotPasswordCodeFragment, args);
                viewModel.requestNavigationConsumed();
            }
        });
    }

    private void requestCode() {
        String email = emailEditText.getText() != null
                ? emailEditText.getText().toString().trim() : "";
        String emailError = AuthInputValidator.validateEmail(requireContext(), email);
        if (emailError != null) { showError(emailError); return; }
        viewModel.requestReset(email);
    }

    @Override
    public void onDestroyView() {
        emailEditText = null;
        sendCodeButton = null;
        progressIndicator = null;
        super.onDestroyView();
    }
}
