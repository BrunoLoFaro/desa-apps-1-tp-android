package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.myapplication.R;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Pantalla de completar registro — ya no se usa en el flujo actual.
 * El registro ahora se completa en ClassicRegisterFragment y la verificación
 * se hace en OtpSignupCodeFragment que navega directamente al Home.
 * Se mantiene únicamente para preservar la acción de navegación en el nav graph.
 */
@AndroidEntryPoint
public class OtpSignupCompleteFragment extends BaseAuthFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp_signup_complete, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController.navigateUp();
    }

    @Override
    protected void navigateToHome() {
        navController.navigate(R.id.action_otpSignupCompleteFragment_to_homeFragment);
    }
}