package com.example.filter.apis.client;

import android.content.Context;
import android.util.Log; // ✅ 로그 사용을 위해 추가

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AppRetrofitClient {
    private static final String BASE_URL = "http://13.124.105.243/";
    private static final String TAG = "API_LOG"; // ✅ 로그 필터용 태그
    private static Retrofit retrofit;

    public static Retrofit getInstance(Context context) {
        if (retrofit == null) {
            // 1. 전체 통신 내용(Body)을 보여주는 인터셉터
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // 2. 헤더에 토큰을 넣는 인터셉터 (+ 커스텀 로그 추가)
            Interceptor authInterceptor = chain -> {
                Request original = chain.request();
                String url = original.url().toString();

                // ✅ 요청 URL 확인 로그
                Log.d(TAG, "🔵 요청 시작: " + url);

                // 인증 제외 URL
                if (url.contains("/api/v1/auth/google")
                        || url.contains("/api/v1/auth/reissue")) {
                    Log.d(TAG, "⚪ 인증 제외 URL입니다. 토큰 없이 진행합니다.");
                    return chain.proceed(original);
                }

                String token = context.getSharedPreferences("Auth", Context.MODE_PRIVATE)
                        .getString("accessToken", null);

                Request.Builder builder = original.newBuilder();

                if (token != null && !token.isEmpty()) {
                    // ✅ 토큰 추가 성공 로그
                    Log.d(TAG, "🟢 토큰 발견! 헤더에 추가함 (" + token.substring(0, Math.min(token.length(), 10)) + "...)");
                    builder.addHeader("Authorization", "Bearer " + token);
                } else {
                    // ❌ 토큰 누락 경고 로그
                    Log.e(TAG, "🔴 토큰이 없습니다! (null or empty)");
                }

                return chain.proceed(builder.build());
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)       // OkHttp 기본 로그 (Tag: OkHttp)
                    .addInterceptor(authInterceptor) // 커스텀 로그 (Tag: API_LOG)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}