package com.example.filter.apis.service;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface UserApi {

    @GET("/api/v1/users/exists")
    Call<ResponseBody> checkUserExists();

    @POST("/api/v1/users/nickname")
    Call<ResponseBody> setNickname(@Body NicknameRequest body);

    class NicknameRequest {
        private String nickname;

        public NicknameRequest(String nickname) {
            this.nickname = nickname;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }


    /// 닉네임 중복 판단 필요
    @GET("/api/v1/users/nickname/exists")
    Call<ResponseBody> checkNicknameExists(@Query("nickname") String nickname);
}
