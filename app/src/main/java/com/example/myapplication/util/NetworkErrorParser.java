package com.example.myapplication.util;

import com.example.myapplication.data.model.ApiErrorResponse;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;

public final class NetworkErrorParser {

    private static final JsonAdapter<ApiErrorResponse> ADAPTER = new Moshi.Builder()
            .build()
            .adapter(ApiErrorResponse.class);

    private NetworkErrorParser() {
    }

    public static String getErrorMessage(Response<?> response, String fallbackMessage) {
        if (response == null) {
            return fallbackMessage;
        }

        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) {
                String errorJson = errorBody.string();
                
                // LOG PARA DEBUG: Imprimimos el error real del backend en el Logcat
                if (response.code() >= 400) {
                    android.util.Log.e("API_ERROR", "Status: " + response.code() + " | Body: " + errorJson);
                }

                if (errorJson != null && !errorJson.isEmpty()) {
                    ApiErrorResponse apiError = ADAPTER.fromJson(errorJson);
                    if (apiError != null && apiError.message != null && !apiError.message.trim().isEmpty()) {
                        return apiError.message.trim();
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("NetworkErrorParser", "Error al parsear el cuerpo del error", e);
        }

        String message = response.message();
        return message == null || message.trim().isEmpty() ? fallbackMessage : message.trim();
    }

    /**
     * MEJORA: Maneja los errores de onFailure (cuando no hay Response).
     * Útil para distinguir entre "Sin Internet", "Timeout" o "Servidor Caído".
     */
    public static String getFailureMessage(Throwable t, String fallbackMessage) {
        if (t == null) return fallbackMessage;
        
        android.util.Log.e("API_FAILURE", "Error de red/petición", t);

        if (t instanceof java.net.UnknownHostException || t instanceof java.net.ConnectException) {
            return "No se pudo establecer conexión con el servidor. Verifica tu internet.";
        } else if (t instanceof java.net.SocketTimeoutException) {
            return "La conexión ha expirado. Reintenta en unos momentos.";
        }
        
        String msg = t.getLocalizedMessage();
        return (msg == null || msg.isEmpty()) ? fallbackMessage : msg;
    }
}