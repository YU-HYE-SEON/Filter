package com.example.filter.apis;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
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


    /// 닉네임 중복
    @GET("/api/v1/users/nickname")
    Call<ResponseBody> checkNicknameExists(@Query("candidate") String nickname);

    // sns 아이디 조희
    // 반환값: Map<String, String> (instagramId, xId)
    @GET("/api/v1/users/social")
    Call<Map<String, String>> getSocialIds();

    // sns 아이디 설정 (업데이트)
    @POST("/api/v1/users/social")
    Call<Void> setSocialIds(@Body Map<String, String> ids);


    // 계정 삭제
    @DELETE("/api/v1/users")
    Call<Void> deleteUser();
}
