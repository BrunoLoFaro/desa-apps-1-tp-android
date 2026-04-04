package com.example.myapplication.data.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SessionManager {
    private static final String PREFS_NAME = "auth_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_ID      = "user_id";
    private static final String KEY_USER_EMAIL   = "user_email";
    private static final String KEY_FIRST_NAME   = "user_first_name";
    private static final String KEY_LAST_NAME    = "user_last_name";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Guarda el token JWT junto con los datos del usuario obtenidos en el login/registro.
     * Evita que pantallas futuras (perfil, etc.) tengan que hacer un fetch extra solo para
     * mostrar el nombre del usuario.
     */
    public void saveSession(String token, long userId, String email, String firstName, String lastName) {
        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, token)
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_FIRST_NAME, firstName)
                .putString(KEY_LAST_NAME, lastName)
                .apply();
    }

    public String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public long getUserId() {
        return preferences.getLong(KEY_USER_ID, -1L);
    }

    public String getUserEmail() {
        return preferences.getString(KEY_USER_EMAIL, null);
    }

    public String getFirstName() {
        return preferences.getString(KEY_FIRST_NAME, null);
    }

    public String getLastName() {
        return preferences.getString(KEY_LAST_NAME, null);
    }

    /**
     * FIX: antes solo verificaba que el string no fuera vacío.
     * Ahora también valida que el JWT no haya expirado parseando el claim "exp".
     */
    public boolean hasValidSession() {
        String token = getAccessToken();
        if (token == null || token.trim().isEmpty()) return false;
        return !isTokenExpired(token);
    }

    public void clearSession() {
        preferences.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_FIRST_NAME)
                .remove(KEY_LAST_NAME)
                .apply();
    }

    private boolean isTokenExpired(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return true;

            byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_PADDING);
            String payload = new String(decoded, StandardCharsets.UTF_8);

            Matcher matcher = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)").matcher(payload);
            if (!matcher.find()) return false; // sin exp → nunca expira

            long exp = Long.parseLong(matcher.group(1));
            return System.currentTimeMillis() / 1000L >= exp;
        } catch (Exception e) {
            return true; // ante error de parseo, tratar como expirado
        }
    }
}
