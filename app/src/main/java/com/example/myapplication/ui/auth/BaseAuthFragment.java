package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.myapplication.R;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Lightweight base class for auth fragments.
 *
 * Responsibilities kept here:
 * - NavController wiring
 * - Snackbar helpers (showError / showInfo)
 *
 * Everything removed from the old BaseAuthFragment:
 * - ConfigLoader / AppConfig (now handled by AuthRepository itself)
 * - AuthViewModel creation (each fragment now injects its own @HiltViewModel)
 * - loadConfig() / onConfigReady() / onConfigError() lifecycle (ViewModel exposes configValid)
 */
@AndroidEntryPoint
public abstract class BaseAuthFragment extends Fragment {

    protected NavController navController;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }

    protected void navigateToHome() {
        try {
            navController.navigate(R.id.homeFragment);
        } catch (Exception ignored) {
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
