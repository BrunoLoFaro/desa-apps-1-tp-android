package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.model.LoginResponse;
import com.example.myapplication.data.network.AuthService;
import com.example.myapplication.data.network.RetrofitClient;
import com.example.myapplication.data.session.SessionManager;
import com.google.android.material.snackbar.Snackbar;

/**
 * Clase base para todas las Activities de autenticación.
 *
 * Elimina la duplicación del patrón loadConfiguration/showError/showInfo/handleLoginSuccess
 * que se repetía idéntico en 8 Activities.
 *
 * Uso:
 *  1. Extender esta clase.
 *  2. Implementar getRootView() devolviendo el CoordinatorLayout de la pantalla.
 *  3. Implementar onConfigReady() para habilitar botones cuando la config carga correctamente.
 *  4. Opcionalmente override onConfigError() para deshabilitar botones además de mostrar el error.
 *  5. Usar handleLoginSuccess(LoginResponse) al recibir respuesta exitosa de login/registro.
 *  6. Usar navigateToHome() para ir a Home sin necesidad de un LoginResponse (ej: sesión válida).
 */
public abstract class BaseAuthActivity extends AppCompatActivity {

    protected AppConfig appConfig;
    protected AuthService authService;
    // Accesible desde subclases para validar sesión activa o guardar datos del usuario.
    protected SessionManager sessionManager;

    private ConfigLoader configLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inicializar aquí para que esté disponible antes de onResume().
        // MainActivity lo necesita en su onResume() antes de llamar a super.onResume().
        sessionManager = new SessionManager(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (configLoader == null) configLoader = new ConfigLoader(this);
        loadConfig();
    }

    private void loadConfig() {
        try {
            appConfig = configLoader.loadConfig();
            if (appConfig != null && appConfig.baseUrl != null && !appConfig.baseUrl.isEmpty()) {
                authService = RetrofitClient.getClient(appConfig, this).create(AuthService.class);
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

    /** Llamado cuando la config cargó y authService está listo. Habilitar botones aquí. */
    protected abstract void onConfigReady();

    /** Llamado cuando la config falla. Por defecto muestra el error; override para deshabilitar botones. */
    protected void onConfigError(String error) {
        showError(error);
    }

    /** Devolver el CoordinatorLayout raíz para anclar los Snackbars y los insets. */
    protected abstract View getRootView();

    /**
     * Guarda los datos de sesión del usuario y navega a HomeActivity limpiando el back stack.
     *
     * Centraliza el patrón duplicado que existía en ClassicRegisterActivity,
     * OtpSignupCompleteActivity, ForgotPasswordNewPasswordActivity y MainActivity.
     */
    protected void handleLoginSuccess(LoginResponse response) {
        if (response != null && response.token != null && !response.token.trim().isEmpty()) {
            long userId = response.userId != null ? response.userId : -1L;
            sessionManager.saveSession(
                    response.token,
                    userId,
                    response.email,
                    response.firstName,
                    response.lastName
            );
        }
        navigateToHome();
    }

    /**
     * Navega a HomeActivity limpiando el back stack.
     * Usar cuando ya hay sesión válida y no hay un LoginResponse disponible.
     */
    protected void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    protected void showError(String message) {
        Snackbar.make(getRootView(), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.error, getTheme()))
                .setTextColor(getResources().getColor(R.color.onError, getTheme()))
                .show();
    }

    protected void showInfo(String message) {
        Snackbar.make(getRootView(), message, Snackbar.LENGTH_LONG).show();
    }
}
