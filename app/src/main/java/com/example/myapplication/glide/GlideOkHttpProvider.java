package com.example.myapplication.glide;

import android.content.Context;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

public class GlideOkHttpProvider {
    private static OkHttpClient client;

    public static synchronized OkHttpClient getClient(Context context) {
        if (client == null) {
            // Cliente limpio sin interceptores de auth: Glide carga imágenes públicas (Unsplash, etc.)
            // y no debe enviar tokens JWT a dominios externos.
            client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }
}
