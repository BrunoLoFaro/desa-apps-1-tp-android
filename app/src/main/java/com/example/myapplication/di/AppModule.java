package com.example.myapplication.di;

import android.util.Log;
import com.example.myapplication.BuildConfig;
import com.example.myapplication.data.config.AppConfig;
import com.example.myapplication.data.config.ConfigLoader;
import com.example.myapplication.data.network.ActivityService;
import com.example.myapplication.data.network.AuthService;
import com.example.myapplication.data.network.BookingService;
import com.example.myapplication.data.network.ReviewService;
import com.example.myapplication.data.network.CatalogMetaService;
import com.example.myapplication.data.session.SessionManager;
import com.squareup.moshi.Moshi;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    private static final String TAG = "OkHttpClient";

    /**
     * Provides a singleton OkHttpClient shared across all network calls.
     * The auth interceptor uses the injected SessionManager (true singleton),
     * so the same token state is used everywhere — no duplicate instances.
     */
    @Provides
    @Singleton
    static OkHttpClient provideOkHttpClient(SessionManager sessionManager) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        // 1. HTTP Logging Interceptor
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                message -> Log.d(TAG, "HTTP: " + message)
        );
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);
        builder.addInterceptor(logging);

        // 2. Authentication Interceptor — attaches JWT from the singleton SessionManager
        builder.addInterceptor(chain -> {
            Request original = chain.request();
            String token = sessionManager.getAccessToken();
            if (token != null && !token.isEmpty()) {
                Log.d(TAG, "Adjuntando token JWT al header Authorization");
                return chain.proceed(original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .build());
            }
            return chain.proceed(original);
        });

        // 3. Request/Response timing inspector (debug only)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(chain -> {
                long start = System.currentTimeMillis();
                Request request = chain.request();
                Log.d(TAG, "→ Enviando: " + request.method() + " " + request.url());
                Response response = chain.proceed(request);
                Log.d(TAG, "← Respuesta: " + response.code() + " en "
                        + (System.currentTimeMillis() - start) + "ms");
                return response;
            });
        }

        return builder.build();
    }

    @Provides
    @Singleton
    static Moshi provideMoshi() {
        return new Moshi.Builder().build();
    }

    @Provides
    @Singleton
    static Retrofit provideRetrofit(OkHttpClient okHttpClient, Moshi moshi, ConfigLoader configLoader) {
        AppConfig config = configLoader.loadConfig();
        if (config == null || !config.hasValidBaseUrl()) {
            throw new IllegalStateException(
                    "Config inválida: verificar que config.json tenga un base_url válido");
        }
        return new Retrofit.Builder()
                .baseUrl(config.baseUrl)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(okHttpClient)
                .build();
    }

    @Provides
    @Singleton
    static AuthService provideAuthService(Retrofit retrofit) {
        return retrofit.create(AuthService.class);
    }

    @Provides
    @Singleton
    static ActivityService provideActivityService(Retrofit retrofit) {
        return retrofit.create(ActivityService.class);
    }

    @Provides
    @Singleton
    static CatalogMetaService provideCatalogMetaService(Retrofit retrofit) {
        return retrofit.create(CatalogMetaService.class);
    }

    @Provides
    @Singleton
    static BookingService provideBookingService(Retrofit retrofit) {
        return retrofit.create(BookingService.class);
    }

    @Provides
    @Singleton
    static ReviewService provideReviewService(Retrofit retrofit) {
        return retrofit.create(ReviewService.class);
    }
}
