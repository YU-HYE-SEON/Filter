package com.example.filter.apis.client;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AppRetrofitClient {
    private static final String BASE_URL = "http://13.124.105.243/";
    private static Retrofit retrofit; // Singleton instance

    public static Retrofit getInstance(Context context) {
        // Create Retrofit instance only once
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            Interceptor authInterceptor = chain -> {
                Request original = chain.request();
                String url = original.url().toString();

                // 인증 제외 URL
                if (url.contains("/api/v1/auth/google")
                        || url.contains("/api/v1/auth/reissue")) {
                    return chain.proceed(original);
                }

                String token = context.getSharedPreferences("Auth", Context.MODE_PRIVATE)
                        .getString("accessToken", null);

                Request.Builder builder = original.newBuilder();
                if (token != null && !token.isEmpty()) {
                    builder.addHeader("Authorization", "Bearer " + token);
                }

                return chain.proceed(builder.build());
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(authInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            // Build Retrofit instance
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
