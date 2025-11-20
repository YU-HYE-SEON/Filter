package com.example.filter.apis.service;

import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface UploadApi {

    // ✅ 필터 오리지널 + 프리뷰 이미지 업로드
    // 반환값: Map<String, String> (originalImageUrl, previewImageUrl)
    @Multipart
    @POST("/api/v1/uploads/filters") // ⚠️ 주의: 서버 Controller의 @RequestMapping 경로를 확인해서 앞에 붙여주세요!
    Call<Map<String, String>> uploadFilterImages(
            @Part MultipartBody.Part original,
            @Part MultipartBody.Part preview
    );

    // 서버가 ResponseEntity<String>을 반환하므로 Call<String>으로 받습니다.
    @Multipart
    @POST("/api/v1/uploads/sticker-images") // 서버 Controller의 @RequestMapping 경로 확인 필요
    Call<ResponseBody> uploadStickerImage(
            @Part MultipartBody.Part file
    );
}