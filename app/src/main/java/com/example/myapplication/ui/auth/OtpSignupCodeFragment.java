package com.example.myapplication.ui.auth;

import android.os.CountDownTimer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.myapplication.R;
import com.example.myapplication.ui.auth.viewmodel.SignupViewModel;
import com.example.myapplication.util.AuthInputValidator;
import com.example.myapplication.util.BiometricHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class OtpSignupCodeFragment extends BaseAuthFragment {

    public static final String SOURCE_REGISTRATION = "registration";
    public static final String SOURCE_OTP_LOGIN = "otp_login";

    private static final long RESEND_COOLDOWN_SECONDS = 60L;
    private static final long EXTRA_COOLDOWN_SECONDS = 120L;
    private static final int MAX_INVALID_ATTEMPTS = 3;

    private TextInputEditText codeEditText;
    private TextInputLayout codeInputLayout;
    private MaterialButton verifyButton;
    private MaterialButton resendButton;
    private CircularProgressIndicator progressIndicator;
    private MaterialTextView resendTimerText;
    private MaterialTextView subtitleText;
    private MaterialTextView[] otpDigitViews;
    private CountDownTimer resendTimer;
    private long resendSecondsLeft;
    private TextWatcher codeWatcher;
    private boolean otpHasError;
    private boolean isProgrammaticallyClearing;
    private boolean requiresResend;
    private boolean lockedByAttempts;
    private int invalidAttempts;

    private String email;
    private String source;
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
            source = getArguments().getString("source", SOURCE_REGISTRATION);
        }

        codeEditText = view.findViewById(R.id.otp_signup_code_edit_text);
        codeInputLayout = view.findViewById(R.id.otp_signup_code_input_layout);
        verifyButton = view.findViewById(R.id.otp_signup_verify_button);
        resendButton = view.findViewById(R.id.otp_signup_resend_button);
        progressIndicator = view.findViewById(R.id.otp_signup_code_progress_indicator);
        resendTimerText = view.findViewById(R.id.otp_signup_resend_timer_text);
        subtitleText = view.findViewById(R.id.otp_signup_code_subtitle);

        otpDigitViews = new MaterialTextView[] {
                view.findViewById(R.id.otp_digit_1),
                view.findViewById(R.id.otp_digit_2),
                view.findViewById(R.id.otp_digit_3),
                view.findViewById(R.id.otp_digit_4),
                view.findViewById(R.id.otp_digit_5),
                view.findViewById(R.id.otp_digit_6)
        };

        super.onViewCreated(view, savedInstanceState);

        if (subtitleText != null && email != null) {
            subtitleText.setText(getString(R.string.otp_verify_subtitle_format, email));
        }

        viewModel = new ViewModelProvider(requireActivity()).get(SignupViewModel.class);

        verifyButton.setOnClickListener(v -> verifyCode());
        resendButton.setOnClickListener(v -> resendCode());

        setupOtpInputUx();
        startResendCooldown();

        viewModel.getOtpCodeState().observe(getViewLifecycleOwner(), state -> {
            resendButton.setEnabled(!state.isLoading && resendSecondsLeft <= 0L);
            codeEditText.setEnabled(!state.isLoading);
            progressIndicator.setVisibility(state.isLoading ? View.VISIBLE : View.GONE);
            updateVerifyButtonState(!state.isLoading);

            if (state.error != null) {
                String backendMessage = state.error.resolve(requireContext());
                String message = backendMessage;

                if (isExpiredOtpBackendMessage(backendMessage)) {
                    requiresResend = true;
                    message = getString(R.string.error_signup_otp_expired_custom);
                    cancelCooldownAndEnableResend();
                } else if (isInvalidOtpBackendMessage(backendMessage)) {
                    invalidAttempts++;
                    message = getString(R.string.error_signup_otp_invalid_custom);
                    cancelCooldownAndEnableResend();

                    if (invalidAttempts >= MAX_INVALID_ATTEMPTS) {
                        lockedByAttempts = true;
                        requiresResend = true;
                        message = getString(R.string.error_signup_otp_too_many_attempts);
                        startResendCooldown(EXTRA_COOLDOWN_SECONDS);
                    }
                }

                showOtpError(message);
                clearOtpInputPreservingError();
                viewModel.otpCodeErrorConsumed();
            }
            if (state.otpResent) {
                clearOtpInput();
                invalidAttempts = 0;
                requiresResend = false;
                lockedByAttempts = false;
                startResendCooldown();
                showInfo(getString(R.string.signup_otp_resent_message));
                viewModel.otpResentConsumed();
            }
            if (state.navigateToHome) {
                navigateToHome();
                viewModel.otpCodeNavigationConsumed();
            }
        });

        if (email == null || email.isEmpty()) {
            navController.navigateUp();
        }
    }

    @Override
    protected void navigateToHome() {
        if (BiometricHelper.shouldShowEnrollment(requireContext())) {
            navController.navigate(R.id.action_otpSignupCodeFragment_to_biometricEnrollFragment);
        } else {
            navController.navigate(R.id.action_otpSignupCodeFragment_to_homeFragment);
        }
    }

    private void verifyCode() {
        if (requiresResend || lockedByAttempts) {
            showOtpError(getString(R.string.error_signup_otp_resend_required));
            return;
        }

        String code = codeEditText.getText() != null
                ? codeEditText.getText().toString().trim() : "";
        String codeError = AuthInputValidator.validateOtp(requireContext(), code);
        if (codeError != null) {
            showOtpError(codeError);
            return;
        }

        if (SOURCE_OTP_LOGIN.equals(source)) {
            viewModel.verifyLoginOtp(email, code);
        } else {
            viewModel.verifySignupOtp(email, code);
        }
    }

    private void resendCode() {
        if (resendSecondsLeft > 0L) return;
        if (SOURCE_OTP_LOGIN.equals(source)) {
            viewModel.resendLoginOtp(email);
        } else {
            viewModel.resendSignupOtp(email);
        }
    }

    private void setupOtpInputUx() {
        View.OnClickListener focusInput = v -> {
            codeEditText.requestFocus();
            showKeyboard();
        };
        for (MaterialTextView digitView : otpDigitViews) {
            digitView.setOnClickListener(focusInput);
        }

        codeWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String raw = s == null ? "" : s.toString();
                String digits = raw.replaceAll("[^0-9]", "");
                if (!raw.equals(digits)) {
                    codeEditText.setText(digits);
                    codeEditText.setSelection(digits.length());
                    return;
                }
                if (digits.length() > 6) {
                    String trimmed = digits.substring(0, 6);
                    codeEditText.setText(trimmed);
                    codeEditText.setSelection(trimmed.length());
                    return;
                }
                if (!isProgrammaticallyClearing) {
                    codeInputLayout.setError(null);
                    otpHasError = false;
                }
                updateOtpBoxes(digits);
                updateVerifyButtonState(true);
            }
        };
        codeEditText.addTextChangedListener(codeWatcher);
        verifyButton.setEnabled(false);

        codeEditText.post(() -> {
            codeEditText.requestFocus();
            showKeyboard();
        });
        updateOtpBoxes("");
    }

    private void updateOtpBoxes(String digits) {
        int currentIndex = Math.min(digits.length(), otpDigitViews.length - 1);
        for (int i = 0; i < otpDigitViews.length; i++) {
            MaterialTextView box = otpDigitViews[i];
            box.setText(i < digits.length() ? String.valueOf(digits.charAt(i)) : "");
            boolean active = (i == currentIndex && digits.length() < 6) || (digits.length() == 6 && i == 5);
            if (otpHasError) {
                box.setBackgroundResource(R.drawable.otp_digit_box_error);
            } else {
                box.setBackgroundResource(active ? R.drawable.otp_digit_box_active : R.drawable.otp_digit_box_default);
            }
        }
    }

    private void clearOtpInput() {
        codeEditText.setText("");
        codeInputLayout.setError(null);
        otpHasError = false;
        updateOtpBoxes("");
        verifyButton.setEnabled(false);
    }

    private void clearOtpInputPreservingError() {
        isProgrammaticallyClearing = true;
        codeEditText.setText("");
        isProgrammaticallyClearing = false;
        updateOtpBoxes("");
        updateVerifyButtonState(true);
    }

    private void showOtpError(String message) {
        otpHasError = true;
        codeInputLayout.setError(message);
        String code = codeEditText.getText() == null ? "" : codeEditText.getText().toString();
        updateOtpBoxes(code);
    }

    private boolean isInvalidOtpBackendMessage(String message) {
        if (message == null) return false;
        String n = message.toLowerCase();
        return n.contains("invalid") || n.contains("incorrect") || n.contains("inválid")
                || n.contains("código inválido") || n.contains("codigo invalido");
    }

    private boolean isExpiredOtpBackendMessage(String message) {
        if (message == null) return false;
        String n = message.toLowerCase();
        return n.contains("expired") || n.contains("expir") || n.contains("vencid");
    }

    private void cancelCooldownAndEnableResend() {
        if (resendTimer != null) {
            resendTimer.cancel();
            resendTimer = null;
        }
        resendSecondsLeft = 0L;
        resendTimerText.setText(R.string.otp_resend_available);
        resendButton.setEnabled(true);
    }

    private boolean isOtpComplete() {
        String code = codeEditText.getText() == null ? "" : codeEditText.getText().toString().trim();
        return code.length() == 6;
    }

    private void startResendCooldown() {
        startResendCooldown(RESEND_COOLDOWN_SECONDS);
    }

    private void startResendCooldown(long seconds) {
        if (resendTimer != null) resendTimer.cancel();
        resendSecondsLeft = seconds;
        resendButton.setEnabled(false);
        updateResendTimerText();

        resendTimer = new CountDownTimer(seconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                resendSecondsLeft = Math.max(0L, millisUntilFinished / 1000L);
                updateResendTimerText();
                resendButton.setEnabled(false);
            }

            @Override
            public void onFinish() {
                resendSecondsLeft = 0L;
                resendTimerText.setText(R.string.otp_resend_available);
                resendButton.setEnabled(true);
            }
        }.start();
    }

    private void updateVerifyButtonState(boolean notLoading) {
        verifyButton.setEnabled(notLoading && isOtpComplete() && !requiresResend && !lockedByAttempts);
    }

    private void updateResendTimerText() {
        resendTimerText.setText(getString(R.string.otp_resend_in_seconds, resendSecondsLeft));
    }

    private void showKeyboard() {
        InputMethodManager imm = requireContext().getSystemService(InputMethodManager.class);
        if (imm != null) imm.showSoftInput(codeEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void onDestroyView() {
        if (resendTimer != null) {
            resendTimer.cancel();
            resendTimer = null;
        }
        if (codeEditText != null && codeWatcher != null) {
            codeEditText.removeTextChangedListener(codeWatcher);
        }
        codeEditText = null;
        codeInputLayout = null;
        verifyButton = null;
        resendButton = null;
        progressIndicator = null;
        resendTimerText = null;
        subtitleText = null;
        otpDigitViews = null;
        codeWatcher = null;
        super.onDestroyView();
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
    }
}
