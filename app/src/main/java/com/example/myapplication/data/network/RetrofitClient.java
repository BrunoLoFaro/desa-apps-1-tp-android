package com.example.myapplication.data.network;

import android.content.Context;
import com.example.myapplication.BuildConfig;
import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.session.SessionManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit = null;

    /**
     * Obtiene el cliente Retrofit con interceptor de autenticación JWT.
     * Usar este método desde BaseAuthActivity y cualquier lugar con acceso a Context.
     */
    public static Retrofit getClient(AppConfig config, Context context) {
        if (retrofit == null) {
            SessionManager sessionManager = context != null
                    ? new SessionManager(context) : null;

            // FIX: solo loguear body en debug; nunca en release (evita passwords/tokens en logcat)
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .addInterceptor(logging);

            // Interceptor de autenticación: adjunta el JWT en cada request si hay sesión activa
            if (sessionManager != null) {
                final SessionManager sm = sessionManager;
                builder.addInterceptor(chain -> {
                    String token = sm.getAccessToken();
                    if (token != null && !token.isEmpty()) {
                        Request authenticated = chain.request().newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .build();
                        return chain.proceed(authenticated);
                    }
                    return chain.proceed(chain.request());
                });
            }

            retrofit = new Retrofit.Builder()
                    .baseUrl(config.baseUrl)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .client(builder.build())
                    .build();
        }
        return retrofit;
    }

    public static void reset() {
        retrofit = null;
    }
}
