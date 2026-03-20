package com.example.myapplication.data.network;

import com.example.myapplication.data.config.AppConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(AppConfig config) {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(config.baseUrl)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    public static void reset() {
        retrofit = null;
    }
}
