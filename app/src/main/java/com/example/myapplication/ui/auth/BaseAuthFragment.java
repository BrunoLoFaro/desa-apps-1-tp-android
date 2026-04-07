package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.myapplication.R;
import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.network.AuthService;
import com.example.myapplication.data.network.RetrofitClient;
import com.example.myapplication.data.session.SessionManager;
import com.google.android.material.snackbar.Snackbar;

public abstract class BaseAuthFragment extends Fragment {

    protected AppConfig appConfig;
    protected AuthService authService;
    protected SessionManager sessionManager;
    protected NavController navController;
    private ConfigLoader configLoader;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        configLoader = new ConfigLoader(requireContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        loadConfig();
    }

    private void loadConfig() {
        try {
            appConfig = configLoader.loadConfig();
            if (appConfig != null && appConfig.baseUrl != null && !appConfig.baseUrl.isEmpty()) {
                authService = RetrofitClient.getClient(appConfig, requireContext()).create(AuthService.class);
                onConfigReady();
            } else {
                authService = null;
                onConfigError(getString(R.string.error_config_load));
            }
        } catch (Exception e) {
            authService = null;
            onConfigError(getString(R.string.error_invalid_config));
        }
    }

    protected abstract void onConfigReady();

    protected void onConfigError(String error) {
        showError(error);
    }

    protected void handleLoginSuccess(LoginResponse response) {
        if (response != null && response.token != null && !response.token.trim().isEmpty()) {
            sessionManager.saveSession(
                    response.token,
                    response.userId != null ? response.userId : -1L,
                    response.email,
                    response.firstName,
                    response.lastName
            );
        }
        navigateToHome();
    }

    protected void navigateToHome() {
        // Subclasses should override this if they have a specific action to Home
        // or we can try to navigate to the homeFragment destination directly
        try {
            navController.navigate(R.id.homeFragment);
        } catch (Exception e) {
            // Fallback or log error
        }
    }

    protected void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getResources().getColor(R.color.error, requireContext().getTheme()))
                    .setTextColor(getResources().getColor(R.color.onError, requireContext().getTheme()))
                    .show();
        }
    }

    protected void showInfo(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
}
