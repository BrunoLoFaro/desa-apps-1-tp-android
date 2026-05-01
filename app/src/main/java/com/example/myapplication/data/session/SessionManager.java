package com.example.myapplication.data.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import com.example.myapplication.util.BiometricHelper;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SessionManager {
    private static final String PREFS_NAME = "auth_session";
    private static final String KEY_ACCESS_TOKEN  = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID       = "user_id";
    private static final String KEY_USER_EMAIL    = "user_email";
    private static final String KEY_FIRST_NAME        = "user_first_name";
    private static final String KEY_LAST_NAME         = "user_last_name";
    private static final String KEY_PROFILE_PHOTO_URI = "profile_photo_uri";

    // Perfil pendiente de sincronización (guardado offline)
    private static final String KEY_PENDING_PROFILE     = "pending_profile";
    private static final String KEY_PENDING_FIRST_NAME  = "pending_first_name";
    private static final String KEY_PENDING_LAST_NAME   = "pending_last_name";
    private static final String KEY_PENDING_PHONE       = "pending_phone";
    private static final String KEY_PENDING_PHOTO_URI   = "pending_photo_uri";
    private static final String KEY_PENDING_CATEGORIES  = "pending_categories";

    private final Context appContext;
    private final SharedPreferences preferences;
    private final MutableLiveData<Boolean> _forceLogout = new MutableLiveData<>(false);

    @Inject
    public SessionManager(@ApplicationContext Context context) {
        this.appContext = context.getApplicationContext();
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            this.preferences = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encrypted SharedPreferences", e);
        }
    }

    /**
     * Guarda el token JWT junto con los datos del usuario obtenidos en el login/registro.
     * Evita que pantallas futuras (perfil, etc.) tengan que hacer un fetch extra solo para
     * mostrar el nombre del usuario.
     */
    public void saveSession(String token, String refreshToken, long userId,
                            String email, String firstName, String lastName) {
        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, token)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_FIRST_NAME, firstName)
                .putString(KEY_LAST_NAME, lastName)
                .apply();
    }

    public String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return preferences.getString(KEY_REFRESH_TOKEN, null);
    }

    /** Actualiza solo el access token (y opcionalmente el refresh token) tras un refresh exitoso. */
    public void saveTokens(String accessToken, String refreshToken) {
        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
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
        boolean expired = isTokenExpired(token);
        if (expired) {
            BiometricHelper.setBiometricEnabled(appContext, false);
        }
        return !expired;
    }

    public void saveProfilePhotoUri(String uri) {
        preferences.edit().putString(KEY_PROFILE_PHOTO_URI, uri).apply();
    }

    public String getProfilePhotoUri() {
        return preferences.getString(KEY_PROFILE_PHOTO_URI, null);
    }

    // ── Perfil pendiente de sincronización ──────────────────────────────────────

    public void savePendingProfile(String firstName, String lastName, String phone,
                                   String photoUri, java.util.List<String> categories) {
        String categoriesStr = categories != null ? android.text.TextUtils.join(",", categories) : "";
        preferences.edit()
                .putBoolean(KEY_PENDING_PROFILE, true)
                .putString(KEY_PENDING_FIRST_NAME, firstName)
                .putString(KEY_PENDING_LAST_NAME, lastName)
                .putString(KEY_PENDING_PHONE, phone != null ? phone : "")
                .putString(KEY_PENDING_PHOTO_URI, photoUri != null ? photoUri : "")
                .putString(KEY_PENDING_CATEGORIES, categoriesStr)
                .apply();
    }

    public boolean hasPendingProfile() {
        return preferences.getBoolean(KEY_PENDING_PROFILE, false);
    }

    public String getPendingFirstName()  { return preferences.getString(KEY_PENDING_FIRST_NAME, ""); }
    public String getPendingLastName()   { return preferences.getString(KEY_PENDING_LAST_NAME, ""); }
    public String getPendingPhone()      { return preferences.getString(KEY_PENDING_PHONE, ""); }
    public String getPendingPhotoUri()   { return preferences.getString(KEY_PENDING_PHOTO_URI, null); }

    public java.util.List<String> getPendingCategories() {
        String raw = preferences.getString(KEY_PENDING_CATEGORIES, "");
        if (raw == null || raw.isEmpty()) return java.util.Collections.emptyList();
        return java.util.Arrays.asList(raw.split(","));
    }

    public void clearPendingProfile() {
        preferences.edit()
                .remove(KEY_PENDING_PROFILE)
                .remove(KEY_PENDING_FIRST_NAME)
                .remove(KEY_PENDING_LAST_NAME)
                .remove(KEY_PENDING_PHONE)
                .remove(KEY_PENDING_PHOTO_URI)
                .remove(KEY_PENDING_CATEGORIES)
                .apply();
    }

    public void clearSession() {
        preferences.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_FIRST_NAME)
                .remove(KEY_LAST_NAME)
                .remove(KEY_PROFILE_PHOTO_URI)
                .apply();
        clearPendingProfile();
    }

    /**
     * Limpia la sesión y emite un evento para que la UI redirija al login.
     * Llamar solo desde el Authenticator OkHttp (401 no autorizado del servidor),
     * no desde el logout explícito del usuario.
     */
    public void triggerForceLogout() {
        clearSession();
        BiometricHelper.setBiometricEnabled(appContext, false);
        _forceLogout.postValue(true);
    }

    public LiveData<Boolean> getForceLogoutEvent() { return _forceLogout; }

    public void consumeForceLogout() { _forceLogout.postValue(false); }

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
