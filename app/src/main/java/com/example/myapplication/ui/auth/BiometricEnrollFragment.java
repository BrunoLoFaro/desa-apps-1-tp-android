package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.example.myapplication.R;
import com.example.myapplication.util.BiometricHelper;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.concurrent.Executor;

@AndroidEntryPoint
public class BiometricEnrollFragment extends BaseAuthFragment {

    private View activateButton;
    private View skipButton;
    private boolean fromProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_biometric_enroll, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activateButton = view.findViewById(R.id.biometric_activate_button);
        skipButton = view.findViewById(R.id.biometric_skip_button);

        String origin = getArguments() != null ? getArguments().getString("origin", "auth") : "auth";
        fromProfile = "profile".equals(origin);

        if (!canAuthenticate()) {
            finishFlow();
            return;
        }

        activateButton.setOnClickListener(v -> showBiometricPrompt());
        skipButton.setOnClickListener(v -> {
            BiometricHelper.setBiometricSkipped(requireContext(), true);
            finishFlow();
        });
    }

    private boolean canAuthenticate() {
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        return BiometricManager.from(requireContext()).canAuthenticate(authenticators)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(requireContext());

        BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                BiometricHelper.setBiometricEnabled(requireContext(), true);
                finishFlow();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                showError(getString(R.string.biometric_enroll_failed));
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                showError(errString != null ? errString.toString() : getString(R.string.biometric_enroll_error));
            }
        };

        BiometricPrompt prompt = new BiometricPrompt(this, executor, callback);

        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setAllowedAuthenticators(authenticators)
                .build();

        prompt.authenticate(promptInfo);
    }

    @Override
    protected void navigateToHome() {
        navController.navigate(R.id.action_biometricEnrollFragment_to_homeFragment);
    }

    private void finishFlow() {
        if (fromProfile) {
            navController.navigateUp();
        } else {
            navigateToHome();
        }
    }

    @Override
    public void onDestroyView() {
        activateButton = null;
        skipButton = null;
        fromProfile = false;
        super.onDestroyView();
    }
}

