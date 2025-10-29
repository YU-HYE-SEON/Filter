package com.example.filter.apis;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AppRetrofitClient {

    public static Retrofit create(Context context, String baseUrl) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            String url = original.url().toString();

            if (url.contains("/api/v1/auth/google") || url.contains("/api/v1/auth/reissue")) {
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

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
